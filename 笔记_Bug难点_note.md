# 2026/06/03

## getPictureVOPage 方法分析

### 方法逻辑是否正确？
逻辑正确，但存在 **N+1 查询** 性能问题。

`getPictureVO` 内部会为每条 Picture 单独查一次用户信息：
```java
// getPictureVO 方法内部
User user = userService.getById(userId);  // 每条记录都查一次数据库
```

在 `getPictureVOPage` 中：
```java
// 对每条 Picture 都调用 getPictureVO
pictureList.stream().map(picture -> this.getPictureVO(picture, request))
```

### 什么是 N+1 查询？

**1 次主查询 + N 次关联查询 = N+1 次数据库查询**

假设一页有 10 张图片：

```
第 1 次查询（1）：获取图片列表
SELECT * FROM picture LIMIT 10

第 2~11 次查询（N = 10）：每张图片查一次用户
SELECT * FROM user WHERE id = 1
SELECT * FROM user WHERE id = 2
...
SELECT * FROM user WHERE id = 10

总计：1 + 10 = 11 次查询
```

如果一页 100 条图片 → 1 + 100 = **101 次查询**，数据库压力巨大。

### 性能优化方案：批量查询

**先收集所有 userId，一次性批量查询用户，再填充到图片中：**
```java
// 1. 收集所有 userId
Set<Long> userIds = pictureList.stream()
    .map(Picture::getUserId)
    .filter(id -> id != null && id > 0)
    .collect(Collectors.toSet());

// 2. 批量查询用户（1 次查询替代 N 次查询）
Map<Long, UserVO> userVOMap = new HashMap<>();
if (!userIds.isEmpty()) {
    userService.listByIds(userIds).forEach(user ->
        userVOMap.put(user.getId(), userService.getUserVO(user))
    );
}

// 3. 组装结果（无需逐条查库）
List<PictureVO> pictureVOList = pictureList.stream().map(picture -> {
    PictureVO pictureVO = PictureVO.objToVo(picture);
    UserVO userVO = userVOMap.get(picture.getUserId());
    pictureVO.setUser(userVO);
    return pictureVO;
}).collect(Collectors.toList());
```

优化后：**N+1 次查询 → 2 次查询**（1 次图片 + 1 次用户批量查询）

---

## getQueryWrapper 中 searchText 与 name/introduction 的关系

### 是否重复？
不重复，使用场景不同：

| 字段 | 用途 | SQL 效果 |
|------|------|---------|
| `searchText` | 搜索框关键词，同时搜多个字段 | `name LIKE '%风景%' OR introduction LIKE '%风景%'` |
| `name` | 单独筛选 name 字段 | `name LIKE '%风景%'` |
| `introduction` | 单独筛选 introduction 字段 | `introduction LIKE '%风景%'` |

- **searchText** = 模糊搜索，一个词搜多个字段（面向普通用户）
- **name / introduction** = 精确筛选，只针对单个字段（面向管理员）

### 如果 searchText 和 name 同时传入值？

假设 `searchText = "风景"`，`name = "山水"`，最终 SQL：
```sql
SELECT * FROM picture
WHERE (name LIKE '%风景%' OR introduction LIKE '%风景%')
  AND name LIKE '%山水%'
```
两个条件是 **AND 叠加** 关系，不冲突。含义：
1. 先找 name 或 introduction 中包含"风景"的
2. 再从结果中筛 name 包含"山水"的

更严格了，不是重复。

---

## JSON 转换的替代方案

### JSON 字符串 → List（反序列化）

`pictureVO.setTags(JSONUtil.toList(picture.getTags(), String.class))` 的替代方案：

**1. Jackson（Spring Boot 自带，推荐）**
```java
ObjectMapper objectMapper = new ObjectMapper();
List<String> tags = objectMapper.readValue(picture.getTags(), new TypeReference<List<String>>() {});
```

**2. Gson（Google）**
```java
Gson gson = new Gson();
List<String> tags = gson.fromJson(picture.getTags(), new TypeToken<List<String>>() {}.getType());
```

**3. Fastjson（阿里巴巴）**
```java
List<String> tags = JSON.parseArray(picture.getTags(), String.class);
```

### List → JSON 字符串（序列化）

`picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()))` 的替代方案：

**1. Jackson（推荐）**
```java
ObjectMapper objectMapper = new ObjectMapper();
picture.setTags(objectMapper.writeValueAsString(pictureVO.getTags()));
```

**2. Gson**
```java
Gson gson = new Gson();
picture.setTags(gson.toJson(pictureVO.getTags()));
```

**3. Fastjson**
```java
picture.setTags(JSON.toJSONString(pictureVO.getTags()));
```

推荐使用 **Jackson**，Spring Boot 内置无需额外依赖。



## 数据库设计

### 为什么 `picture` 实体表中 `tags` 字段设计成 JSON 字符串而不是数组或 List。

这是一个关于数据库设计的问题。核心原因是：**MySQL 关系型数据库中没有原生的数组类型**。

让我解释清楚：

1. MySQL 没有数组类型，如果要存储多个标签，有几种选择：
   - JSON 字符串：如 `["风景","自然","山水"]`
   - 逗号分隔字符串：如 `风景,自然,山水`
   - 关联表：如 `picture_tag` 表

2. 为什么选择 JSON 字符串而不是其他方式？

让我简洁回答。核心原因是 **MySQL 没有原生的数组/List 类型**，字段只能存基本类型（varchar、int 等）。

#### 可选方案对比

| 方案           | 存储                    | 查询                                   | 优缺点                       |
| -------------- | ----------------------- | -------------------------------------- | ---------------------------- |
| JSON 字符串    | `["风景","自然"]`       | 支持 `LIKE '%"风景%"'` 模糊查询        | 灵活，可读性好               |
| 逗号分隔字符串 | `风景,自然`             | 查询不精确，`LIKE '%风景%'` 可能误匹配 | 简单但不精确                 |
| 关联表         | 单独建 `picture_tag` 表 | SQL JOIN 查询，精确                    | 最规范，但增加表和查询复杂度 |

#### 为什么当前项目选了 JSON 字符串？

1. **简单**：一个字段搞定，不用建额外的关联表
2. **可读**：`["风景","自然"]` 比 `风景,自然` 结构更清晰
3. **查询方便**： `LIKE '%"标签名"%'` 就能模糊匹配
4. **MySQL 5.7+ 支持 JSON 类型**：MySQL 对 JSON 类型有专门的函数支持（如 `JSON_CONTAINS`）

#### 如果关联表（更规范的方式）

需要额外建表：

```
picture 表：id, name, url, ...
tag 表：id, name
picture_tag 表：picture_id, tag_id（多对多关联）
```

查询需要 JOIN：

```sql
SELECT p.* FROM picture p
JOIN picture_tag pt ON p.id = pt.picture_id
JOIN tag t ON pt.tag_id = t.id
WHERE t.name = '风景'
```

**更规范但更复杂**，适合标签需要单独管理、统计的场景。

#### 简单说

- 数据库**存不了数组**，只能存字符串
- JSON 字符串是**折中方案**：既保持了数组结构，又兼容数据库存储
- 代码中通过 DTO封装类和实体类 `JSONUtil.toList()` / `JSONUtil.toJsonStr()` 做 Java List 和 JSON 字符串之间的互转互转

---

# 2026/06/24

## uploadPicture 更新图片的三个 Bug：不删旧文件 / 额度重复累加 / 准入校验错位

### 背景：一个方法同时干"新增"和"更新"

`PictureServiceImpl.uploadPicture` 靠 `pictureId` 是否为空区分场景：`pictureId == null` 是新增，`pictureId != null` 是更新（"原地换图"，`saveOrUpdate` 改记录）。但额度那段逻辑**不区分场景**，导致更新时连踩三个坑。

---

### Bug①：更新时不删旧文件 → COS 残留孤儿文件

`:163` 无论新增还是更新都上传一个**全新文件**（每次 UUID 生成新路径，新 URL ≠ 旧 URL）：

```java
UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);
```

更新时 `:198` `saveOrUpdate` 把记录 URL 改成新地址，**但旧 URL 指向的 COS 对象从此没人引用、也没删**。

对比 `deletePicture` 有 `clearPictureFile` 删 COS 文件，`uploadPicture` 更新分支**没有任何删旧文件逻辑**。频繁更新同一张图 → COS 堆一堆孤儿文件，白占存储。

---

### Bug②：更新时额度重复累加 → totalSize / totalCount 全算错（最严重）

看事务 `:197-208`：

```java
transactionTemplate.execute(status -> {
    boolean result = this.saveOrUpdate(picture);
    ThrowUtils.throwIf(!result, ...);
    if (finalSpaceId != null) {
        boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId)
                .setSql("totalSize = totalSize + " + picture.getPicSize())  // ← 永远"加"
                .setSql("totalCount = totalCount + 1")                       // ← 永远"加 1"
                .update();
    }
    return null;
});
```

这段**不区分新增/更新，永远做加法**。但更新时这张图**第一次上传时已经占过一次额度**，现在又加一遍 → **重复计算**。

走一遍数字：

```
初始：空间 totalSize=0, totalCount=0
① 上传图 A(10MB)，新增 → totalSize=10, totalCount=1   ✅ 正确(1张 10MB)
② 更新图 A(换成 20MB) → totalSize += 20 = 30, totalCount += 1 = 2   ❌
   实际还是 1 张、20MB，额度却记成 2 张、30MB → 多算 1 张 + 10MB
```

更新越多，额度越虚高，跟实际完全对不上。

---

### Bug③：更新时"准入校验"也是错的

`:102-105` 的额度准入校验：

```java
if (spaceId != null) {                          // ← 只在请求带了 spaceId 时才校验
    ...
    ThrowUtils.throwIf(oldSpace.getTotalCount() >= oldSpace.getMaxCount(), ... "空间条数不足");
    ThrowUtils.throwIf(oldSpace.getTotalSize() >= oldSpace.getMaxSize(), ... "空间大小不足");
}
```

两个问题：

**a) 更新不传 spaceId 时，校验被整个跳过。**
更新场景下 `spaceId` 来自 `pictureUploadRequest.getSpaceId()`（`:92`）。前端更新时往往不传 spaceId → `spaceId == null` → 这个 `if` 不进 → **额度校验被绕过**（后面 `:130-132` 才从 oldPicture 补回 spaceId，但校验已经过去了）。

**b) 就算校验了，语义也是"新增"的，不适合更新。**
更新是"原地换图"，**条数不变**（还是 1 张），用 `totalCount >= maxCount` 拦更新不合理——本来在额度内的图换个文件，凭啥被"条数不足"拦住？正确应按"大小差额"判断。

---

### 根因：新增和更新混在一个方法，额度逻辑没分支

`uploadPicture` 一个方法同时干新增 + 更新，但额度那段是**无脑加法、不区分场景**，这是三个 bug 的总根源。

---

### 正确做法：更新走"差额调整 + 条数不变 + 删旧文件"

更新的本质是"原地替换文件"，额度影响只有**大小的变化**，条数不变：

```java
// 先把 oldPicture 提到方法外（原代码它在 if 块内，事务里拿不到）
Picture oldPicture = (pictureId != null) ? this.getById(pictureId) : null;

transactionTemplate.execute(status -> {
    boolean result = this.saveOrUpdate(picture);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");

    if (finalSpaceId != null) {
        if (pictureId != null && oldPicture != null) {
            // ★ 更新：只调大小差额，条数不变
            long delta = picture.getPicSize() - oldPicture.getPicSize();  // 可正可负
            spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId)
                    .setSql("totalSize = totalSize + (" + delta + ")")
                    // totalCount 不动
                    .update();
        } else {
            // 新增：加大小 + 加条数（原逻辑）
            spaceService.lambdaUpdate().eq(Space::getId, finalSpaceId)
                    .setSql("totalSize = totalSize + " + picture.getPicSize())
                    .setSql("totalCount = totalCount + 1")
                    .update();
        }
    }
    return null;
});

// ★ 更新成功后删旧文件（复用 clearPictureFile 的多记录引用判断，事务外执行）
if (pictureId != null && oldPicture != null) {
    clearPictureFile(oldPicture);   // 删旧 URL 的 COS 对象
}
```

对应地，准入校验也要按场景分：更新只判"大小差额是否超限"，不判条数。

---

### 小结

| Bug | 是否存在 | 位置 | 后果 |
|------|:---:|------|------|
| ① 更新不删旧文件 | ✅ | `:163` 上传新文件，无删旧逻辑 | COS 孤儿文件堆积，白占存储 |
| ② 额度重复累加（最严重） | ✅ | `:200-204` 永远 `+size +1` | 更新一次多算 1 张 + 旧大小，额度虚高 |
| ③ 更新准入校验错位 | ✅ | `:102-105` 在 `if(spaceId!=null)` 内，且是"新增"语义 | 不传 spaceId 时被绕过；传了也会被"条数不足"误拦 |
| 根因 | — | 新增/更新混在一个方法，额度无分支 | 容易踩"忘了区分场景"的坑 |

---

### 经验教训

1. **"新增/更新"混在一个方法时，所有副作用（额度、文件、计数）都要按场景分支**：更新≠新增，额度不能无脑加，文件要清旧的。
2. **更新的额度按"差额"算**：`totalSize += (新大小 - 旧大小)`，条数不变；别把更新当新增再算一遍。
3. **换文件就要清旧文件**：上传新文件换了 URL，旧 URL 的 COS 对象要删（复用 `clearPictureFile` 的多记录引用判断），否则孤儿文件越堆越多。
4. **准入校验要跟操作语义匹配**：更新是"原地换图"不增条数，用"条数不足"拦更新是错的；校验的判断维度（条数/大小差额）要随场景变。
5. **变量作用域别埋雷**：`oldPicture` 原本声明在 `if (pictureId != null)` 块内，事务里拿不到——改之前要先把它提到外层，否则差额计算无从下手。

---

# 2026/06/26

## 以图搜图：从接口失败到 Selenium 跑通的漫长排障

### 背景：以图搜图接口一调用就抛 BusinessException

`GetImagePageUrlApi.getImagePageUrl` 向百度 `graph.baidu.com/upload` 发 POST 拿"以图搜图结果页地址"，第一步就抛"接口调用失败 → 搜索失败"。用途是给图片抓取**全网相似图**填充信息。

---

### 难点①：百度识图接口对非浏览器环境彻底关闭（纯 HTTP 死路）

实测各种调用方式，百度全部拒绝：

| 调用方式 | 百度返回 |
|---------|---------|
| 传图片 URL（原代码方式） | `1002 "Params illegal"` |
| multipart 传文件（真实照片 + Cookie + 完整浏览器头） | `"Reject"` |

而且访问识图首页时 Cookie jar 是空的——百度靠 **JS 动态种 Cookie / 风控 token**，纯 HTTP 客户端（Hutool/curl）怎么伪装都过不去。

- **根因**：百度识图 `graph.baidu.com/upload` 改版，对非真实浏览器环境一律拒绝。
- **结论**：纯 HTTP 方案彻底死路，只能用 **Selenium 起真实浏览器**绕过风控。
- **排错教训**：别在"加请求头 / Acs-Token"上浪费力气——实测加全套浏览器头（UA/Origin/Referer/Cookie/Acs-Token）返回码纹丝不动，根因不在请求头而在"是不是真浏览器"。Acs-Token 本身是**百度翻译**接口的反爬头（AES 动态生成），跟识图无关。

---

### 难点②：msedgedriver 下不来（Selenium Manager 自动下载失败）

`new EdgeDriver()` 报 `The path to the driver executable must be set`。

- **根因**：Selenium Manager 自动下载 msedgedriver 失败——微软 blob 存储 `PublicAccessNotPermitted`（禁止匿名下载），`azureedge.net` CDN 在本机 DNS 解析失败。
- **解决**：手动从官网下**完全匹配 Edge 版本**（149.0.4022.80）的 x64 zip，解压出 `msedgedriver.exe` 放项目 `drivers/` 目录，代码里 `System.setProperty("webdriver.edge.driver", ...)` 指定本地路径，优先于 Selenium Manager 自动下载。

---

### 难点③：selenium 被锁成 4.1.4，驱动不了新版 Edge（最隐蔽、最耗时）

driver 放好后报 `403 Forbidden` / `Unable to establish websocket connection`，堆栈里 `Build info: version: '4.1.4'`——**实际跑的是 4.1.4，不是 pom 写的 4.21.0**。4.1.4（2022 年）驱动不了 Edge 149。

**根因**：项目用 `import` 方式引入 `spring-boot-dependencies` 2.7.6 BOM（不是继承 `spring-boot-starter-parent`），BOM 把所有 selenium 子模块锁在 **4.1.4**（`<selenium.version>4.1.4</selenium.version>`），覆盖了 pom `<dependencies>` 里写的 4.21.0。`mvn dependency:tree -Dverbose` 每行都带 `(version managed from 4.21.0)` 就是铁证。

**踩的关键认知坑**：
- 第一反应是加 `<properties><selenium.version>4.21.0</selenium.version></properties>` 覆盖——**无效**！因为 **import 的 BOM 在导入时就用它自身 properties 把 `${selenium.version}` 解析成了 4.1.4**，项目 property 根本插不进去。（只有**继承 parent** 的方式才支持 property 覆盖。）
- 删本地仓库的 4.1.4 也没用——IDEA 一 reload，依赖树要求 4.1.4，又把它下回来。

**正确做法**：在项目自己的 `<dependencyManagement>` 里**逐个显式声明** selenium 子模块为 4.21.0（项目直接声明优先于 import 的 BOM）：

```xml
<dependencyManagement>
    <dependencies>
        <dependency><!-- spring-boot-dependencies BOM, scope=import --></dependency>
        <!-- 覆盖 BOM 锁定的旧版 selenium, 强制 4.21.0 -->
        <dependency>
            <groupId>org.seleniumhq.selenium</groupId>
            <artifactId>selenium-remote-driver</artifactId>
            <version>4.21.0</version>
        </dependency>
        <!-- ...edge-driver / chromium-driver / api / support / json / http 等同理... -->
    </dependencies>
</dependencyManagement>
```

---

### 难点④：双 maven 仓库（命令行下到 D 盘，IDEA 用 C 盘）

排查"为什么删了 4.1.4 还在"时发现：**命令行 mvn 的 localRepository 是 `D:\ApcheTomcat\maven\...\repository`（conf/settings.xml 配的），IDEA 用默认 `C:\Users\86187\.m2\repository`**，两边独立、不共享。

- 命令行 `mvn dependency:get` 下到了 D 盘，IDEA（用 C 盘）看不到，还是老版本 → 误以为没解决。
- **解决**：命令行下载/验证时带 `-Dmaven.repo.local=C:/Users/86187/.m2/repository` 下到 IDEA 的仓库。

---

### 难点⑤：新版结果页 URL 变了（card= → session_id=）

版本解决后，Edge 能启动、能上传，但 step3 等待 `card=` 超时。看报错里的"当前url"其实**已经跳转到结果页了**，只是新版 URL 是：

```
https://graph.baidu.com/s?card_key=&entrance=GENERAL&session_id=...&sign=...&tpl_from=pc
```

参数是 `session_id=` / `entrance=`，不是旧版的 `card=`。改判断条件为 `session_id=` 即过。另外结果页相似图是 JS 异步加载，上传后要 sleep 几秒再抓。

---

### 最终结果

跑通，`Build info: version: '4.21.0'`，抓到 39 张相似图（38 张真实相似图 + 1 张查询图，过滤 `bcebos` 查询图后干净）。

---

### 经验教训

1. **接口失效先验证"是不是真浏览器问题"，别在请求头上耗**：纯 HTTP 被风控拦（cookie 靠 JS 动态生成），加什么头都白搭，果断切 Selenium。
2. **版本被 BOM 锁定时，看 `dependency:tree -Dverbose` 的 `managed from`**：一眼看出谁被谁管成什么版本，比猜靠谱。
3. **import 的 BOM 不能用 property 覆盖版本**：只有继承 parent 才行；import 场景必须在项目 `<dependencyManagement>` 逐个显式声明覆盖。
4. **命令行 mvn 和 IDEA 的 localRepository 可能不是同一个**：排查"依赖版本不对"先确认是哪个仓库，命令行操作带 `-Dmaven.repo.local` 指到 IDEA 的仓库。
5. **driver 下不来别死磕自动下载**：微软源禁匿名下载 + CDN DNS 失败时，手动下对应版本放本地、`System.setProperty` 指定，最快。

---

# 2026/07/05

## 数据分析的技术实现思路与系统设计思想

### 背景：两种分析范式

数据分析通常有两种处理方式：

- **实时分析**：数据生成的同时立即处理分析，提供即时结果。适用于需要快速决策的场景（监控异常检测、电商实时推荐）。
- **离线分析**：批量收集存储后做复杂计算和深度分析。适用于数据量极大、不需要即时结果的场景（历史报表、数据挖掘）。

| 维度    | 实时分析                                                                          | 离线分析                                                                                          |
|-------|-------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------|
| 时机    | 数据产生即处理                                                                        | 批量采集存储后跑批                                                                                       |
| 延迟    | 毫秒~秒级                                                                         | 分钟~小时~T+1                                                                                     |
| 吞吐    | 低~中（逐条/微批）                                                                     | 高（批量摊薄）                                                                                       |
| 技术栈   | Flink / Spark Streaming / Kafka Streams；OLAP（ClickHouse、Doris、Druid）；时序库（Prometheus） | Spark / Hive SQL；数仓（StarRocks、ClickHouse）；调度（Airflow、DolphinScheduler、XXL-JOB）                       |
| 典型场景  | 监控告警、实时大屏、推荐、风控                                                                | 历史报表、T+1 报表、数据挖掘、特征工程                                                                          |

**设计思想**：实时 = 事件驱动 + 流式处理（低延迟、持续耗资源）；离线 = 批量摊薄成本 + 空间换时间（高吞吐、低成本、延迟大）。大型系统常用 **Lambda 架构** = 离线层（历史批结果）+ 实时层（当天增量）合并查询。

---

## 一、业务层加速：缓存（没学大数据也能用的方案）

### 技术实现

```
查询分析 → 查 Redis → 命中 → 返回
                  ↓ 未命中
              查 DB 聚合计算 → 写回 Redis（设 TTL）→ 返回
```

- **本地缓存**（Caffeine / Guava Cache）：应用内存，纳秒级，单机。
- **分布式缓存**（Redis）：跨实例共享，毫秒级，集群首选。
- **多级缓存**（L1 本地 + L2 Redis + DB）：热数据本地命中。

针对分析模块的 key 设计：

```
analyze:space:{spaceId}:category    // 分类统计
analyze:space:{spaceId}:size        // 大小分布
analyze:user:upload:trend           // 用户上传趋势
```

TTL 按业务可接受的新鲜度设（分析场景容忍分钟级滞后 → 5~30 分钟）。数据更新（新图上传）时可主动删对应 key，或只靠 TTL 过期。

### 缓存三大坑（分析场景也要防）

| 问题 | 触发                  | 防御                          |
|---|---------------------|-----------------------------|
| 穿透 | 查不存在的 key，每次打到 DB     | 缓存空值（null 占位，短 TTL）          |
| 击穿 | 热点 key 过期瞬间，大量请求打到 DB | 加锁（只让一个线程回源）、逻辑过期           |
| 雪崩 | 大量 key 同时过期          | TTL 加随机抖动、多级缓存               |

### 设计思想

- **空间换时间**：用内存换计算速度。
- **读写分离思想**：读走缓存，写回 DB。
- **最终一致性**：分析结果稍旧可接受，不必强一致。
- **适用前提**：读多写少 + 容忍 stale 数据 —— 分析结果完美匹配（变化不频繁）。

---

## 二、预计算：定时任务 + 结果表（定期分析的标准解）

### 核心思想

把昂贵的聚合计算提前做好，查询时只读结果。

### 技术实现

**1. 建结果表**（以"用户上传行为分析"为例）：

```sql
CREATE TABLE space_upload_analyze_daily (
    id BIGINT PRIMARY KEY,
    spaceId BIGINT,
    analyzeDate DATE,
    uploadCount BIGINT,      -- 当日上传数
    totalSize BIGINT,        -- 当日上传总大小
    uploaderCount INT,       -- 当日上传人数
    UNIQUE KEY uk_space_date (spaceId, analyzeDate)
);
```

**2. 定时任务每天凌晨跑一次**，算出昨天的结果写入：

```java
@Scheduled(cron = "0 0 2 * * ?")  // 凌晨2点
public void calcDailyUploadAnalyze() {
    // 统计每个空间昨日的上传情况，批量 INSERT 到结果表
    // 历史数据可一次性回刷
}
```

**3. 查询时直接按日期范围查结果表**（不再聚合）：

```sql
SELECT * FROM space_upload_analyze_daily
WHERE spaceId = ? AND analyzeDate BETWEEN ? AND ?
```

查 7 天趋势就查 7 行，极快。

**4. 当天数据怎么办？**

- 结果表只存到「昨天」，今天的数据走实时聚合（缓存兜底）→ 这就是 **Lambda 思想**（离线历史 + 实时当天）。
- 或每小时增量预计算一次，精度到小时。

### 设计思想

- **预计算 / 空间换时间**：查询时的重活挪到空闲时段提前做。
- **CQRS（命令查询职责分离）**：写（定时任务做重计算）和读（查询走结果表）用不同的数据路径 —— 写复杂、读极轻。
- **批处理摊薄成本**：一次算好全天，而不是每次查询重算。
- **Lambda 架构**：历史预计算 + 当天实时增量。

---

## 三、系统设计思想提炼

| 设计思想          | 体现                | 适用判断                  |
|---------------|-------------------|-----------------------|
| 空间换时间         | 缓存、预计算、结果表        | 算得慢但存得起 → 值得          |
| CQRS / 读写分离   | 写业务表、读结果表/缓存      | 读和写的形态差异大             |
| 预计算 vs 即时计算   | 定时任务 vs 实时聚合      | 高频查询 + 容忍延迟 → 预计算     |
| 最终一致性         | 缓存 TTL、T+1 报表     | 业务容忍短暂滞后              |
| Lambda / Kappa | 离线批 + 实时流         | 既要吞吐又要新鲜度             |
| 按需演进          | 单机→缓存→预计算→OLAP   | 别一上来就上大数据             |

> **核心权衡轴始终是三个：数据量 × 查询频率 × 新鲜度要求。** 三个都低 → 实时聚合；频率高/容忍延迟 → 缓存或预计算；数据量巨大 → OLAP 引擎；全都要 → 流批一体。

---

## 四、本项目演进路径（渐进式架构）

针对图库分析模块（空间使用 / 分类 / 标签 / 大小分析）：

| 阶段          | 方案             | 触发条件           | 实现                                                  |
|-------------|----------------|----------------|-----------------------------------------------------|
| 阶段 1（当前）    | 实时聚合 + 缓存      | 数据量小（单空间几千图）   | SQL `GROUP BY` 聚合 + Redis 缓存结果（TTL 5~30 分钟）        |
| 阶段 2        | 定时预计算 + 结果表    | 分析变慢、或要历史趋势    | 每日/每小时预计算，查询走结果表（用户上传趋势就用这招）                        |
| 阶段 3        | OLAP 引擎 / 流处理  | 数据量百万级、或要实时大屏  | 数据同步到 ClickHouse/Doris，或 Flink + Kafka 实时聚合         |

> 体现 **YAGNI + 渐进式架构**：先用最简单的（实时聚合）跑起来，哪个维度先扛不住就升级哪个，不要一上来就上大数据全家桶。

---

## 五、补充要点

- **增量计算**：定时任务不必每次全量重算，只算「上次以来新增的数据」（按 createTime 过滤），大幅减少计算量。
- **物化视图**：数据库层面的预计算（MySQL 触发器维护统计表，或 OLAP 引擎的物化视图），让预计算自动化。
- **冷热分离**：历史分析数据归档到冷存储/数仓，业务主库只留近期热数据。
- **降级策略**：缓存挂了/预计算没跑完时，降级到实时聚合（慢一点但能用），保证可用性。

---

### 一句话总结

实时/离线是两种范式 → 缓存和预计算是业务层最常用的两种加速手段 → 背后统一的思想是「空间换时间 + CQRS + 最终一致 + 按需演进」→ 落地时按数据量渐进升级。
6. **网页自动化别用硬编码的 URL 片段判断跳转**：百度改版把 `card=` 换成了 `session_id=`，跟着实际 URL 走，别假设。

---



# 2026/07/07

## SpaceUserAuthManager：RBAC 权限管理器 + findFirst 详解

### 一、SpaceUserAuthManager 是什么

基于 **RBAC（基于角色的访问控制）** 的团队空间权限管理器。核心职责：**给一个角色 → 返回它拥有的权限列表**。

这是团队空间模块的权限基础：不同成员在空间里有不同角色（viewer/editor/admin），角色决定能做什么操作。

### 二、RBAC 三层模型（用户 → 角色 → 权限）

```
用户(在空间里有角色, SpaceUser.spaceRole)
   ↓
角色 (viewer / editor / admin)            ← SpaceUserRole
   ↓
权限 (picture:view / picture:upload ...)   ← SpaceUserPermission
```

结合 `spaceUserAuthConfig.json`，角色权限对应：

| 角色             | 拥有的权限                                                 |
|----------------|-------------------------------------------------------|
| viewer（浏览者）    | `picture:view`                                        |
| editor（编辑者）    | `picture:view` + `upload` + `edit` + `delete`         |
| admin（管理员）     | 上面全部 + `spaceUser:manage`（管成员）                       |

**权限链路**：请求 → 拦截器拿到用户在该 space 的角色 → `getPermissionsByRole(role)` → 判断权限列表含不含本次操作 → 放行/拒绝。

### 三、类的设计亮点

**1. 配置驱动（JSON），不用数据库表**
权限规则相对固定，放 `biz/spaceUserAuthConfig.json`。优点：实现方便、查询高效。适合「角色权限映射不常变」的场景。

**2. 静态加载 + 单例常量（启动时读一次，运行时只读）**

```java
public static final SpaceUserAuthConfig SPACE_USER_AUTH_CONFIG;
static {
    String json = ResourceUtil.readUtf8Str("biz/spaceUserAuthConfig.json");
    SPACE_USER_AUTH_CONFIG = JSONUtil.toBean(json, SpaceUserAuthConfig.class);
}
```

`static` 块在类加载（应用启动）时读一次，存进 `public static final` 常量。之后每次鉴权直接读内存，不查库不读文件。这是「启动预热 + 全局单例」模式。

**3. `getPermissionsByRole` 防御式默认无权限**

```java
if (StrUtil.isBlank(spaceUserRole)) return new ArrayList<>();   // 角色空 → 无权限
SpaceUserRole role = ...findFirst()...orElse(null);             // 找匹配角色
if (role == null) return new ArrayList<>();                     // 角色不存在 → 无权限
return role.getPermissions();
```

任何异常情况（空、找不到）都默认返回空列表 = 无权限，安全。

### 四、findFirst 为什么用（核心，极易误解）

```java
SpaceUserRole role = SPACE_USER_AUTH_CONFIG.getRoles().stream()
        .filter(r -> spaceUserRole.equals(r.getKey()))
        .findFirst()
        .orElse(null);
```

**常见误解 ❌：`findFirst` = 「匹配多个时挑第一个」。**

**真相 ✅：`findFirst` 是 Stream API 里「从流里拿出一个元素」的方法名，不管匹配是一个还是多个。** Stream **没有** `getOne()` / `first()` / `get(0)` 这种「直接取」的写法（Stream 没有索引概念）。要从过滤后的流里取出一个元素，API 只给了 `findFirst()` 和 `findAny()` 两个选择。所以哪怕你心知肚明匹配只有一个，也只能写 `findFirst` —— 这是「取出一个元素」的唯一 API。

**等价 for 循环对比（秒懂）：**

```java
// stream().filter(...).findFirst() 完全等价于：
SpaceUserRole role = null;
for (SpaceUserRole r : SPACE_USER_AUTH_CONFIG.getRoles()) {
    if (spaceUserRole.equals(r.getKey())) {
        role = r;
        break;   // ← 遇到第一个匹配就停, 这就是 "findFirst"
    }
}
```

`findFirst` = **「从前往后遍历，遇到第一个匹配的就拿出来」**。即使整个列表只有一个 viewer 匹配，也得遍历到它、把它拿出来 —— 这个「拿出来」的动作，Stream 里就叫 `findFirst`。

> 名字里的 "first" 容易误导：不是「预期有多个所以取 first」，而是「从头遍历，**碰到的第一个**就返回」。只有一个的时候，那一个就是 first。

**findFirst vs findAny：**

| 方法          | 行为                                  | 适用            |
|-------------|-------------------------------------|---------------|
| `findFirst` | 取**第一个**（保留顺序）。串行流=第一个；并行流也保证顺序第一个 | 要确定的「第一个」     |
| `findAny`   | 取**任意一个**。串行流碰巧是第一个；并行流哪个分片先找到就用哪个  | 并行流求快，不在乎哪个   |

这里串行流 + 配置才 3 个角色，`findFirst` 足够，且语义更确定。

**隐含前提：角色 key 唯一。** `findFirst` 假设「匹配多个时取第一个就行」，依赖配置里 key 不重复（viewer/editor/admin 各一）。一般可控；想严格可在启动时加「key 唯一」校验。

**想「直接取」？转成 Map（可选优化）**

嫌 `findFirst` 别扭，是因为数据结构是 `List`（JSON 数组），List 只能按索引取、不能按 key 取。想直接取，启动时把 List 转成 Map：

```java
public static final Map<String, SpaceUserRole> ROLE_MAP;
static {
    ...读 json...
    ROLE_MAP = SPACE_USER_AUTH_CONFIG.getRoles().stream()
            .collect(Collectors.toMap(SpaceUserRole::getKey, r -> r));
}
// 用的时候直接 get, O(1), 语义直白, 没有 findFirst
SpaceUserRole role = ROLE_MAP.get(spaceUserRole);
```

角色少时 stream `findFirst` 无所谓；角色多了或想要语义清爽，转 Map 是正路。

### 五、小提醒（可选优化）

```java
return role.getPermissions();   // 直接返回配置内部的 list 引用
```

返回的是**全局配置对象内部的同一个 list**，调用方若意外 `add`/`remove` 会污染全局 `SPACE_USER_AUTH_CONFIG`。更稳的做法：

```java
return new ArrayList<>(role.getPermissions());               // 返回副本
// 或
return Collections.unmodifiableList(role.getPermissions());  // 不可变视图
```

---

### 一句话总结

`SpaceUserAuthManager` 是配置驱动的 RBAC 权限管理器（角色 → 权限），启动时静态加载 JSON 到全局常量；`findFirst` **不是「多个里挑第一个」，而是 Stream 里「把匹配元素拿出来」的唯一方法** —— 匹配只有一个时它就拿那一个。嫌别扭就转成 `Map` 直接 `get(key)`。