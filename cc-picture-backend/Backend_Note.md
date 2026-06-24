# 2025/05/31

## 如何实现图片上传和下载？

图片本质上是一种 “小型” 文件，那么我要思考：将文件上传到哪里？从哪里下载？

最简单的方式就是上传到后端项目所在的服务器，直接使用 Java 自带的文件读写 API 就能实现。但是，这种方式存在不少缺点，比如：

- 不利于扩展：单个服务器的存储是有限的，如果存满了，只能再新增存储空间或者清理文件。

- 不利于迁移：如果后端项目要更换服务器部署，之前所有的文件都要迁移到新服务器，非常麻烦。

- 不够安全：如果忘记控制权限，用户很有可能通过恶意代码访问服务器上的文件，而且想控制权限也比较麻烦，需要自己实现。

- 不利于管理：只能通过一些文件管理器进行简单的管理操作，但是缺乏数据处理、流量控制等多种高级能力。

因此，除了存储一些需要清理的临时文件之外，通常不会将用户上传并保存的文件（比如用户头像和图片）直接上传到服务器，而是更推荐使用专业的第三方存储服务，专业的工具做专业的事。其中，最常用的便是
对象存储
### 什么是对象存储？

对象存储是一种存储 海量文件 的 分布式 存储服务，具有高扩展性、低成本、可靠安全等优点。

比如开源的对象存储服务 MinIO，还有商业版的云服务，像亚马逊 S3（Amazon S3）、阿里云对象存储（OSS）、腾讯云对象存储（COS）等等

### 本项目採用 设计方案

创建图片其实包括了 2 个过程：上传图片文件 + 补充图片信息并保存到数据库中

有 2 种常见的处理方式：

- 1）先上传再提交数据：用户直接上传图片，系统生成图片的存储 URL；然后在用户填写其他相关信息并提交后，才保存图片记录到数据库中。

- 2）上传图片时直接保存记录：在用户上传图片后，系统立即生成图片的完整数据记录（包括图片 URL
  和其他元信息），无需等待用户点击提交，图片信息就立刻存入了数据库中。之后用户再填写其他图片信息，相当于编辑了已有图片记录的信息。

方案 1 的优点是流程简单，但缺点是如果用户不提交，图片会残留在存储中，导致空间浪费；方案 2 则可以理解为保存了
“图片草稿”，即使用户不填写任何额外信息，也能找到之前的创建记录。

```markdown
为什么图片先存到服务器还会浪费空间？存到服务器和存到数据库有什么区别？

图片存到服务器 = 图片文件本身存储在服务器的磁盘/对象存储（如OSS、S3）中，这是一个实际的文件
图片存到数据库 = 在数据库表中创建一条记录，记录包含图片URL、标题、描述等信息
所以方案1的问题是：

用户上传图片 → 图片文件已经存储在服务器磁盘上（占用物理空间）
用户不点提交 → 数据库中没有这条图片的记录
结果：有图片文件在磁盘上，但数据库里没记录这个图片存在
这就是"浪费空间"——文件存在但没人知道它该属于谁、是什么
而方案2：

用户上传图片 → 图片文件存到服务器 同时 数据库立刻创建记录
即使后续不填信息，至少数据库里有记录，可以通过记录管理这个图片
打个比方：

方案1：你把衣服放进柜子，但没在登记本上写下来 → 你忘了这衣服，衣服占用柜子空间但找不到
方案2：你把衣服放进柜子，同时在登记本上写好 → 你总能找到这衣服，还能补全信息
```

```markdown
---
## 两个不同的"存储"

| 存储位置 | 存的是什么 | 例子 |
|---------|----------|------|
| **服务器/对象存储** | 图片**文件本身**（实际的jpg/png文件） | 电脑硬盘里的 `photo.jpg` |
| **数据库** | 图片的**信息记录**（URL、标题、描述等） | Excel表里的一行数据 |

---

## 方案1为什么浪费空间？

```

用户上传图片
↓
图片文件存到服务器磁盘 ✓（占用物理空间）
↓
用户填写信息...
↓
用户【不点提交】，直接离开页面 ✗
↓
结果：数据库里没有记录

```

**问题：**
- 图片文件已经在服务器磁盘上（占用了存储空间）
- 但数据库里**没有这条图片的记录**
- 系统不知道这个图片属于谁、是什么时候上传的
- 变成了"无主图片"，只能靠定时清理任务来删除

---

## 打个比方

想象图书馆借书：

### 方案1（先上传后提交）
```

你把书放到书架上（存到服务器）
但没在登记本上写你的名字（没存数据库）
→ 书占位置，但没人知道是谁的
→ 图书管理员不知道该不该删这本书

```

### 方案2（上传即保存）
```

你把书放到书架上（存到服务器）
同时在登记本上写你的名字（存数据库）
→ 书有记录，管理员知道这是你的
→ 即使你没填完书评，管理员也能管理这本书

```
---
## 总结

- **存到服务器** = 实际占用磁盘空间
- **存到数据库** = 系统里"记得"这个图片的存在

方案1的问题是：图片占空间了，但系统"不记得"它，所以叫浪费。。
```

在我们的系统中，由于图片是核心资源，所以此处选择方案 2。 便于对图片进行溯源，还可以对图片上传做一些限制 ——
比如发现用户上传资源过多，就禁止上传

---

# 2025/06/01

## uploadPicture 方法详解

### 方法流程

```
用户上传图片 → MultipartFile
    ↓
1. 校验图片 (validPicture)
   - 文件不能为空
   - 大小不超过 2MB
   - 格式只能是 jpeg/jpg/png/webp
    ↓
2. 生成上传路径
   - 生成 16 位随机 UUID
   - 获取原文件名后缀
   - 拼接路径：/uploadPathPrefix/日期_UUID.后缀
    ↓
3. 创建临时文件
   - File.createTempFile(uploadPath, null)
   - multipartFile.transferTo(file) 将上传文件内容写入临时文件
    ↓
4. 上传到腾讯云 COS
   - cosManager.putPictureObject(uploadPath, file)
   - 获取图片信息（宽、高、格式等）
    ↓
5. 封装返回结果
   - UploadPictureResult（URL、尺寸、大小等）
    ↓
6. 清理临时文件 (deleteTempFile)
   - 在 finally 块中执行，保证无论成功失败都删除
```

### 为什么要创建临时文件？

**核心原因：腾讯云 COS SDK 的 API 限制**

腾讯云 COS 的 `putObject` 方法需要传入 `File` 对象（或 InputStream），不能直接传入 `MultipartFile`。

**详细原因：**
| 原因 | 说明 |
|------|------|
| API 要求 | COS SDK 的 `putObject(String key, File file)` 方法签名要求 File 对象 |
| 内存管理 | `MultipartFile` 默认存在内存中，大文件会导致内存溢出 |
| 上传效率 | File 对象在磁盘上，SDK 可以分片读取上传，更稳定 |
| 资源清理 | 临时文件用完即删，不会永久占用应用服务器空间 |

**关键代码：**

```java
// 创建临时文件
file =File.

createTempFile(uploadPath, null);

// 将上传文件内容写入临时文件
multipartFile.

transferTo(file);

// 使用临时文件上传到 COS
cosManager.

putPictureObject(uploadPath, file);

// 无论成功失败，都删除临时文件
this.

deleteTempFile(file); // 在 finally 块中
```

**临时文件生命周期：**

```
创建 → 写入数据 → 上传到 COS → 删除
```

**打个比方：**
就像你收到了一封信（MultipartFile）：

1. 你需要把这封信的内容抄到一张新纸上（创建临时 File）
2. 把这张纸交给快递员（上传到 COS）
3. 快递员拿走后，你把这张纸撕碎（删除临时文件）

**为什么不能直接把原件给快递员？** → 因为快递员只接受你抄写好的格式（File 对象）。

### 总结

| 问题        | 答案                                     |
|-----------|----------------------------------------|
| 为什么要临时文件？ | COS SDK 需要 File 对象，不能直接用 MultipartFile |
| 为什么最后要删除？ | 临时文件只在传递数据时需要，上传后就没用了                  |
| 不删除会怎样？   | 磁盘空间会被占满，最终服务器崩溃                       |

---

## 两种图片上传方案对比（通俗版）

### 方案1：先上传再提交（像「先占座，后点菜」）

**流程：**

1. 用户上传图片 → 图片先存到服务器，得到一个地址
2. 用户填写其他信息（标题、描述等）
3. 用户点击「提交」按钮 → 这时才在数据库中创建图片记录

**通俗理解：**

- 就像你先去餐厅占了个座位（上传图片），但还没点菜
- 如果你不点菜就直接走了（不点提交），座位就白占了
- 需要定期清理这些"占座但不点菜"的图片

### 方案2：上传即保存（像「拍照即存相册」）

**流程：**

1. 用户上传图片 → **立刻**在数据库创建记录，图片URL直接存入
2. 用户填写其他信息 → 相当于在"编辑"已保存的图片信息

**通俗理解：**

- 就像手机拍完照，照片立刻存入相册
- 之后你可以给照片加标签、写备注（编辑信息）
- 但照片本身已经稳稳地保存了，不会丢

### 核心区别

| 对比项      | 方案1（先上传后提交） | 方案2（上传即保存）    |
|----------|-------------|---------------|
| 数据库写入时机  | 用户点提交后      | 图片上传完成时       |
| 用户不提交会怎样 | 图片浪费占用空间    | 图片已保存，只是信息不完整 |
| 后续操作     | 创建新记录       | 编辑已有记录        |

**简单说：方案1是"暂存"，方案2是"直接存"**

---

## 生产环境代码完善建议

### 1. 空值校验

在 `PictureServiceImpl.java` 中，调用 `fileManager.uploadPicture()` 后需要做空值校验：

```java
UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
ThrowUtils.

throwIf(uploadPictureResult ==null, ErrorCode.SYSTEM_ERROR, "图片上传失败");
```

**原因：防御性编程**

- 虽然 `FileManager.uploadPicture()` 正常返回时不会返回 null（失败会抛异常）
- 但如果未来代码变更，可能引入 bug
- 符合防御性编程原则

### 2. 日志完善

生产环境需要补充关键日志，便于排查问题：

#### PictureServiceImpl.java

| 位置       | 日志类型 | 记录内容               |
|----------|------|--------------------|
| 方法入口     | info | 用户ID、pictureId、文件名 |
| 方法出口（成功） | info | 图片ID、URL、用户ID      |

```java

@Override
public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
    log.info("开始上传图片，用户ID = {}, pictureId = {}, 文件名 = {}",
            loginUser.getId(),
            pictureUploadRequest != null ? pictureUploadRequest.getId() : null,
            multipartFile != null ? multipartFile.getOriginalFilename() : null);

    // ... 业务逻辑 ...

    log.info("图片上传成功，图片ID = {}, URL = {}, 用户ID = {}", picture.getId(), picture.getUrl(), loginUser.getId());
    return PictureVO.objToVo(picture);
}
```

#### FileManager.java

| 位置      | 日志类型  | 记录内容        |
|---------|-------|-------------|
| 方法入口    | info  | 文件名、大小、路径前缀 |
| COS上传成功 | info  | 路径、耗时       |
| COS上传失败 | error | 路径、异常信息     |

```java
public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
    String originFilename = multipartFile.getOriginalFilename();
    long fileSize = multipartFile.getSize();
    log.info("开始上传图片到COS，文件名 = {}, 大小 = {}bytes, 路径前缀 = {}", originFilename, fileSize, uploadPathPrefix);

    // ... 上传逻辑 ...

    long startTime = System.currentTimeMillis();
    PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
    long endTime = System.currentTimeMillis();
    log.info("COS上传成功，路径 = {}, 耗时 = {}ms", uploadPath, endTime - startTime);

    // ... 封装结果 ...
}
```

### 生产环境日志的重要性

- **排查问题**：上传失败时能快速定位是 COS 问题还是数据库问题
- **性能监控**：通过上传耗时监控 COS 上传性能
- **审计追溯**：记录谁上传了什么图片
- **问题定位**：出现问题时能快速查找相关日志

---

## @RequestPart 注解详解

### 作用

`@RequestPart("file")` 是 Spring MVC 用于接收 **multipart/form-data** 类型请求的注解。

| 方面         | 说明                            |
|------------|-------------------------------|
| **用途**     | 处理文件上传请求（multipart/form-data） |
| **"file"** | 绑定请求中 name="file" 的部分         |
| **绑定对象**   | 通常绑定到 `MultipartFile` 类型      |

### 与 @RequestParam 的区别

```java
// @RequestParam - 用于普通表单参数
@PostMapping("/submit")
public void submit(@RequestParam("username") String username) {
}

// @RequestPart - 用于文件上传
@PostMapping("/upload")
public void upload(@RequestPart("file") MultipartFile file) {
}
```

### 前后端对应关系

**前端（HTML/JS）：**

```html

<form action="/upload" method="post" enctype="multipart/form-data">
    <!-- name="file" 与后端注解中的 "file" 对应 -->
    <input type="file" name="file">
    <button type="submit">上传</button>
</form>
```

**后端（Java）：**

```java

@PostMapping("/upload")
public BaseResponse<String> upload(@RequestPart("file") MultipartFile multipartFile) {
    // 处理文件上传
}
```

### 简单说

- 前端发送：`name="file"` 的文件字段
- 后端接收：`@RequestPart("file")` 绑定这个字段

---

## 性能优化

### 当前项目文件上传 会先在本地创建临时文件。如果不需要对文件进行额外的处理、想进一步提高性能，可以直接用流的方式将请求中的文件上传到 COS

```java
// 上传文件  
public static String uploadToCOS(MultipartFile multipartFile, String bucketName, String key) throws Exception {
    // 创建 COS 客户端  
    COSClient cosClient = createCOSClient();

    try (InputStream inputStream = multipartFile.getInputStream()) {
        // 元信息配置  
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());

        // 创建上传请求  
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, inputStream, metadata);

        // 上传文件  
        cosClient.putObject(putObjectRequest);

        // 生成访问链接  
        return "https://" + bucketName + ".cos." + cosClient.getClientConfig().getRegion().getRegionName()
                + ".myqcloud.com/" + key;
    } finally {
        cosClient.shutdown();
    }
}

```

---

# 2026/06/03

## `getPictureVOById` 和 `getPictureById` 这两个方法是否有必要拆分。

```java
// 根据 id 获取图片（封装类）- 所有可用
@GetMapping("/get/vo")
public BaseResponse<PictureVO> getPictureVOById(long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    return ResultUtils.success(pictureService.getPictureVO(picture, request));
}

// 根据 id 获取图片（仅管理员可用）
@GetMapping("/get")
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
public BaseResponse<Picture> getPictureById(long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    Picture picture = pictureService.getById(id);
    ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
    return ResultUtils.success(picture);
}
```

1. PictureVO 比 Picture 信息更丰富，为什么管理员反而要用信息更少的 Picture？
2. 两个方法是否有必要拆分？

关键点：

- **Picture** 是实体类，包含数据库所有字段（包括一些敏感信息如 userId）
- **PictureVO** 是视图类，是对外展示的，可能过滤了一些敏感字段

但实际上管理员需要看到 Picture 实体类的原因：

1. **管理需求**：管理员可能需要看到原始的数据库字段，比如 userId、创建时间、编辑时间等
2. **调试排查**：管理员排查问题时需要看到完整的原始数据
3. **编辑操作**：管理员编辑图片时需要获取完整的实体信息

而普通只需要看到 PictureVO（脱敏/简化后的信息）。

所以拆分是有必要的，但理由不是因为 PictureVO 比 Picture 信息多，而是因为：

- **权限控制不同**：普通 vs 管理员
- **返回数据不同**：VO 是给前端的，实体是给管理后台的
- **用途不同**：展示 vs 管理

## 两个方法的区别

|          | `getPictureVOById` | `getPictureById` |
|----------|--------------------|------------------|
| **权限**   | 所有                 | 仅管理员             |
| **返回类型** | `PictureVO`（视图类）   | `Picture`（实体类）   |
| **用途**   | 前台展示               | 后台管理/编辑          |

## 为什么管理员需要 Picture 实体类？

虽然 PictureVO 信息看起来更丰富（多了 user 用户信息），但 **Picture 实体类有管理端需要的原始字段**：

```java
// Picture 实体类有，PictureVO 可能没有的字段：
private Long userId;        // 上传者ID（编辑时需要回显）
private Date editTime;      // 编辑时间（管理端需要展示）
private String tags;        // JSON 字符串（原始格式，编辑时需要）
// ... 其他管理端专用字段
```

**举个例子：**

- **普通**访问图片详情 → `getPictureVOById` → 看到展示页面，tags 已经解析成 `List<String>`
- **管理员**编辑图片 → `getPictureById` → 拿到原始数据回填到编辑表单

## 简单说

```
前台看图片 → 用 VO（展示，脱敏，格式化好）
后台管理员管图片 → 用实体（编辑，原始数据完整）
```

**拆分的核心是权限和数据用途不同，不是信息量的多少。**不同，不是信息量的多少。**

```markdown
getPictureVOById → 所有可访问（前台展示）
getPictureById → 仅管理员可访问（后台管理）
```

---

## @TableField(typeHandler = JacksonTypeHandler.class)  注解

这是 MyBatis-Plus 的注解，用于处理 Java 对象与数据库字段之间的类型转换。
让 MyBatis-Plus 在**读写数据库时自动进行 JSON 转换**。

### 工作流程

```
Java 侧:  List<String> tags = ["风景", "自然", "山水"]
    ↓  写入数据库（自动序列化）
数据库侧:  tags = '["风景","自然","山水"]'
    ↓ 读取数据库（自动反序列化）
Java 侧:  List<String> tags = ["风景", "自然", "山水"]
```

### 对比：加与不加的区别

| 场景                    | 不加 typeHandler                             | 加了 typeHandler             |
|-----------------------|--------------------------------------------|----------------------------|
| **实体类字段类型**           | `String`（必须手动存 JSON 字符串）                   | `List<String>`（直接 Java 类型） |
| **写入数据库**             | 需要手动 `JSONUtil.toJsonStr(tags)`            | **自动**序列化为 JSON            |
| **读取数据库**             | 需要手动 `JSONUtil.toList(tags, String.class)` | **自动**反序列化为 List           |
| **objToVo / voToObj** | 需要手动转换 tags 类型                             | 不需要，类型一致                   |

### 代码对比

**不加 typeHandler（当前写法）：**

```java
// Picture 实体
private String tags;  // String 类型

// 写入时手动转
picture.

setTags(JSONUtil.toJsonStr(pictureUpdateRequest.getTags()));

// 读取时手动转
        pictureVO.

setTags(JSONUtil.toList(picture.getTags(),String.class));
```

**加了 typeHandler：**

```java
// Picture 实体
@TableField(typeHandler = JacksonTypeHandler.class)
private List<String> tags;  // 直接 List 类型

// 写入时直接
picture.

setTags(pictureUpdateRequest.getTags());  // 自动转 JSON

// 读取时直接
        pictureVO.

setTags(picture.getTags());  // 自动转 List
// 甚至不需要 objToVo 中手动转换了
```

**一个注解，省去所有手动 JSON 转换的代码。** MyBatis-Plus 帮你自动处理 Java 对象和 JSON 字符串之间的互转。串之间的互转。

---

## 其他相关注解

### 1. EnumTypeHandler（存枚举名称字符串）

**不使用 typeHandler（手动处理）：**

```java
// 实体类字段必须是 String
private String picFormat;

// 写入时手动转换
picture.

setPicFormat(PictureFormat.JPEG.name());  // 手动调用 name()

// 读取时手动转换
PictureFormat format = PictureFormat.valueOf(picture.getPicFormat());  // 手动解析
```

**使用 EnumTypeHandler（自动处理）：**

```java
// 实体类
@TableField(typeHandler = EnumTypeHandler.class)
private PictureFormat picFormat;

// 写入时直接赋值
picture.

setPicFormat(PictureFormat.JPEG);  // 自动存为 "JPEG"

// 读取时直接使用
PictureFormat format = picture.getPicFormat();  // 自动转为枚举
```

```
数据库中：     "JPEG"（字符串）
```

---

### 2. EnumOrdinalTypeHandler（存枚举序号）

**不使用 typeHandler（手动处理）：**

```java
// 实体类字段必须是 int
private Integer picFormat;

// 写入时手动转换
picture.

setPicFormat(PictureFormat.JPEG.ordinal());  // 手动调用 ordinal()

// 读取时手动转换
PictureFormat format = PictureFormat.values()[picture.getPicFormat()];  // 手动解析
```

**使用 EnumOrdinalTypeHandler（自动处理）：**

```java
// 实体类
@TableField(typeHandler = EnumOrdinalTypeHandler.class)
private PictureFormat picFormat;

// 写入时直接赋值
picture.

setPicFormat(PictureFormat.JPEG);  // 自动存为 0

// 读取时直接使用
PictureFormat format = picture.getPicFormat();  // 自动转为枚举
```

```
数据库中：    0（整数）
```

**两者对比：**

```
枚举定义：    JPEG(0), JPG(1), PNG(2), WEBP(3)

EnumTypeHandler       → 数据库存 "JPEG"    （可读，安全）
EnumOrdinalTypeHandler → 数据库存 0         （不可读，枚举顺序变了数据就错）
```

---

### 3. DateTypeHandler

**不使用 typeHandler（MyBatis 自动处理，通常不需要手动指定）：**

```java
// 实体类（无需注解，自动映射）
private Date createTime;

// 数据库字段类型：datetime
// MyBatis 自动完成 Date ↔ datetime 的转换
```

**需要自定义格式时（数据库存的是字符串格式的日期）：**

不使用 typeHandler（手动处理）：

```java
// 实体类字段必须是 String 
//  varchar，存的值是 "2026-06-03 14:30:00"
private String createTime;

// 写入时手动格式化
picture.

setCreateTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").

format(new Date()));

// 读取时手动解析
Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(picture.getCreateTime());
```

使用自定义 typeHandler（自动处理）：

```java

@TableField(typeHandler = DateStringTypeHandler.class)
private Date createTime;

// 写入时直接赋值
picture.

setCreateTime(new Date());  // 自动转为 "2026-06-03 14:30:00"

// 读取时直接使用
Date time = picture.getCreateTime();  // 自动转为 Date
```

```
数据库中：     "2026-06-03 14:30:00"（字符串）
```

---

### 总结

| TypeHandler                 | 不用时 Java 字段 | 使用后 Java 字段         | 数据库存储                   | 省去的手动操作                      |
|-----------------------------|-------------|---------------------|-------------------------|------------------------------|
| `EnumTypeHandler`           | `String`    | `PictureFormat`（枚举） | `"JPEG"`                | `.name()` / `valueOf()`      |
| `EnumOrdinalTypeHandler`    | `Integer`   | `PictureFormat`（枚举） | `0`                     | `.ordinal()` / `values()[n]` |
| 自定义 `DateStringTypeHandler` | `String`    | `Date`              | `"2026-06-03 14:30:00"` | `SimpleDateFormat` 格式化/解析    |

**核心：不用时字段必须是基础类型 + 手动转换，用了之后字段直接目标类型 + 自动转换。****

---

##  

```java
/**
 * 获取预置标签和分类
 * @return 预设的固定数据
 */
@GetMapping("/tag_category")
public BaseResponse<PictureTagCategory> listPictureTagCategory() {
    // 要支持用户根据标签和分类搜索图片，我们可以给用户列举一些常用的标签和分类，便于筛选
    // 在项目规模不大的时候，我们没必要将标签和分类单独用数据表来维护了
    // 前期直接在 PictureController 中写一个接口，返回预设的固定数据即可
    // TODO
    // 随着系统规模和数据不断扩大，可以再改为使用配置中心或数据库动态管理这些数据，或者通过定时任务计算出热门的图片分类和标签
    PictureTagCategory pictureTagCategory = new PictureTagCategory();
    List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
    List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
    pictureTagCategory.setTagList(tagList);
    pictureTagCategory.setCategoryList(categoryList);
    return ResultUtils.success(pictureTagCategory);
}
```

注释提到的三种扩展方案的示例代码：

1. 配置中心动态管理
2. 数据库动态管理
3. 定时任务计算热门标签/分类

---

## 方案1：配置中心动态管理（以 Nacos 为例）

```yaml
# Nacos 配置中心中的配置
picture:
  tag-category:
    tag-list: "热门,搞笑,生活,高清,艺术,校园,背景,简历,创意"
    category-list: "模板,电商,表情包,素材,海报"
```

```java

@RestController
@RequestMapping("/picture")
@RefreshScope  // Nacos 配置变更时自动刷新
public class PictureController {

    @Value("${picture.tag-category.tag-list}")
    private String tagListStr;

    @Value("${picture.tag-category.category-list}")
    private String categoryListStr;

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        pictureTagCategory.setTagList(Arrays.asList(tagListStr.split(",")));
        pictureTagCategory.setCategoryList(Arrays.asList(categoryListStr.split(",")));
        return ResultUtils.success(pictureTagCategory);
    }
```

**优点**：不用重启服务，在 Nacos 控制台改配置即可生效。

---

## 方案2：数据库动态管理

```sql
-- 建表
CREATE TABLE picture_tag_category
(
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    type       VARCHAR(20)  NOT NULL COMMENT '类型：tag/category',
    value      VARCHAR(100) NOT NULL COMMENT '标签或分类的值',
    sort_order INT DEFAULT 0 COMMENT '排序'
);
```

```java
// 实体类
@Data
@TableName(value = "picture_tag_category")
public class PictureTagCategoryEntity {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String type;    // "tag" 或 "category"
    private String value;
    private Integer sortOrder;
}

// Mapper
public interface PictureTagCategoryMapper extends BaseMapper<PictureTagCategoryEntity> {
}

// Controller
@GetMapping("/tag_category")
public BaseResponse<PictureTagCategory> listPictureTagCategory() {
    // 查询所有标签
    List<String> tagList = pictureTagCategoryMapper.selectList(
            new QueryWrapper<PictureTagCategoryEntity>().eq("type", "tag").orderByAsc("sort_order")
    ).stream().map(PictureTagCategoryEntity::getValue).collect(Collectors.toList());

    // 查询所有分类
    List<String> categoryList = pictureTagCategoryMapper.selectList(
            new QueryWrapper<PictureTagCategoryEntity>().eq("type", "category").orderByAsc("sort_order")
    ).stream().map(PictureTagCategoryEntity::getValue).collect(Collectors.toList());

    PictureTagCategory pictureTagCategory = new PictureTagCategory();
    pictureTagCategory.setTagList(tagList);
    pictureTagCategory.setCategoryList(categoryList);
    return ResultUtils.success(pictureTagCategory);
}
```

**优点**：管理员可以通过后台界面增删改标签和分类。

---

## 方案3：定时任务计算热门标签/分类

```java

@Component
@Slf4j
public class HotTagCategoryTask {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 每天凌晨2点统计热门标签和分类
     */
    @Scheduled(cron = "0 0 2 * ?")
    public void calculateHotTagsAndCategories() {
        log.info("开始统计热门标签和分类");

        // 1. 统计最近30天内每个标签的使用次数
        // 假设 tags 存的是 JSON 数组， SQL 模糊统计
        // 这里简化处理，实际项目中可能需要更复杂的统计逻辑

        // 查询最近30天的图片数据
        List<Picture> recentPictures = pictureMapper.selectList(
                new QueryWrapper<Picture>()
                        .ge("createTime", LocalDate.now().minusDays(30))
                        .select("tags", "category")
        );

        // 2. 统计标签出现频率
        Map<String, Integer> tagCountMap = new HashMap<>();
        for (Picture picture : recentPictures) {
            if (StrUtil.isNotBlank(picture.getTags())) {
                List<String> tags = JSONUtil.toList(picture.getTags(), String.class);
                tags.forEach(tag -> tagCountMap.merge(tag, 1, Integer::sum));
            }
        }

        // 3. 统计分类出现频率
        Map<String, Integer> categoryCountMap = new HashMap<>();
        for (Picture picture : recentPictures) {
            if (StrUtil.isNotBlank(picture.getCategory())) {
                categoryCountMap.merge(picture.getCategory(), 1, Integer::sum);
            }
        }

        // 4. 按使用次数降序排序，取前10个
        List<String> hotTagList = tagCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> hotCategoryList = categoryCountMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        // 5. 缓存到 Redis
        redisTemplate.opsForValue().set("hot:tags", hotTagList, 1, TimeUnit.DAYS);
        redisTemplate.opsForValue().set("hot:categories", hotCategoryList, 1, TimeUnit.DAYS);

        log.info("热门标签统计完成：{}", hotTagList);
        log.info("热门分类统计完成：{}", hotCategoryList);
    }
```

```java
// Controller 中读取 Redis 缓存
@GetMapping("/tag_category")
public BaseResponse<PictureTagCategory> listPictureTagCategory() {
    PictureTagCategory pictureTagCategory = new PictureTagCategory();
    // 从 Redis 读取热门标签和分类
    List<String> hotTagList = (List<String>) redisTemplate.opsForValue().get("hot:tags");
    List<String> hotCategoryList = (List<String>) redisTemplate.opsForValue().get("hot:categories");
    pictureTagCategory.setTagList(hotTagList != null ? hotTagList : Collections.emptyList());
    pictureTagCategory.setCategoryList(hotCategoryList != null ? hotCategoryList : Collections.emptyList());
    return ResultUtils.success(pictureTagCategory);
}
```

**优点**：根据实际使用情况动态展示热门标签，自动更新。

---

## 三种方案对比

| 方案          | 适用阶段 | 修改方式         | 实时性  | 复杂度 |
|-------------|------|--------------|------|-----|
| **硬编码**（当前） | 项目初期 | 改代码重新部署      | 无    | 最低  |
| **配置中心**    | 中期   | Nacos 控制台改配置 | 准实时  | 低   |
| **数据库**     | 中期   | 后台管理界面增删改    | 实时   | 中   |
| **定时任务**    | 后期   | 自动统计，无需人工    | 每天更新 | 高   |

随着系统规模扩大，可以逐步演进：**硬编码 → 配置中心 → 数据库 → 定时任务**，甚至组合使用。时任务**，甚至组合使用。

## 集群部署 定时任务需要分布式锁

如果服务部署了多个实例（集群），多个实例会同时执行同一个定时任务，导致：

- 重复计算
- 重复写入 Redis
- 浪费资源

**单机部署不需要，集群部署必须加。**

### 不加分布式锁的问题

```
实例1: 凌晨2点触发定时任务 → 查数据库 → 写Redis
实例2: 凌晨2点触发定时任务 → 查数据库 → 写Redis  ← 重复执行
实例3: 凌晨2点触发定时任务 → 查数据库 → 写Redis  ← 重复执行
```

### 加了分布式锁后

```
实例1: 凌晨2点获取锁成功 → 执行任务 → 释放锁
实例2: 凌晨2点获取锁失败 → 跳过
实例3: 凌晨2点获取锁失败 → 跳过
```

### 常分布式锁实现

| 方案                 | 适用场景                 | 复杂度 |
|--------------------|----------------------|-----|
| **Redis Redisson** | 最常用，Spring Boot 项目首选 | 低   |
| **Redis SETNX**    | 轻量级                  | 低   |
| **Zookeeper**      | 强一致性要求高              | 高   |
| **数据库锁**           | 不想引入新中间件             | 中   |

### 以 Redisson 为例的代码

```java

@Component
@Slf4j
public class HotTagCategoryTask {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private RedissonClient redissonClient;

    /**
     * 每天凌晨2点统计热门标签和分类
     */
    @Scheduled(cron = "0 0 2 * ?")
    public void calculateHotTagsAndCategories() {
        // 获取分布式锁，锁的key要唯一，等待时间0（获取不到立即放弃），过期时间1小时（防止死锁）
        RLock lock = redissonClient.getLock("hot:tag:category:lock");
        try {
            boolean tryLock = lock.tryLock(0, 360, TimeUnit.SECONDS);
            if (!tryLock) {
                log.info("未获取到分布式锁，跳过本次执行");
                return;
            }
            // 执行统计逻辑...
            log.info("获取分布式锁成功，开始统计热门标签和分类");

            // 查询最近30天的图片数据
            List<Picture> recentPictures = pictureMapper.selectList(
                    new QueryWrapper<Picture>()
                            .ge("createTime", LocalDate.now().minusDays(30))
                            .select("tags", "category")
            );

            // 统计标签出现频率
            Map<String, Integer> tagCountMap = new HashMap<>();
            for (Picture picture : recentPictures) {
                if (StrUtil.isNotBlank(picture.getTags())) {
                    List<String> tags = JSONUtil.toList(picture.getTags(), String.class);
                    tags.forEach(tag -> tagCountMap.merge(tag, 1, Integer::sum));
                }
            }

            // 统计分类出现频率
            Map<String, Integer> categoryCountMap = new HashMap<>();
            for (Picture picture : recentPictures) {
                if (StrUtil.isNotBlank(picture.getCategory())) {
                    categoryCountMap.merge(picture.getCategory(), 1, Integer::sum);
                }
            }

            // 按频率降序排序，取前10个
            List<String> hotTagList = tagCountMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            List<String> hotCategoryList = categoryCountMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            // 缓存到 Redis
            redisTemplate.opsForValue().set("hot:tags", hotTagList, 1, TimeUnit.DAYS);
            redisTemplate.opsForValue().set("hot:categories", hotCategoryList, 1, TimeUnit.DAYS);

            log.info("热门标签统计完成：{}", hotTagList);
            log.info("热门分类统计完成：{}", hotCategoryList);
        } catch (InterruptedException e) {
            log.error("获取分布式锁异常", e);
        } finally {
            // 释放锁（只有锁的持有者才能释放）
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
```

### 简单说

| 场景        | 是否需要分布式锁 |
|-----------|----------|
| 单机部署      | 不需要      |
| 集群部署（多实例） | **必须加**  |

**核心逻辑：多个实例抢同一把锁，抢到的执行，抢不到的跳过。**的跳过。**

# 其他分布式锁实现实例

## 方案1：Zookeeper 分布式锁

```java

@Component
@Slf4j
public class HotTagCategoryTask {

    @Resource
    private PictureMapper pictureMapper;

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Resource
    private CuratorFramework curatorFramework;

    /**
     * 每天凌晨2点统计热门标签和分类
     */
    @Scheduled(cron = "0 0 2 * ?")
    public void calculateHotTagsAndCategories() {
        // 创建 Zookeeper 分布式锁（节点路径要唯一）
        InterProcessMutex lock = new InterProcessMutex(curatorFramework, "/lock/hot-tag-category");
        try {
            // 尝试获取锁，等待时间3秒，过期时间1小时
            boolean acquired = lock.acquire(3, TimeUnit.SECONDS);
            if (!acquired) {
                log.info("未获取到Zookeeper分布式锁，跳过本次执行");
                return;
            }
            log.info("获取Zookeeper分布式锁成功，开始统计热门标签和分类");

            // 执行统计逻辑...
            doCalculate();

        } catch (Exception e) {
            log.error("热门标签统计异常", e);
        } finally {
            try {
                // 释放锁
                if (lock.isAcquiredInThisProcess()) {
                    lock.release();
                }
            } catch (Exception e) {
                log.error("释放Zookeeper锁异常", e);
            }
        }
    }

    private void doCalculate() {
        // 统计逻辑（同之前）
    }
```

**配置类：**

```java

@Configuration
public class ZookeeperConfig {

    @Value("${zookeeper.connect-string}")
    private String connectString;

    @Bean
    public CuratorFramework curatorFramework() {
        CuratorFramework client = CuratorFrameworkFactory.builder()
                .connectString(connectString)          // 如 "localhost:2181"
                .sessionTimeoutMs(500)                 // 会话超时5秒
                .connectionTimeoutMs(300)               // 连接超时3秒
                .retryPolicy(new ExponentialBackoffRetry(100, 3))  // 重试策略
                .build();
        client.start();
        return client;
    }
```

**application.yml：**

```yaml
zookeeper:
  connect-string: localhost:2181
```

---

## 方案2：数据库锁

### 方式一：唯一索引（推荐简单场景）

**建表：**

```sql
CREATE TABLE distributed_lock
(
    lock_key    VARCHAR(100) PRIMARY KEY COMMENT '锁标识',
    lock_holder VARCHAR(100) COMMENT '持有者（实例标识）',
    expire_time BIGINT COMMENT '过期时间戳',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

**获取锁和释放锁：**

```java

@Component
@Slf4j
public class DatabaseLockManager {

    @Resource
    private JdbcTemplate jdbcTemplate;

    /**
     * 尝试获取锁
     */
    public boolean tryLock(String lockKey, String holder, long expireSeconds) {
        long now = System.currentTimeMillis();
        long expireTime = now + expireSeconds * 100;

        try {
            // 先尝试插入（唯一索引保证只有一个实例能插入成功）
            int inserted = jdbcTemplate.update(
                    "INSERT INTO distributed_lock (lock_key, lock_holder, expire_time) VALUES (?, ?, ?)",
                    lockKey, holder, expireTime
            );
            return inserted > 0;
        } catch (DuplicateKeyException e) {
            // 插入失败说明锁已被占用，检查是否过期
            Long oldExpireTime = jdbcTemplate.queryForObject(
                    "SELECT expire_time FROM distributed_lock WHERE lock_key = ?",
                    Long.class, lockKey
            );
            if (oldExpireTime != null && oldExpireTime < now) {
                // 锁已过期，尝试抢占（CAS 操作）
                int updated = jdbcTemplate.update(
                        "UPDATE distributed_lock SET lock_holder = ?, expire_time = ? WHERE lock_key = ? AND expire_time = ?",
                        holder, expireTime, lockKey, oldExpireTime
                );
                return updated > 0;
            }
            return false;
        }
    }

    /**
     * 释放锁（只有持有者才能释放）
     */
    public boolean unlock(String lockKey, String holder) {
        int deleted = jdbcTemplate.update(
                "DELETE FROM distributed_lock WHERE lock_key = ? AND lock_holder = ?",
                lockKey, holder
        );
        return deleted > 0;
    }
```

**使用：**

```java

@Component
@Slf4j
public class HotTagCategoryTask {

    @Resource
    private DatabaseLockManager databaseLockManager;

    @Scheduled(cron = "0 0 2 * ?")
    public void calculateHotTagsAndCategories() {
        String lockKey = "hot_tag_category_lock";
        String holder = UUID.randomUUID().toString();  // 当前实例唯一标识
        try {
            boolean locked = databaseLockManager.tryLock(lockKey, holder, 360);
            if (!locked) {
                log.info("未获取到数据库分布式锁，跳过本次执行");
                return;
            }
            log.info("获取数据库分布式锁成功，开始统计热门标签和分类");

            // 执行统计逻辑...
            doCalculate();

        } catch (Exception e) {
            log.error("热门标签统计异常", e);
        } finally {
            databaseLockManager.unlock(lockKey, holder);
        }
    }
```

---

### 方式二：SELECT FOR UPDATE（行级悲观锁）

```java

@Component
@Slf4j
public class HotTagCategoryTask {

    @Resource
    private DataSource dataSource;

    @Scheduled(cron = "0 0 2 * ?")
    public void calculateHotTagsAndCategories() {
        try (Connection conn = dataSource.getConnection()) {
            // 关闭自动提交，开启事务
            conn.setAutoCommit(false);

            // 加行级排他锁（如果该行被其他事务锁定，会阻塞等待）
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT lock_key FROM distributed_lock WHERE lock_key = 'hot_tag_category_lock' FOR UPDATE")) {
                ResultSet rs = ps.executeQuery();

                if (rs.next()) {
                    log.info("获取数据库悲观锁成功，开始统计");
                    // 执行统计逻辑...
                    doCalculate();
                }
            }
            // 提交事务 = 释放锁
            conn.commit();

        } catch (Exception e) {
            log.error("热门标签统计异常", e);
        }
    }
```

---

## 四种分布式锁对比

| 方案                 | 实现复杂度 | 性能     | 可靠性          | 适用场景               |
|--------------------|-------|--------|--------------|--------------------|
| **Redis Redisson** | 低     | 高      | 高            | 最常用，Spring Boot 首选 |
| **Redis SETNX**    | 低     | 高      | 中            | 轻量级，不引入框架          |
| **Zookeeper**      | 高     | 中      | **最高**（强一致性） | 对一致性要求极高的场景        |
| **数据库唯一索引**        | 中     | **最低** | 中            | 不想引入新中间件的小项目       |
| **数据库悲观锁**         | 中     | **最低** | 中            | 已有数据库的项目临时方案       |

**当前项目推荐**：Redis Redisson（简单高效），如果不想引入 Redis，数据库唯一索引方案即可。引入 Redis，用数据库唯一索引方案即可。

---

# 2026/06/04

## 图片并发审核问题分析

### doPictureReview 方法是否存在权限中途被撤销的风险？

执行时间线：

```
请求1：管理员审核图片
    ↓
@AuthCheck 校验权限 ✓（此时还是管理员）
    ↓
doPictureReview 执行中...
    ↓                      ← 时间窗口（毫秒级）
updateById 操作数据库 ✓
    ↓
请求结束

请求2：撤销管理员权限（几乎同时发生）
    ↓
修改数据库 user.role
```

**结论：当前单体应用中基本没有风险**，因为整个请求处理是毫秒级的。

### 并发审核（更实际的风险）

两个管理员同时审核同一张图片：

```
管理员A：审核通过 → reviewStatus = 1
管理员B：审核拒绝 → reviewStatus = 2（覆盖了A的结果）
```

**影响评估：** 不涉及资金、库存等关键数据，只是最终审核状态以谁后执行为准，业务上可以接受。且已有重复审核校验能防住大部分情况：

```java
if(oldPicture.getReviewStatus().

equals(reviewStatus)){
        throw new

BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
}
```

### 如果要加乐观锁，SQL 对比

**不加乐观锁（当前）：**

```sql
UPDATE picture
SET reviewStatus = 1,
    reviewerId   = 100,
    reviewTime   = '2026-06-03'
WHERE id = 123
```

**加了乐观锁：**

```sql
UPDATE picture
SET reviewStatus = 1,
    reviewerId   = 100,
    reviewTime   = '2026-06-03'
WHERE id = 123
  AND reviewStatus = 0
```

区别就是 WHERE 多了一个 `AND reviewStatus = 0`。如果被其他管理员改了，条件不匹配，更新行数为 0。

### 性能影响

| 方式                     | 额外开销         | 是否会阻塞等待 |
|------------------------|--------------|---------|
| 乐观锁                    | 多一个 WHERE 条件 | 不会      |
| 悲观锁（SELECT FOR UPDATE） | 需要加行锁        | 会阻塞等待   |

### 总结

| 问题        | 答案                    |
|-----------|-----------------------|
| 并发审核重要吗？  | 不重要，不涉及资金等关键数据        |
| 需要加乐观锁吗？  | 当前项目不需要               |
| 乐观锁影响性能吗？ | 几乎不影响（多一个 WHERE 条件而已） |

**当前项目保持现状就好。** 如果未来是订单审核、资金审核这类场景，才需要考虑乐观锁。

### 如果要严格处理权限中途变更

可以在执行操作前再校验一次：

```java
// 二次校验权限（防止权限中途被撤销）
User freshUser = userService.getById(loginUser.getId());
if(!UserRoleEnum.ADMIN.

getValue().

equals(freshUser.getUserRole())){
        throw new

BusinessException(ErrorCode.NO_AUTH_ERROR, "权限已被撤销");
}
```

---

# 2026/06/06

## Manager 包：文件上传逻辑的模板方法模式演变

### FileManager 的两种上传方式（旧代码）

在重构之前，`FileManager` 类中存在两个上传方法，它们的核心流程完全一致，但实现细节不同：

#### 方法1：本地文件上传 `uploadPicture(MultipartFile, String)`

```java
public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
    // 1. 校验图片（本地文件）
    validPicture(multipartFile);

    // 2. 生成上传路径
    String uuid = RandomUtil.randomString(16);
    String originFilename = multipartFile.getOriginalFilename();
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

    File file = null;
    try {
        // 3. 创建临时文件
        file = File.createTempFile(uploadPath, null);
        multipartFile.transferTo(file);

        // 4. 上传到COS
        PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);

        // 5. 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // ... 设置各种属性 ...
        return uploadPictureResult;
    } finally {
        // 6. 清理临时文件
        this.deleteTempFile(file);
    }
}
```

#### 方法2：URL上传 `uploadPictureByUrl(String, String)`

```java
public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
    // 1. 校验图片（URL方式校验）
    validPicture(fileUrl);

    // 2. 生成上传路径（类似）
    String uuid = RandomUtil.randomString(16);
    String originFilename = FileUtil.mainName(fileUrl);
    String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

    File tempFile = null;
    try {
        // 3. 创建临时文件
        tempFile = File.createTempFile(uploadPath, null);
        HttpUtil.downloadFile(fileUrl, tempFile);  // ← 不同点：从URL下载

        // 4. 上传到COS（完全相同）
        PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);

        // 5. 封装返回结果（完全相同）
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        // ... 设置各种属性（相同代码）...
        return uploadPictureResult;
    } finally {
        // 6. 清理临时文件（完全相同）
        this.deleteTempFile(tempFile);
    }
}
```

### 问题分析

两个方法的流程**完全一致**，但存在**大量重复代码**：

| 步骤        | 本地上传                            | URL上传                           | 是否相同 |
|-----------|---------------------------------|---------------------------------|------|
| 1. 校验图片   | `validPicture(MultipartFile)`   | `validPicture(String)`          | ❌ 不同 |
| 2. 生成路径   | 从 MultipartFile 获取              | 从 URL 获取                        | ❌ 不同 |
| 3. 创建临时文件 | `multipartFile.transferTo()`    | `HttpUtil.downloadFile()`       | ❌ 不同 |
| 4. 上传COS  | `cosManager.putPictureObject()` | `cosManager.putPictureObject()` | ✓ 相同 |
| 5. 封装结果   | 相同代码                            | 相同代码                            | ✓ 相同 |
| 6. 清理文件   | `deleteTempFile()`              | `deleteTempFile()`              | ✓ 相同 |

**代码重复率超过60%**，这是典型的"流程相同、细节不同"场景。

---

### 重构：模板方法模式

#### 核心思想

将通用流程定义在抽象类中，将不同的部分抽象为方法，由子类实现。

#### 模板抽象类 `PictureUploadTemplate`

```java

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    protected CosManager cosManager;

    @Resource
    protected CosClientConfig cosClientConfig;

    /**
     * 模板方法：定义上传流程（final防止子类修改）
     */
    public final UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1. 校验图片（抽象方法，子类实现）
        validPicture(inputSource);

        // 2. 图片上传地址（抽象方法，子类实现获取文件名）
        String uuid = RandomUtil.randomString(16);
        String originFilename = getOriginFilename(inputSource);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 处理文件来源（抽象方法，子类实现）
            processFile(inputSource, file);

            // 4. 上传图片到对象存储
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();

            // 5. 封装返回结果（通用逻辑）
            return buildResult(originFilename, file, uploadPath, imageInfo);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败，路径 = {}", uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件（通用逻辑）
            deleteTempFile(file);
        }
    }

    // ===== 三个抽象方法，由子类实现 =====

    /**
     * 校验输入源（本地文件或 URL）
     */
    protected abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    protected abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws Exception;

    // ===== 通用私有方法 =====

    private UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
        return uploadPictureResult;
    }

    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
```

---

#### 子类实现1：FilePictureUpload（本地文件上传）

```java

@Service
public class FilePictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");

        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        final long ONE_M = 1024 * 1024L;
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");

        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        return multipartFile.getOriginalFilename();
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        MultipartFile multipartFile = (MultipartFile) inputSource;
        multipartFile.transferTo(file);
    }
}
```

---

#### 子类实现2：UrlPictureUpload（URL上传）

```java

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");

        try {
            // 1. 校验url格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL格式不正确");
        }

        // 2. 校验URL协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求验证文件
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 4. 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }

            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    final long ONE_M = 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
```

---

### 重构对比总结

#### 代码结构对比

| 方面       | 旧代码（FileManager） | 新代码（模板方法模式）    |
|----------|------------------|----------------|
| **类数量**  | 1个类              | 1个抽象类 + 2个子类   |
| **重复代码** | 大量重复（60%+）       | 几乎无重复          |
| **扩展性**  | 新增上传方式需修改现有类     | 新增上传方式只需新增子类   |
| **可维护性** | 修改一处需要同步修改另一处    | 修改模板类自动应用到所有子类 |
| **代码量**  | ~270行            | 总计~250行，但结构更清晰 |

#### 职责划分

```
┌─────────────────────────────────────────────────┐
│         PictureUploadTemplate（抽象模板）          │
│  ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  │
│  ✓ 定义通用上传流程                              │
│  ✓ 实现通用逻辑（路径生成、COS上传、结果封装）    │
│  ✗ 抽象差异化方法（校验、获取文件名、处理文件）    │
└─────────────────────────────────────────────────┘
           △                        △
           │                        │
    ┌──────┴────────┐      ┌───────┴────────┐
    │ FilePictureUpload│    │ UrlPictureUpload│
    │  （本地文件上传）  │    │  （URL上传）     │
    └─────────────────┘    └─────────────────┘
```

#### 使用方式对比

**旧代码：**

```java

@Resource
private FileManager fileManager;

// 本地文件上传
UploadPictureResult result1 = fileManager.uploadPicture(multipartFile, "/picture");

// URL上传
UploadPictureResult result2 = fileManager.uploadPictureByUrl(fileUrl, "/picture");
```

**新代码：**

```java

@Resource
private FilePictureUpload filePictureUpload;

@Resource
private UrlPictureUpload urlPictureUpload;

// 本地文件上传
UploadPictureResult result1 = filePictureUpload.uploadPicture(multipartFile, "/picture");

// URL上传
UploadPictureResult result2 = urlPictureUpload.uploadPicture(fileUrl, "/picture");
```

---

### 设计模式总结

#### 模板方法模式的适用场景

当满足以下条件时，应使用模板方法模式：

1. **流程相同**：多个操作的核心流程一致
2. **细节不同**：某些步骤的实现方式不同
3. **避免重复**：存在大量重复代码
4. **需要扩展**：未来可能新增类似的实现

#### 本项目中的应用

| 场景        | 流程                   | 差异点             |
|-----------|----------------------|-----------------|
| 文件上传      | 校验→路径→临时文件→COS→结果→清理 | 校验方式、获取文件名、处理文件 |
| （可扩展）支付流程 | 验证→扣款→通知             | 支付渠道、签名方式、回调处理  |

#### 关键设计点

1. **模板方法用 final 修饰**：防止子类修改核心流程
2. **抽象方法 protected**：子类可见，外部不可见
3. **通用方法 private**：不暴露给子类
4. **依赖注入**：子类通过 @Resource 继承父类的依赖

#### 扩展性示例

如果要新增"Base64上传"方式，只需：

```java

@Service
public class Base64PictureUpload extends PictureUploadTemplate {

    @Override
    protected void validPicture(Object inputSource) {
        String base64 = (String) inputSource;
        // 校验 Base64 格式
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        // 从 Base64 中提取文件名
        return "base64_upload";
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String base64 = (String) inputSource;
        // 解码 Base64 并写入文件
    }
}
```

无需修改任何现有代码，符合**开闭原则**（对扩展开放，对修改关闭）。

# 2026/06/09

● uploadPictureByBatch
方法详解                                                                                                                                                                                            
这个方法的核心功能是：从 Bing
图片搜索批量抓取图片并上传到系统。                                                               
流程图

用户输入关键词 → 搜索 Bing 图片 → 解析 HTML 提取图片 URL → 循环上传每张图片

---
代码逐行解析

```java
public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
}
```

作用：批量上传图片，返回成功上传的数量

---
第1步：提取参数

```java
String searchText = pictureUploadByBatchRequest.getSearchText();  // 搜索关键词，如"风景"
Integer count = pictureUploadByBatchRequest.getCount();            // 要上传的数量
ThrowUtils.

throwIf(count >30, ErrorCode.PARAMS_ERROR, "最多 30 条");  // 限制最多30条，防止滥用
```

---
第2步：构造 Bing 搜索 URL

String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);

示例：searchText = "风景"
→ fetchUrl = "https://cn.bing.com/images/async?q=风景&mmasync=1"

这是一个 Bing 图片搜索的异步接口，返回包含图片列表的 HTML。

---
第3步：用 Jsoup 抓取页面

Document document;
try {
document = Jsoup.connect(fetchUrl).get(); // 发送 HTTP 请求，获取 HTML 文档
} catch (IOException e) {
log.error("获取页面失败", e);
throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
}

Jsoup 是一个 Java HTML 解析库，可以像 jQuery 一样操作 DOM。

  ---
第4步：定位图片元素

Element div = document.getElementsByClass("dgControl").first(); // 找到 class="dgControl" 的 div
if (ObjUtil.isNull(div)) {
throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
}
Elements imgElementList = div.select("img.mimg"); // 在该 div 下查找所有 class="mimg" 的 img 标签

Bing 返回的 HTML 结构大致如下：
  <div class="dgControl">
      <img class="mimg" src="https://...1.jpg">
      <img class="mimg" src="https://...2.jpg">
      <img class="mimg" src="https://...3.jpg">
      ...
  </div>

  ---
第5步：循环上传每张图片

int uploadCount = 0; // 成功上传计数器
for (Element imgElement : imgElementList) { // 遍历每个 img 元素
String fileUrl = imgElement.attr("src"); // 获取 src 属性（图片 URL）

      if (StrUtil.isBlank(fileUrl)) {
          log.info("当前链接为空，已跳过: {}", fileUrl);
          continue;  // 跳过空链接
      }

      // 处理图片 URL：去掉问号后的参数（防止转义问题）
      int questionMarkIndex = fileUrl.indexOf("?");
      if (questionMarkIndex > -1) {
          fileUrl = fileUrl.substring(0, questionMarkIndex);
      }
      // 示例：https://example.com/img.jpg?w=200&h=150 → https://example.com/img.jpg

      // 调用单图上传方法
      PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
      try {
          PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
          log.info("图片上传成功, id = {}", pictureVO.getId());
          uploadCount++;  // 成功则计数+1
      } catch (Exception e) {
          log.error("图片上传失败", e);
          continue;  // 失败则跳过当前图片，继续下一张
      }

      if (uploadCount >= count) {  // 达到目标数量，停止循环
          break;
      }

}

  ---
第6步：返回结果

return uploadCount; // 返回实际成功上传的数量

  ---
总结

┌──────┬──────────────┬───────────────────────────────┐
│ 步骤 │ 功能 │ 技术点 │
├──────┼──────────────┼───────────────────────────────┤
│ 1 │ 参数校验 │ 限制最多 30 张 │
├──────┼──────────────┼───────────────────────────────┤
│ 2 │ 构造搜索 URL │ Bing 图片 API │
├──────┼──────────────┼───────────────────────────────┤
│ 3 │ 抓取 HTML │ Jsoup HTTP 请求 │
├──────┼──────────────┼───────────────────────────────┤
│ 4 │ 解析 DOM │ CSS 选择器 │
├──────┼──────────────┼───────────────────────────────┤
│ 5 │ 循环上传 │ 复用已有的 uploadPicture 方法 │
├──────┼──────────────┼───────────────────────────────┤
│ 6 │ 返回结果 │ 实际上传数量 │
└──────┴──────────────┴───────────────────────────────┘

关键设计思路

1. 复用已有代码：调用 uploadPicture(fileUrl, ...)，URL 上传逻辑已在前面实现过
2. 容错处理：单张失败不影响整体，continue 跳过
3. 数量限制：防止用户一次抓取过多图片
4. URL 清理：去掉 URL 参数避免转义问题

---

## URL 上传文件后缀丢失问题修复

### 问题现象

通过 URL 上传图片后，腾讯云 COS 保存的 URL **没有文件类型后缀**：

```
https://cc-picture-1308624837.cos.ap-shanghai.myqcloud.com/public/2058909011734790145/2026-06-09_8CtEnO3SPsc0aoXb.
```

URL 以 `.` 结尾，后面应该是 `.jpg`、`.png` 等后缀，但实际上丢失了。

---

### 问题排查过程

#### 1. 定位问题代码

在 `PictureUploadTemplate.uploadPicture()` 第 52 行：

```java
String originFilename = getOriginFilename(inputSource);
String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, FileUtil.getSuffix(originFilename));
```

#### 2. 分析 `getOriginFilename()` 实现

查看 `UrlPictureUpload.getOriginFilename()` 方法（原代码）：

```java
@Override
protected String getOriginFilename(Object inputSource) {
    String fileUrl = (String) inputSource;
    // 从 URL 中提取文件名
    return FileUtil.mainName(fileUrl);  // ❌ 问题在这里！
}
```

**`FileUtil.mainName()` 的作用**：去掉文件后缀，只保留主文件名

例如：
- `image.jpg` → `image`
- `https://example.com/photo.png` → `https://example.com/photo`

#### 3. 追踪后缀丢失的完整链路

```
原始 URL: https://example.com/image.jpg?w=200
    ↓
UrlPictureUpload.getOriginFilename()
    → FileUtil.mainName(fileUrl)
    → "https://example.com/image"  (后缀 .jpg 被去掉！)
    ↓
PictureUploadTemplate.uploadPicture()
    → FileUtil.getSuffix("https://example.com/image")
    → "" (空字符串，因为已经没有后缀了)
    ↓
最终文件名: "2026-06-09_uuid." (没有后缀！)
```

---

### 修复方案

修改 `UrlPictureUpload.getOriginFilename()` 方法，**保留文件后缀**：

```java
@Override
protected String getOriginFilename(Object inputSource) {
    String fileUrl = (String) inputSource;
    // 直接返回原始 URL，让模板方法统一处理文件名提取
    // 去掉 URL 参数，保留文件名和后缀
    int questionMarkIndex = fileUrl.indexOf("?");
    if (questionMarkIndex > -1) {
        fileUrl = fileUrl.substring(0, questionMarkIndex);
    }
    return fileUrl;  // 保留完整 URL，包含后缀
}
```

---

### 修复前后对比

| 阶段 | 修复前 | 修复后 |
|------|--------|--------|
| **输入 URL** | `https://example.com/image.jpg?w=200` | `https://example.com/image.jpg?w=200` |
| **getOriginFilename()** | `https://example.com/image` ❌ | `https://example.com/image.jpg` ✅ |
| **FileUtil.getSuffix()** | `""` (空) ❌ | `"jpg"` ✅ |
| **最终文件名** | `2026-06-09_uuid.` ❌ | `2026-06-09_uuid.jpg` ✅ |
| **COS URL** | `.../2026-06-09_uuid.` ❌ | `.../2026-06-09_uuid.jpg` ✅ |

---

### 根本原因总结

| 问题 | 原因 |
|------|------|
| **为什么要用 FileUtil.mainName()？** | 误以为要从 URL 中提取"文件名"，但实际上这个方法会去掉后缀 |
| **为什么之前本地文件上传没问题？** | 本地文件上传的 `getOriginFilename()` 返回 `multipartFile.getOriginalFilename()`，保留后缀 |
| **为什么 URL 上传会出问题？** | URL 上传的 `getOriginFilename()` 错误地使用了 `FileUtil.mainName()`，导致后缀丢失 |

---

### 经验教训

1. **理解工具方法的行为**：`FileUtil.mainName()` 会去掉后缀，不是简单的"提取文件名"
2. **统一处理逻辑**：两种上传方式（本地文件、URL）的 `getOriginFilename()` 应该返回**包含后缀**的完整文件名
3. **模板方法的责任边界**：`FileUtil.getSuffix()` 应该在**有后缀**的字符串上调用，而不是已经被去掉后缀的字符串

---

### 修复后的完整链路

```
原始 URL: https://example.com/image.jpg?w=200
    ↓
UrlPictureUpload.getOriginFilename()
    → 去掉参数: "https://example.com/image.jpg"
    → 返回完整 URL
    ↓
PictureUploadTemplate.uploadPicture()
    → FileUtil.getSuffix("https://example.com/image.jpg")
    → "jpg" ✅
    ↓
最终文件名: "2026-06-09_uuid.jpg" ✅
    ↓
COS URL: https://cc-picture-xxx.cos.ap-shanghai.myqcloud.com/public/2058909011734790145/2026-06-09_uuid.jpg ✅
```

---

**修复时间**：2026-06-09  
**影响范围**：所有通过 URL 上传的图片  
**修复验证**：上传一张图片，检查 COS URL 是否包含正确的文件后缀

---

## Bing 图片 URL 特殊格式导致后缀丢失

### 问题现象

通过 `uploadPictureByBatch` 从 Bing 批量抓取图片时，虽然第一次修复解决了标准 URL 的后缀问题，但 **Bing 图片的特殊 URL 格式**仍然导致后缀丢失：

**调试日志输出**：
```
上传图片调试信息：originFilename = https://thfvnext.bing.com/th/id/OIP.eYG-J5FSGzEScenwq5UmQgHaE2, suffix = eYG-J5FSGzEScenwq5UmQgHaE2
```

**问题分析**：
- `suffix = eYG-J5FSGzEScenwq5UmQgHaE2` 不是标准的文件后缀（jpg、png 等）
- 这是 Bing 图片的 ID，被 `FileUtil.getSuffix()` 误认为是文件后缀
- 最终 COS URL 变成：`.../2026-06-09_uuid.eYG-J5FSGzEScenwq5UmQgHaE2`

---

### Bing 图片 URL 格式分析

#### 标准 URL vs Bing URL

| URL 类型 | 示例 | 文件名格式 | 后缀提取结果 |
|----------|------|------------|-------------|
| **标准 URL** | `https://example.com/image.jpg?w=200` | `image.jpg` | `jpg` ✅ |
| **Bing URL** | `https://thfvnext.bing.com/th/id/OIP.eYG-J5FSGzEScenwq5UmQgHaE2` | `OIP.eYG-J5FSGzEScenwq5UmQgHaE2` | `eYG-J5FSGzEScenwq5UmQgHaE2` ❌ |

**Bing URL 的特点**：
- 文件名包含点（`.`），但没有真正的文件后缀
- 最后一个点后面的是图片 ID，不是文件类型
- URL 中的 `Content-Type` 头才是真实的文件类型

---

### 排查过程

#### 1. 添加调试日志

在 `PictureUploadTemplate.uploadPicture()` 中添加日志：

```java
String originFilename = getOriginFilename(inputSource);
String suffix = FileUtil.getSuffix(originFilename);
log.info("上传图片调试信息：originFilename = {}, suffix = {}", originFilename, suffix);
```

#### 2. 发现异常输出

**预期输出**：
```
originFilename = https://example.com/image.jpg, suffix = jpg
```

**实际输出**：
```
originFilename = https://thfvnext.bing.com/th/id/OIP.eYG-J5FSGzEScenwq5UmQgHaE2, suffix = eYG-J5FSGzEScenwq5UmQgHaE2
```

#### 3. 分析 Bing URL 格式

```
https://thfvnext.bing.com/th/id/OIP.eYG-J5FSGzEScenwq5UmQgHaE2
                                ↓
                          Bing 图片 ID 格式
                                ↓
                    FileUtil.getSuffix() 把 ID 当作后缀
```

---

### 解决方案

由于 Bing URL 无法通过文件名提取真实后缀，我们需要从 **HTTP 响应头的 Content-Type** 中获取文件类型。

#### 核心思路

1. **在 `validPicture()` 中缓存 Content-Type**：发送 HEAD 请求时保存 Content-Type
2. **在 `getOriginFilename()` 中使用 Content-Type**：根据 Content-Type 生成正确的文件名和后缀
3. **添加辅助方法**：Content-Type 转换为文件扩展名

---

### 完整修复代码

#### UrlPictureUpload.java（修复后完整代码）

```java
package com.zjcc.ccpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate {

    /**
     * 缓存当前 URL 对应的 Content-Type
     * key: URL, value: Content-Type (如 "image/jpeg")
     * 
     * 为什么用静态变量？
     * - validPicture() 和 getOriginFilename() 是在同一个请求中调用的
     * - 使用缓存可以在两个方法之间传递 Content-Type 信息
     * - ConcurrentHashMap 保证线程安全
     */
    private static final java.util.Map<String, String> URL_CONTENT_TYPE_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        try {
            // 1. 校验 url 格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL格式不正确");
        }
        // 2. 校验 URL 协议
        ThrowUtils.throwIf(!(fileUrl.startsWith("http://") || fileUrl.startsWith("https://")),
                ErrorCode.PARAMS_ERROR, "仅支持 HTTP 或 HTTPS 协议的文件地址");

        // 3. 发送 HEAD 请求以验证文件是否存在
        HttpResponse response = null;
        try {
            response = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回 无需后续验证
            // 有些 URL 地址可能不支持通过 HEAD 请求访问，为了提高导入成功率，即使 HEAD 请求访问失败，也不会报错
            if (response.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }
            // 4 校验文件类型
            String contentType = response.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 允许的图片类型
                final List<String> ALLOW_CONTENT_TYPES = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                ThrowUtils.throwIf(!ALLOW_CONTENT_TYPES.contains(contentType.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
                // ✅ 关键修复：缓存 Content-Type，后续生成文件名时使用
                URL_CONTENT_TYPE_CACHE.put(fileUrl, contentType);
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    // 限制文件大小为 2MB
                    final long ONE_M = 1024 * 1024L;
                    ThrowUtils.throwIf(contentLength > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式错误");
                }
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 去掉 URL 参数
        int questionMarkIndex = fileUrl.indexOf("?");
        if (questionMarkIndex > -1) {
            fileUrl = fileUrl.substring(0, questionMarkIndex);
        }

        // ✅ 关键修复：从缓存中获取 Content-Type
        String contentType = URL_CONTENT_TYPE_CACHE.get(fileUrl);
        if (StrUtil.isNotBlank(contentType)) {
            // 根据 Content-Type 生成正确的文件名
            String extension = contentTypeToExtension(contentType);
            if (StrUtil.isNotBlank(extension)) {
                // 对于 Bing 等 URL，提取 ID 部分
                String fileName = extractFileNameFromUrl(fileUrl);
                return fileName + "." + extension;
            }
        }

        // 如果没有 Content-Type，返回原始 URL（兼容旧逻辑）
        return fileUrl;
    }

    /**
     * 从 URL 中提取文件名（去掉路径）
     * 例如：https://example.com/path/to/OIP.xyz → OIP.xyz
     */
    private String extractFileNameFromUrl(String url) {
        int lastSlashIndex = url.lastIndexOf("/");
        if (lastSlashIndex > -1 && lastSlashIndex < url.length() - 1) {
            return url.substring(lastSlashIndex + 1);
        }
        return url;
    }

    /**
     * 将 Content-Type 转换为文件扩展名
     * 
     * @param contentType HTTP Content-Type (如 "image/jpeg")
     * @return 文件扩展名 (如 "jpg")
     */
    private String contentTypeToExtension(String contentType) {
        if (contentType == null) {
            return null;
        }
        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            default:
                return null;
        }
    }

    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        // 下载文件到临时目录
        HttpUtil.downloadFile(fileUrl, file);
    }
}
```

---

### 修复后的处理流程

#### 流程图

```
Bing URL: https://thfvnext.bing.com/th/id/OIP.eYG-J5FSGzEScenwq5UmQgHaE2
    ↓
validPicture() 发送 HEAD 请求
    ↓
获取 Content-Type: image/jpeg
    ↓
缓存到 URL_CONTENT_TYPE_CACHE
    ↓
getOriginFilename()
    ↓
提取文件名: OIP.eYG-J5FSGzEScenwq5UmQgHaE2
    ↓
转换 Content-Type: image/jpeg → jpg
    ↓
返回: OIP.eYG-J5FSGzEScenwq5UmQgHaE2.jpg
    ↓
FileUtil.getSuffix(OIP.eYG-J5FSGzEScenwq5UmQgHaE2.jpg) = "jpg" ✅
    ↓
最终文件名: 2026-06-09_uuid.jpg ✅
    ↓
COS URL: https://cc-picture-xxx.cos.ap-shanghai.myqcloud.com/public/.../2026-06-09_uuid.jpg ✅
```

---

### 关键设计点

| 设计点 | 说明 | 原因 |
|--------|------|------|
| **使用 Content-Type** | 从 HTTP 响应头获取真实文件类型 | Bing URL 文件名不包含真实后缀 |
| **静态缓存** | 使用 `URL_CONTENT_TYPE_CACHE` 在方法间传递数据 | `validPicture()` 和 `getOriginFilename()` 需要共享 Content-Type |
| **线程安全** | 使用 `ConcurrentHashMap` | 可能有多线程同时上传图片 |
| **兼容性** | 如果没有 Content-Type，回退到原始逻辑 | 保证对标准 URL 的兼容性 |

---

### 对比总结

#### 修复前后对比

| 场景 | 修复前 | 修复后 |
|------|--------|--------|
| **标准 URL** | ✅ 正常 | ✅ 正常 |
| **Bing URL** | ❌ 后缀丢失 | ✅ 使用 Content-Type 生成后缀 |
| **其他特殊 URL** | ❌ 可能后缀丢失 | ✅ 使用 Content-Type 生成后缀 |

#### 最终效果

**修复前**：
```
COS URL: .../2026-06-09_uuid.eYG-J5FSGzEScenwq5UmQgHaE2 ❌
```

**修复后**：
```
COS URL: .../2026-06-09_uuid.jpg ✅
```

---

### 经验教训

1. **不要假设 URL 格式**：不同来源的 URL 格式可能差异很大
2. **优先使用 HTTP 头信息**：Content-Type 比文件名更可靠
3. **调试日志的重要性**：通过日志快速定位问题根源
4. **缓存的作用**：在方法间传递数据时，静态缓存是简单有效的方案

---

**修复时间**：2026-06-10  
**影响范围**：所有通过 Bing 或类似特殊格式 URL 上传的图片  
**修复验证**：通过 `uploadPictureByBatch` 批量上传 Bing 图片，检查 COS URL 是否包含正确的文件后缀

---

# 2026/06/13

## URL 上传图片 name 出现重复后缀（logo.png.png → name = logo.png）

### 问题现象

用户给 URL 上传添加 webp 支持后，发现通过 URL 上传一张 **URL 本身就带标准后缀** 的图片（如 `https://xxx/logo.png`），存入数据库的图片名称（`picName`）变成了 `logo.png`，**多了一个后缀**（预期应该是 `logo`）。

> 注意：这次 bug 和"后缀丢失"方向相反，是"后缀重复"。加 webp 只是又多了一个触发场景，并不是 bug 的来源。

---

### 问题排查过程

#### 1. name（picName）是怎么生成的？

在 `PictureUploadTemplate` 中，`originFilename` 这个字符串被两个地方同时使用：

```java
// 模板方法 uploadPicture() 中
String originFilename = getOriginFilename(inputSource);   // 由子类 UrlPictureUpload 实现

// 用途1：取后缀，拼 COS 文件名
String suffix = FileUtil.getSuffix(originFilename);

// 用途2：取主名，存数据库 picName（在 buildResult 中）
uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
```

**关键认知**：同一个 `originFilename` 字符串，既要被 `getSuffix()` 取后缀（给 COS 文件名用），又要被 `mainName()` 取主名（给数据库 name 用）。如果 `originFilename` 自身后缀重复，两个用途都会受影响，只是表现不同。

#### 2. 走一遍 URL = `https://xxx/logo.png` 的流程

在 `UrlPictureUpload.getOriginFilename()` 中（这正是 06-10 为修 Bing 而写的逻辑）：

```java
// 去掉 URL 参数后 fileUrl = https://xxx/logo.png
String contentType = URL_CONTENT_TYPE_CACHE.get(fileUrl);  // = "image/png"
String extension   = contentTypeToExtension("image/png");  // = "png"
String fileName    = extractFileNameFromUrl(fileUrl);      // = "logo.png"  ← URL 自带 .png，没去掉！
return fileName + "." + extension;                          // = "logo.png.png"  ❌
```

所以 `originFilename = "logo.png.png"`。

#### 3. 模板方法拿这个值继续处理

| 用途 | 代码 | 输入 `logo.png.png` | 结果 |
|------|------|---------------------|------|
| 取后缀拼 COS 文件名 | `FileUtil.getSuffix()` | `logo.png.png` | `png` ✅（恰好正确，不影响 COS） |
| 存数据库 name | `FileUtil.mainName()` | `logo.png.png` | `logo.png` ❌ |

`FileUtil.mainName()` 只会去掉**最后一个点**之后的部分。`logo.png.png` → `logo.png`，于是数据库 name 留下了一个多余的 `.png`。

> 这也解释了"为什么 COS 文件名看起来是对的、但数据库 name 却错了"——两个消费方对后缀重复的容错程度不同。

---

### 根本原因

```java
String fileName = extractFileNameFromUrl(fileUrl);  // 只取 URL 最后一段，不去后缀
return fileName + "." + extension;                   // 无条件再拼一个后缀 → 重复
```

`extractFileNameFromUrl` 的职责是"取 URL 最后一段"，它**不会去掉原有的后缀**。于是：

- **Bing 的 `OIP.eYG-J5FSGzEScenwq5UmQgHaE2`**：没有标准后缀，拼成 `OIP.eYG...E2.jpg`，`mainName` 取到 `OIP.eYG...E2`，没问题 → 这是 06-10 想解决的场景。
- **标准的 `logo.png`**：URL 本身就有 `.png`，再拼一个变成 `logo.png.png`，`mainName` 留下 `logo.png` → 这是本次 bug。

**结论：本轮 bug 正是上一轮"为修 Bing 后缀丢失而引入的拼接逻辑"造成的副作用。** 凡是 URL 本身带 `jpg/png/webp` 标准后缀的图片都会中招，加 webp 只是让 webp 这一类也被发现了而已。

---

### 修复方案

思路：URL 提取出的文件名先用 `FileUtil.mainName()` 去掉原有后缀，再用 Content-Type 推导的后缀拼一次，从源头杜绝重复。顺带让 Bing 的 `OIP.xyz` 也变得更干净（`OIP.xyz.png` → `OIP.png`）。

修改 `UrlPictureUpload.getOriginFilename()`：

```java
@Override
protected String getOriginFilename(Object inputSource) {
    String fileUrl = (String) inputSource;
    // 去掉 URL 参数
    int questionMarkIndex = fileUrl.indexOf("?");
    if (questionMarkIndex > -1) {
        fileUrl = fileUrl.substring(0, questionMarkIndex);
    }

    // 从缓存中获取 Content-Type
    String contentType = URL_CONTENT_TYPE_CACHE.get(fileUrl);
    if (StrUtil.isNotBlank(contentType)) {
        // 根据 Content-Type 推导正确的后缀
        String extension = contentTypeToExtension(contentType);
        // 提取 URL 最后一段，并去掉原有后缀，避免重复拼接（如 logo.png.png）
        String fileName = extractFileNameFromUrl(fileUrl);
        String mainName = FileUtil.mainName(fileName);
        if (StrUtil.isNotBlank(extension)) {
            return mainName + "." + extension;
        }
        return mainName;
    }

    // 如果没有 Content-Type，返回原始 URL（兼容旧逻辑）
    return fileUrl;
}
```

核心改动只有一处：把原来的 `return fileName + "." + extension;` 换成先 `mainName`（去后缀）再拼。`FileUtil` 已在文件顶部 import，无需新增依赖。`extractFileNameFromUrl` 的职责也变得更单一（只负责"取文件名"），去后缀交给 `mainName`。

---

### 修复前后对比

| URL 最后一段 | Content-Type | 修复前 originFilename | 修复前 name | 修复后 originFilename | 修复后 name |
|------------|-------------|----------------------|------------|----------------------|------------|
| `logo.png` | image/png | `logo.png.png` ❌ | `logo.png` ❌ | `logo.png` ✅ | `logo` ✅ |
| `logo.webp` | image/webp | `logo.webp.webp` ❌ | `logo.webp` ❌ | `logo.webp` ✅ | `logo` ✅ |
| `OIP.xyz` | image/png | `OIP.xyz.png` | `OIP.xyz` | `OIP.png` ✅（更干净） | `OIP` ✅ |
| `logo`（无后缀） | image/png | `logo.png` | `logo` | `logo.png` ✅ | `logo` ✅ |

---

### 三次后缀修复的演进关系（打地鼠）

| 时间 | 问题 | 修复手段 | 引入的副作用 |
|------|------|---------|------------|
| 2026-06-09 | URL 上传 COS 后缀丢失（误用 `mainName` 砍掉了后缀） | 改成返回完整 URL | — |
| 2026-06-10 | Bing URL `OIP.xxx` 把 ID 当后缀 | 引入 Content-Type 缓存 + `extractFileNameFromUrl` + 拼后缀 | 标准后缀 URL 会重复拼接 ← **本轮 bug 的根源** |
| 2026-06-13 | `logo.png` 变 `logo.png.png`，name 多后缀 | 先 `mainName` 去后缀，再按 Content-Type 拼一次 | （目前无） |

后缀处理连续修了三次，本质是"修一个场景、又漏一个场景"。根本原因是 **`originFilename` 这一个字符串同时承担了"取后缀"和"取主名"两个职责**，对它的格式假设必须自洽——既要带后缀（给 `getSuffix`），又不能有重复后缀（给 `mainName`）。

---

### 经验教训

1. **一个字符串两个职责的陷阱**：`originFilename` 同时用于 `getSuffix`（取后缀）和 `mainName`（取主名）。修改这个字符串的生成逻辑时，必须同时考虑两个消费方，不能只看其中一边。
2. **工具方法的边界要清楚**：`FileUtil.mainName()` 只去掉"最后一个点"之后的部分，对 `a.b.c` 只去 `.c`，留下 `a.b`。不能假设它能"去掉所有后缀"或"只保留真正的文件名"。
3. **修复必须回归所有场景**：06-10 为 Bing 加的拼接逻辑只验证了 Bing 场景，没回归"URL 本身带标准后缀"的场景，于是埋下了这次的回归 bug。
4. **URL 上传后缀处理的回归清单**：以后改动这块，至少覆盖四类 URL —— 带标准后缀（jpg/png/webp）、Bing 无标准后缀（`OIP.xxx`）、带查询参数（`logo.png?v=1`）、纯无后缀（`logo`）。

---

**修复时间**：2026-06-13  
**影响范围**：所有 URL 本身带标准图片后缀（jpg/png/webp）的上传请求  
**修复验证**：用 `https://xxx/logo.png`、`logo.webp`、Bing `OIP.xxx` 三类 URL 分别上传，确认 name 无重复后缀、COS 文件后缀正确

---

# 2026/06/18

## @AllArgsConstructor 注解详解（Lombok 构造器注入）

### 作用

`@AllArgsConstructor` 是 **Lombok** 的注解，在**编译期**自动生成一个"包含类中所有字段"的构造方法，省去手写。

以本项目 `SpaceController` 为例：

```java
@Slf4j
@RestController
@RequestMapping("/space")
@AllArgsConstructor
public class SpaceController {
    private final SpaceService spaceService;
    private final SpaceDAO spaceDAO;
}
```

`@AllArgsConstructor` 等价于手写了下面这个构造方法：

```java
public SpaceController(SpaceService spaceService, SpaceDAO spaceDAO) {
    this.spaceService = spaceService;
    this.spaceDAO = spaceDAO;
}
```

---

### 在 Spring 中的真正用途：构造器注入

这是它在 Controller / Service 上最常见的用法。Spring 有一条规则：

> 一个类如果**只有一个构造方法**，Spring 会自动用它来注入依赖，连 `@Autowired` 都不用写。

所以容器启动时，会找到 `SpaceService`、`SpaceDAO` 这两个 Bean，通过自动生成的构造方法注入进去。

#### 对比字段注入

| 方式 | 写法 | 优缺点 |
|------|------|--------|
| 字段注入 | `@Autowired private SpaceService spaceService;` | 字段不能 final、隐藏依赖、单测要靠反射塞 mock |
| 构造器注入（本例） | `private final SpaceService spaceService;` + `@AllArgsConstructor` | 字段可 final（不可变、线程安全）、依赖一目了然、单测直接 `new SpaceController(mockService, mockDao)` |

---

### 三个相关注解对比

| 注解 | 生成的构造方法 | 适用场景 |
|------|--------------|---------|
| `@NoArgsConstructor` | 无参构造 | 需要无参构造（如 JPA 实体、序列化框架） |
| `@AllArgsConstructor` | **所有字段**都作为参数 | 字段较少且全是依赖时可用 |
| `@RequiredArgsConstructor` | 只有 `final` 字段 / `@NonNull` 字段作为参数 | **Spring 依赖注入首选** |

---

### 为什么推荐换成 @RequiredArgsConstructor

`SpaceController` 里两个字段都是 `final`，此时 `@AllArgsConstructor` 和 `@RequiredArgsConstructor` **效果完全一样**。但区别在于：

- 万一以后加了一个**非 final 的普通字段**（比如某个配置值），`@AllArgsConstructor` 会把它也塞进构造方法，让 Spring 去找对应的 Bean → 找不到就启动报错；
- `@RequiredArgsConstructor` 只收 `final` 字段，普通字段不进构造方法，不会出问题。

所以做依赖注入时，**`@RequiredArgsConstructor` 是更稳妥的标配写法**。

| 场景 | @AllArgsConstructor | @RequiredArgsConstructor |
|------|---------------------|--------------------------|
| 全是 final 字段 | ✅ 正常 | ✅ 正常 |
| 混入非 final 字段 | ❌ 会把普通字段也作为构造参数 | ✅ 只注入 final 字段 |
| 依赖注入推荐度 | 一般 | ⭐ 首选 |

---

### 小结

- `@AllArgsConstructor` = 自动生成"全字段构造方法"，配合 Spring 实现**构造器注入**，省去 `@Autowired`。
- 依赖注入场景下，更推荐用 `@RequiredArgsConstructor`，避免未来加非 final 字段时把 Bean 装配搞坏。
- 构造器注入优于字段注入：字段可 `final`、依赖清晰、便于单测。

---

## 并发按用户加锁：从 `.intern()` 到 `ConcurrentHashMap`

### 场景：同一用户只能创建一个私有空间

`SpaceServiceImpl.addSpace` 用"加锁 + 事务"保证同一用户不会重复创建私有空间，核心片段：

```java
// 针对用户进行加锁：同一用户串行，不同用户并行
String lock = String.valueOf(userId).intern();
synchronized (lock) {
    Long newSpaceId = transactionTemplate.execute(status -> {
        boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有1个私有空间");
        boolean result = this.save(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return space.getId();
    });
    return Optional.ofNullable(newSpaceId).orElse(-1L);
}
```

目标：**同一个用户串行**（同时只有一个线程能创建空间），**不同用户并行**（互不阻塞）。

---

### 1. 为什么用 `.intern()`

`String.valueOf(userId)` 每次调用都 new 一个**全新**的 String 对象（在堆里，不在常量池）。而 `synchronized` 锁的是**对象身份（内存地址）**，不是字符串内容。于是：

```
线程A：userId=123 → String.valueOf(123) → 对象 @0xA1
线程B：userId=123 → String.valueOf(123) → 对象 @0xB2  ← 另一个对象！
两个 synchronized 锁不同对象 → 互斥失效 ❌（锁形同虚设）
```

`.intern()` 返回字符串在**常量池里的规范实例**，相同内容永远返回**同一个**对象：

```
线程A："123".intern() → 常量池 "123" @0xP
线程B："123".intern() → 同一个 @0xP
锁同一个对象 → 互斥生效 ✅
```

**结论：`.intern()` 就是让"同一个 userId → 同一把锁"成立的关键。**

> ⚠️ 这是 **JVM 进程级锁**，只在单个实例内有效。集群部署（多实例）就失效了，需要换成 Redis Redisson 分布式锁（按 `lock:userId` 加锁），和定时任务集群需要分布式锁是同一个道理。

---

### 2. （常量池全局共享）为什么用 intern 字符串当锁"不优雅"

`.intern()` 能用，但属于 code smell。原因在于你拿了一个**谁都能碰、又收不回来**的全局对象，去充当一把本该归你独占的锁：

**① 锁不归你管 → 可能和不相干代码抢同一把锁**
intern 出来的字符串全 JVM 只有一份。任何代码（自己的、第三方 jar、框架）只要对**同样的字符串**加锁，就和你抢同一个 monitor。你无法保证没有别人用 `"123"` 当锁，一旦撞上，两个不相干的模块会莫名其妙互相阻塞——这种 bug 极难排查，因为代码里完全看不出关联。

**② intern 进去的字符串收不回来 → 池污染 / 内存隐患**
字符串常量池是全局共享结构：

| JDK 版本 | 常量池位置 | intern 字符串能否回收 |
|---------|----------|-----------------|
| JDK 6 及以前 | PermGen | 几乎不回收，大量 intern 会 **PermGen OOM**（intern() 被告诫慎用的历史根源） |
| JDK 7+ | 普通 Java 堆 | 理论上可 GC，但它是全局共享结构、回收时机远没有普通对象干脆 |

后果：系统有 10 万用户，就有 10 万个形如 `"100001"` 的字符串被钉在池里。而用自己 Map 的话，**用完可以 `remove` 释放**。

**③ 把"数据"当"锁"，语义不清**
锁是基础设施，userId 是业务数据。用业务值当锁等于把两件事绑死：哪天锁的维度变了（比如按 `userId + 空间类型` 加锁），就得做字符串拼接 `"userId_" + uid + "_" + type`，越拼越脏；而一个专用锁对象一眼就知道"这是用来锁的"。

**小结**：一把好锁应该是**你独占、可控、可回收、自解释**的对象；intern 字符串这四条一条都不满足。小规模单机可以用、教程里也常见，但它是个 smell；真正会咬人的是**集群后 JVM 锁直接失效**。

---

### 3. 更优雅的写法：`ConcurrentHashMap` 登记锁对象

```java
private final ConcurrentHashMap<Long, Object> userLocks = new ConcurrentHashMap<>();

Object lock = userLocks.computeIfAbsent(userId, k -> new Object());
synchronized (lock) {
    // 业务逻辑
}
```

#### 为什么用 `ConcurrentHashMap`（而不是 HashMap / synchronizedMap）

**① 这张表是多线程共享的，普通 HashMap 会被写坏**
每个线程进来都要 `get/computeIfAbsent` 自己 userId 对应的锁对象。多线程并发写普通 `HashMap` 会破坏内部结构（Java 7 会成环导致 CPU 100%，Java 8+ 丢数据），所以表本身**必须线程安全**。

**② ★ 核心：`computeIfAbsent` 是原子的**
要害是"同一个 userId 永远拿到**同一个**锁对象"。用普通 map 的 check-then-act 写法有竞态：

```java
// ❌ 竞态：同 userId 可能造出两把不同的锁
Object lock = userLocks.get(userId);
if (lock == null) {
    lock = new Object();
    userLocks.put(userId, lock);
}
```

两个线程同时处理同一个 userId=123：

```
线程A：get(123) == null
线程B：get(123) == null        ← 两人都看到没有
线程A：new Object() → 对象@X，put(123, @X)
线程B：new Object() → 对象@Y，put(123, @Y)   ← 同一个 userId 两把不同的锁 ❌
```

这跟"不用 intern 的字符串"是**同一个病**。

`ConcurrentHashMap.computeIfAbsent(key, k -> new Object())` 是**原子操作**：同一个 key 只有一个线程执行 lambda 创建对象，其它线程直接拿到已存在的那个。一句话就拿到了"同 key → 同对象，必然"的保证——**这才是用 CHM 的根本原因，不是为了并发读快，而是为了原子语义**。

**③ 桶级锁，不同 userId 查找互不阻塞**
`Hashtable` / `Collections.synchronizedMap` 每次操作**锁整张表**，会把不同用户的查找也串起来，违背"不同用户并行"的目标。`ConcurrentHashMap` 按**桶（bucket）加锁**，不同 key 落在不同桶，查找可并行。

#### 分工要清楚：真正做互斥的是 value 对象，不是 CHM

| 角色 | 干什么 |
|------|--------|
| `ConcurrentHashMap` | 锁对象的**线程安全注册表**，负责安全地 get-or-create 锁对象 |
| `synchronized(lock)` | 真正的**按用户互斥**靠 value 那个 `Object` |

而且**不能在持有 CHM 内部桶锁的时候去做耗时业务**（DB 事务那一大坨）。标准姿势是先用 `computeIfAbsent` 把锁对象"取出来"，再在**表外** `synchronized(lock)`。

#### ⚠️ 延伸坑：清理锁对象会重新引入竞态

想"用完 `remove(userId)` 释放内存"？有竞态：

```
线程A：拿到锁对象@X，准备进 synchronized
线程C：remove(123) 把@X删了
线程D：computeIfAbsent(123) → 新建对象@Z
→ 线程A 锁@X、线程D 锁@Z，同一个用户又出现两把锁 ❌
```

所以实际工程里要么**干脆不 remove**（接受 map 慢慢变大，或定期清空），要么直接用 **Guava `Striped<Lock>`**——固定数量的一组锁按 key hash 分配，既不创建无限对象、也没有清理竞态，本来就是为这种场景造的。

---

### 4. 三种"按用户加锁"写法对比

| 写法 | "同 userId → 同锁" 怎么保证 | 优点 | 缺点 |
|------|------------------------|------|------|
| `String.valueOf(id).intern()` + `synchronized` | 常量池规范实例 | 简单、无额外字段 | 池污染、锁不归你管、集群失效 |
| `ConcurrentHashMap<id, Object>` + `synchronized` | `computeIfAbsent` 原子 | 锁私有可控、桶级并发 | 清理有竞态、map 可能变大 |
| Guava `Striped<Lock>` | 固定锁池按 hash 分配 | 无限对象、无清理竞态、内存可控 | 极小概率不同 key 撞同一把锁（可接受） |

---

### 经验教训

1. `synchronized` 锁的是**对象身份**不是内容；要让"同值 → 同锁"成立，要么用 intern（能跑但不优雅），要么用专用注册表。
2. "同 key → 同对象"在并发下天然有竞态，必须靠**原子操作**（`computeIfAbsent`）来保证——这正是 `ConcurrentHashMap` 在锁注册表场景的核心价值，不是为了读得快。
3. JVM 级锁（`synchronized` / intern / CHM）都只在**单实例**有效；集群部署必须上分布式锁。
4. 用业务值（字符串）当锁是把数据和并发机制耦合，优先用专用的、私有的锁对象。

---

## MyBatis-Plus 主键回写：为什么 `save` 后 `space.getId()` 能拿到新 id

```java
boolean result = this.save(space);
return space.getId();   // 没有再查一次数据库，直接从对象里取
```

关键在 `Space` 实体上的注解：

```java
@TableId(type = IdType.ASSIGN_ID)
private Long id;
```

`IdType.ASSIGN_ID` = MyBatis-Plus 用**雪花算法**生成全局唯一的 Long 主键。`this.save(space)`（来自 `ServiceImpl`）执行 INSERT 时：

```
1. MP 发现 id 为 null → 雪花算法生成一个 id
2. 把这个 id 写回到 space 对象本身的 id 字段（通过反射，keyProperty=id）
3. 执行 INSERT
```

所以 `save()` 一返回，**内存里同一个 `space` 对象的 `id` 已经被填上了**，`getId()` 直接读到——这就是"**主键回写**"，不用再 `SELECT` 一次。

这个机制对所有主键策略都成立：

| IdType | id 怎么来 | 回写时机 |
|--------|----------|---------|
| `ASSIGN_ID` | 雪花算法（**本项目用这个**） | insert **前**生成并回写 |
| `AUTO` | 数据库自增 | insert **后**用 `LAST_INSERT_ID()` 取回回写 |
| `ASSIGN_UUID` | UUID | insert 前生成并回写 |

> 补充：`ASSIGN_ID` 只在 id 为 null 时才生成；如果实体上已经手动 `setId(...)` 了，MP 会直接用你给的值、不再生成雪花 id。

---

## 编程式事务 TransactionTemplate：谁负责回滚

承接上面的"按用户加锁"。那段代码在 `synchronized` 锁块里又套了一层**编程式事务** `transactionTemplate.execute(...)`：

```java
synchronized (lock) {
    Long newSpaceId = transactionTemplate.execute(status -> {
        boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).exists();
        ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户只能有1个私有空间");  // ① 抛异常 → 回滚
        boolean result = this.save(space);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);                            // ② 抛异常 → 回滚
        return space.getId();                                                              // ③ 正常返回 → 提交
    });
    return Optional.ofNullable(newSpaceId).orElse(-1L);
}
```

### 直接结论：这段代码没有"显式回滚"语句，回滚靠抛异常自动触发

`transactionTemplate.execute(callback)` 的回滚规则：

| 回调里发生什么 | 结果 |
|----------|------|
| 正常 `return`（没异常） | **提交 commit** |
| 抛出**任何异常**（RuntimeException 或 checked Exception 都算） | **自动回滚 rollback** |

所以这段代码的"回滚负责人" = **回调里抛出的异常**，有三个触发点：

1. 第 ① 行：用户已有私有空间 → `throwIf` 抛 `BusinessException` → 回滚（这里只有查询没写入，实际没什么数据可退，但事务确实标记回滚并把异常继续往上抛）。
2. 第 ② 行：`save()` 返回 false → 抛 `BusinessException` → 回滚。
3. `this.save(space)` 本身抛异常（主键冲突、字段约束等）→ 回滚。

而第 ③ 行正常 `return` → 提交。

### 两种"自己控制回滚"的方式

| 方式 | 写法 | 本代码用了吗 |
|------|------|----------|
| （a）抛异常 → 自动回滚 | `throw new BusinessException(...)` 或 `ThrowUtils.throwIf(...)` | ✅ 用的就是这种 |
| （b）手动标记回滚 + 正常返回 | `status.setRollbackOnly(); return null;` | ❌ 没用 |

```java
// （b）手动控制的写法（本代码没这么写）
transactionTemplate.execute(status -> {
    if (某条件) {
        status.setRollbackOnly();   // 标记回滚，不抛异常
        return null;
    }
    // ... 业务 ...
});
```

本代码走的是（a），所以你**看不到任何 `rollback()` 调用**——**异常就是回滚开关**。

### ⚠️ 坑：和 `@Transactional` 对 checked 异常的处理不同

| 事务方式 | 抛 RuntimeException | 抛 checked Exception |
|---------|--------------------|---------------------|
| `@Transactional`（默认） | 回滚 | **不回滚**（默认提交！）需配 `rollbackFor` 才回滚 |
| `TransactionTemplate.execute()` | 回滚 | **也回滚**（checked 异常被包成 `UndeclaredThrowableException` 再抛出） |

所以这段代码里 `throwIf` 抛的 `BusinessException`（继承自 `RuntimeException`），无论用哪种事务都会触发回滚；但如果某天回调里抛了个 checked 异常，`@Transactional` 默认不会回滚，而 `TransactionTemplate.execute()` 会——这点要心里有数。

### 为什么这里要"锁 + 事务"一起用

把上面两节连起来看：

| 机制 | 作用层级 | 解决什么 |
|------|--------|---------|
| `synchronized(lock)` | JVM 进程级 | 防止**同一用户的两个线程**同时进这段逻辑（防"检查-插入"竞态） |
| `transactionTemplate.execute()` | 数据库级 | 保证"检查 exists + 插入"是**原子**的，失败整体回滚 |

关键认知：**光靠事务防不住同一用户的并发插入**。默认隔离级别（READ COMMITTED）下，两个并发事务可能都查到"不存在"、然后都去插入——这正是外面那把 `synchronized` 要拦的。而事务负责的是"要么 check+insert 全成功、要么全回滚"的原子性（当 insert 之后还有其它写操作时，事务的价值更明显）。

> 仍然要记住：`synchronized` 是 JVM 级锁，**集群部署就失效**，届时得把锁换成 Redis 分布式锁，事务那层不变。

### 经验教训

1. `TransactionTemplate` 的回滚默认**靠回调抛异常触发**，不需要（也不应该）手写 rollback。
2. 想不抛异常也回滚，用 `status.setRollbackOnly()`；正常 `return` 就是提交。
3. `TransactionTemplate.execute()` 对**所有异常**都回滚，比 `@Transactional`（默认只回滚 unchecked）更"激进"，checked 异常行为两者不同，迁移时要当心。
4. "加锁 + 事务"组合拳：**锁防并发竞态，事务保原子性**，两者职责不同、不能互相替代；集群里锁要升级为分布式锁。

---

# 2026/06/21

## 本类调用 @Async 失效：clearPictureFile 异步清理变成了同步

### 问题现象

`PictureServiceImpl.deletePicture()` 里删除图片后，注释写着"异步清理文件"，实际调用却写成了本类内部调用：

```java
@Override
public void deletePicture(long pictureId, User loginUser) {
    // ... 校验、删库记录 ...
    // 异步清理文件
    this.clearPictureFile(oldPicture);   // ← 本类内部用 this. 调用
}

@Async
@Override
public void clearPictureFile(Picture oldPicture) {
    // ... 查是否被多条记录引用，引用数<=1 才删 COS 文件 ...
}
```

`clearPictureFile` 明明标了 `@Async`，但因为是**本类内部 `this.` 调用**，异步**根本没生效**——实际是**同步阻塞**执行的。

---

### 为什么失效：Spring AOP 自调用（self-invocation）问题

`@Async`、`@Transactional`、`@Cacheable` 这类注解，都是靠 **Spring AOP 代理**实现的。Spring 注入到 Controller 的不是你写的原始 `PictureServiceImpl`，而是一个**代理对象**（外面包了一层增强逻辑）：

```
Controller → [代理对象: 拦截 @Async,把方法提交到线程池] → 原始 PictureServiceImpl
```

两种调用路径，结果完全不同：

| 调用方式 | 走不走代理 | @Async 生效吗 |
|---------|----------|-------------|
| 外部调用（Controller → `pictureService.clearPictureFile()`） | ✅ 走代理 | ✅ 异步 |
| 本类调用（`deletePicture` 里 `this.clearPictureFile()`） | ❌ `this` 是原始对象，跳过代理 | ❌ 同步 |

**关键认知：`this` 指向的是原始对象，不是代理对象。** 本类内部 `this.xxx()` 调用直接绕过了代理，`@Async` 的增强逻辑根本没机会执行，方法在**当前线程**里老老实实同步跑。

> 这个坑不止 `@Async`——`@Transactional`、`@Cacheable`、`@Retryable` 遇到本类调用全都失效，原理一模一样：注解生效靠代理，`this.` 走不到代理。

---

### 实际影响

- **功能上没坏**：文件照样会被删（COS 的 `deleteObject` 照常执行，只是不在线程池里跑）。
- **但"异步"没了**：清理在**当前请求线程**里同步执行，用户的删除请求会一直阻塞，直到 COS 删除文件的网络 IO 跑完才返回。

也就是说，注释写的"异步清理文件"，实际行为是"同步阻塞清理"，名不副实。

---

### 四种修复方案

#### 方案①：自注入（改动最小，推荐）

把自己当 Bean 注入进来，通过代理引用调用：

```java
@Resource
@Lazy   // 必须加，否则启动期循环依赖
private PictureService pictureService;   // 注入自身代理

// deletePicture 内改为：
pictureService.clearPictureFile(oldPicture);   // 走代理,@Async 生效
```

#### 方案②：拆到独立 Bean（最干净）

把 `clearPictureFile` 挪到一个新的 `@Component`（如 `PictureFileCleaner`），`PictureServiceImpl` 注入它来调用。职责分离，彻底回避自调用。

#### 方案③：AopContext 手动拿代理

```java
((PictureService) AopContext.currentProxy()).clearPictureFile(oldPicture);
```

**前置条件**：必须在启动类加 `@EnableAspectJAutoProxy(exposeProxy = true)`。

#### 方案④：干脆不用 @Async，手动丢线程池

```java
CompletableFuture.runAsync(() -> this.clearPictureFile(oldPicture), asyncExecutor);
```

不依赖代理，本类调用也能异步，最稳。

---

### 和 `@EnableAspectJAutoProxy(exposeProxy = true)` 的关系

本项目启动类上已经写了：

```java
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableAsync
public class CcPictureBackendApplication { ... }
```

这说明**方案③的前置条件早就满足了**，可以直接用 `AopContext.currentProxy()` 来修。

#### `@EnableAspectJAutoProxy` 是什么

开启 Spring 基于 AspectJ 注解的**自动代理**总开关。它在容器启动时注册一个 `AnnotationAwareAspectJAutoProxyCreator`（本质是 `BeanPostProcessor`），干一件事：

> 扫描所有 Bean，凡是带 AOP 注解的（`@Async`、`@Transactional`、`@Cacheable`、自定义 `@Aspect` 等），就把它**包装成代理对象**注入到别处。代理拦截方法调用，真正把"异步""事务"增强逻辑织进去。

没有代理，`@Async` / `@Transactional` 形同虚设。它是注解式 AOP 能跑起来的总开关。

> 补充：Spring Boot 只要引了 `spring-boot-starter-aop`，基础自动代理默认就开了，所以**不带参数的 `@EnableAspectJAutoProxy` 在 Boot 项目里其实是冗余的**。这里特意写它，意义就在于传了 `exposeProxy = true`（这个不是默认值，必须显式开）。

#### `exposeProxy = true` 才是重点

默认代理对象是"藏起来"的——目标类内部不知道自己的代理是谁（这正是 `this.clearPictureFile()` 拿不到代理的原因）。加了 `exposeProxy = true` 后，Spring 会把**当前正在执行的代理对象**塞进一个 `ThreadLocal`（`AopContext`），于是你在目标方法里就能手动把它"捞"出来：

```java
((PictureService) AopContext.currentProxy()).clearPictureFile(oldPicture);
```

这就是方案③的写法。

#### 另一个参数 `proxyTargetClass`（顺带）

| 取值 | 代理方式 | 要求 |
|------|---------|------|
| `false`（默认） | 优先 JDK 动态代理 | 基于**接口**，目标类必须实现接口 |
| `true` | 强制 CGLIB | 基于**子类继承**，不要求有接口 |

Spring Boot 2.x+ 默认就是 CGLIB（`spring.aop.proxy-target-class=true`），所以一般不用管。

---

### 总结

| 问题 | 答案 |
|------|------|
| 本类调用 `@Async` 方法，异步生效吗？ | ❌ 不生效，`this.` 绕过代理，变成同步 |
| 文件还会被清理吗？ | ✅ 会，只是同步阻塞，不是异步 |
| 为什么？ | `@Async` 靠 AOP 代理实现，`this` 是原始对象不走代理 |
| 怎么修？ | 自注入 / 拆独立 Bean / `AopContext.currentProxy()` / 手动线程池 |
| `@EnableAspectJAutoProxy` 干嘛的？ | 注解式 AOP 自动代理的总开关 |
| `exposeProxy = true` 干嘛的？ | 把代理对象暴露到 `AopContext`，让方案③能拿到代理 |

### 经验教训

1. **`@Async`、`@Transactional` 等 AOP 注解只在"跨 Bean 调用"时生效**；本类内 `this.xxx()` 直接绕过代理，注解全部失效。
2. 遇到自调用要异步/事务时，要么**自注入代理**（`@Lazy`），要么**拆到独立 Bean**，要么用 **`AopContext.currentProxy()`**（需 `exposeProxy = true`）。
3. 排查"注解不生效"类问题，先问一句：**这个调用走的是代理还是原始对象？**——这是 Spring AOP 类问题的万能切入点。
4. 代码注释（如"异步清理"）和实际行为不一致时，要警惕：注释可能是开发者的**意图**而非**事实**，以运行时行为为准。

---

## @GetMapping 参数绑定：不加 @RequestBody 默认怎么传参

以 `SpaceController.java` 为例：

```java
@GetMapping("/get")
@AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
public BaseResponse<Space> getSpaceById(Long id, HttpServletRequest request) {
    ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
    Space space = spaceService.getById(id);
    ...
}
```

这里 `Long id` **没加任何注解** → 默认从 **URL 查询字符串（query string）** 取值，实际请求长这样：

```
GET /space/get?id=123
```

Spring 把查询参数 `id` 的值绑定到方法参数 `id` 上（**参数名必须和 query 的 key 一致**）。

---

### 为什么不是 @RequestBody

| 注解                | 数据从哪来                      | 一般配什么请求                     |
|-------------------|----------------------------|-----------------------------|
| `@RequestBody`    | **请求体（body）**，按 JSON 解析    | POST / PUT / DELETE（带 body） |
| `@RequestParam`   | query string 或 form 表单     | GET / POST                  |
| `@PathVariable`   | URL **路径**片段 `/get/{id}`   | 任意                          |
| `@ModelAttribute` | 把多个字段拼成一个对象（来自 query/form） | 任意                          |
| **不加注解**          | 看类型（见下）                    | 任意                          |

`@RequestBody` 是用来读 **HTTP 请求体**里的 JSON 的。而 **GET 请求规范上不带 body**，所以 `@GetMapping` 的参数基本不用
`@RequestBody`（强行用技术上能跑，但很怪、不推荐）。

---

### "不加注解"时，Spring 怎么决定从哪取值

关键看参数**类型**：

**① 简单类型**（`Long`、`String`、`Integer`、`int`、`Date`…）
→ 等价于加了 `@RequestParam`，从 **query string**（GET）或 form 数据取值，参数名要匹配：

```java
// 这两行效果一样
public BaseResponse<Space> getSpaceById(Long id, ...) { }
public BaseResponse<Space> getSpaceById(@RequestParam Long id, ...) { }
```

> ⚠️ 这种情况下**默认 required=true**。如果请求里没带 `id`，Spring 会报缺失参数。想可选要写
`@RequestParam(required = false) Long id`。

**② 复杂类型（POJO / DTO）**
→ 等价于加了 `@ModelAttribute`，Spring 把 query/form 里**字段名匹配**的值逐个塞进对象：

```java
// 假设 SpaceQueryRequest 有 name、level 字段
public BaseResponse<...>

list(SpaceQueryRequest req) {
}
// 请求：GET /space/list?name=我的空间&level=1
// req.getName() = "我的空间", req.getLevel() = 1
```

**③ Spring 内置特殊类型**（`HttpServletRequest`、`HttpServletResponse`、`Principal`、`HttpSession`、`MultipartFile`…）
→ Spring **自动注入**框架对象，不需要你传，也不需要注解。所以 `getSpaceById` 里的 `HttpServletRequest request` 就是这么进来的。

---

### 套到具体代码

`getSpaceById(Long id, HttpServletRequest request)`：

| 参数                           | 类型   | 绑定来源                                       |
|------------------------------|------|--------------------------------------------|
| `Long id`                    | 简单类型 | query string `?id=123`（等价 `@RequestParam`） |
| `HttpServletRequest request` | 内置类型 | Spring 自动注入                                |

所以前端调这个接口就是：`GET /space/get?id=123`，后端 `id` 就能拿到 `123`。

---

### 经验教训

1. **GET 不用 `@RequestBody`**：GET 不带 body；`@RequestBody` 是 POST/PUT 那类有 JSON body 的请求才用的。
2. **简单参数不加注解 = 隐式 `@RequestParam`**：从 query string 取，名字要对上，默认必填。
3. **对象不加注解 = 隐式 `@ModelAttribute`**：按字段名从 query/form 拼对象。
4. 真正想明确语义时，**建议老老实实把 `@RequestParam` / `@PathVariable` 写出来**——不写也能跑，但显式注解可读性更好、行为更可控（比如
   `required = false`）。

---

## Spring Boot Profile 配置：多 yml 文件加载与激活机制

### 为什么要拆成多个 yml 文件

项目里有两个配置文件：

- `application.yml` —— 公共配置（端口、数据库、redis、mybatis-plus 等），所有环境通用
- `application-local.yml` —— 环境相关 / 敏感配置（COS 密钥），只在"本地环境"生效

拆分目的：把**每个环境都一样**的配置放主文件，把**因环境而异或敏感**的配置放 profile 文件。切换环境（local / dev / prod）只改 active profile 即可，密钥不混在一起。

### Profile 的加载规则：叠加 + 冲突时覆盖

激活某个 profile 后，Spring Boot 的加载逻辑是：

```
1. 永远加载 application.yml            （基础配置）
2. 额外加载 application-{profile}.yml   （profile 专属，作为补充/覆盖）
3. 两者合并到同一个 Environment
4. 同一个 key 两边都有 → profile 专属文件胜出
   只有一边有的 → 直接用那边的值
```

**profile 专属文件是"补丁"，不是"替换"。** 激活 local 后，`application.yml` 里的端口/数据库/redis 照常全量生效，`application-local.yml` 只负责新增 `cos.client.*`（或覆盖冲突项）。

| 配置项 | application.yml | application-local.yml | local 激活后 |
|--------|:---:|:---:|------|
| `server.port` | ✅ | — | 生效（主配置） |
| `spring.datasource.*` | ✅ | — | 生效 |
| `cos.client.*` | — | ✅ | 生效（local 补充进来） |

### CosClientConfig 怎么读到 cos.client.*

```java
@Configuration
@ConfigurationProperties(prefix = "cos.client")
@Data
public class CosClientConfig {
    private String host;
    private String secretId;
    ...
}
```

`@ConfigurationProperties(prefix = "cos.client")` 把 Environment 里所有 `cos.client.*` 属性按字段名绑到字段上（靠 `@Data` 生成的 setter 注入）。这些值在 `application-local.yml` 里，**只有 local profile 激活、该文件被加载后才会出现**；否则字段全是 null。

### 激活 Profile 的五种方式

#### 方式①：写在 application.yml 里（开发默认，推荐）

```yaml
spring:
  profiles:
    active: local
```

> 本项目这行目前是注释掉的（`application.yml:19-20`），靠 IDE 激活。推荐取消注释，让配置自包含。

#### 方式②：命令行参数 `--`（打包部署 / 临时测试最常用）

参数放在 `-jar xxx.jar` **后面**，`--` 开头是 **Spring Boot 命令行参数**：

```bash
# 单个 profile
java -jar cc-picture-backend.jar --spring.profiles.active=local

# 多个 profile（逗号分隔，后面的覆盖前面的）
java -jar cc-picture-backend.jar --spring.profiles.active=dev,redis

# 顺便覆盖其它配置项
java -jar cc-picture-backend.jar --spring.profiles.active=prod --server.port=9000
```

Maven 启动（开发期）：

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

#### 方式③：环境变量 `SPRING_PROFILES_ACTIVE`（容器 / Docker / K8s 必备）

环境变量名是大写下划线形式（Spring 的 **relaxed binding** 自动把 `SPRING_PROFILES_ACTIVE` 映射到 `spring.profiles.active`）：

```bash
# Linux / macOS：临时设置（仅当前 shell 生效）
export SPRING_PROFILES_ACTIVE=local
java -jar cc-picture-backend.jar

# Linux / macOS：一行搞定（只对这条命令生效）
SPRING_PROFILES_ACTIVE=local java -jar cc-picture-backend.jar

# Windows CMD
set SPRING_PROFILES_ACTIVE=local
java -jar cc-picture-backend.jar

# Windows PowerShell
$env:SPRING_PROFILES_ACTIVE="local"
java -jar cc-picture-backend.jar
```

Docker：

```bash
# docker run 用 -e
docker run -e SPRING_PROFILES_ACTIVE=prod -p 8123:8123 cc-picture
```

```yaml
# docker-compose.yml
services:
  app:
    image: cc-picture
    environment:
      - SPRING_PROFILES_ACTIVE=prod
```

Kubernetes（在 Deployment 的 pod spec 里）：

```yaml
env:
  - name: SPRING_PROFILES_ACTIVE
    value: "prod"
```

> 环境变量方式的优点：**不用改代码、不用改命令行**，运维直接配，CI/CD 和容器化场景首选。

#### 方式④：JVM 系统属性 `-D`（IDE 调试常用）

参数放在 `-jar` **前面**：

```bash
java -Dspring.profiles.active=local -jar cc-picture-backend.jar
```

> ⚠️ `-D` 和 `--` 都能激活 profile，但**位置不能错**：
> - `-Dxxx=yyy` → JVM 系统属性，**必须在 `-jar` 之前**
> - `--xxx=yyy` → Spring 命令行参数，**必须在 `-jar` 之后**
>
> 写错位置会被当成 jar 的 main 方法参数，不生效也不报错，极难排查。

#### 方式⑤：IDE 运行配置（本地开发，本项目用的就是这种）

IntelliJ IDEA → Run/Debug Configurations，三种填法任选其一：

- **Active profiles** 栏填 `local`
- **VM options** 加 `-Dspring.profiles.active=local`
- **Environment variables** 加 `SPRING_PROFILES_ACTIVE=local`

### 优先级（高 → 低）

同一个配置出现在多处时，按优先级高的为准：

| 优先级 | 来源 | 示例 |
|:---:|------|------|
| 高 | 命令行参数 `--` | `--spring.profiles.active=local` |
| ↑ | JVM 系统属性 `-D` | `-Dspring.profiles.active=local` |
| ↑ | OS 环境变量 | `SPRING_PROFILES_ACTIVE=local` |
| ↑ | application-{profile}.yml | application-local.yml |
| 低 | application.yml | 基础配置 |

> 同一个 profile 下，`application-{profile}.yml` 覆盖 `application.yml`；多个 profile 同时激活时，`active` 列表里**靠后的覆盖靠前的**。

### 怎么确认到底激活了哪个

看启动日志（banner 之后），Spring Boot 会打印一行：

```
The following 1 profile is active: "local"
```

- 看到这行 → profile 激活成功，`application-local.yml` 已加载
- 看到 `No active profile set, fell back to default profiles: default` → 没激活，profile 文件没加载，`cos.client.*` 为 null

### 本项目的坑

`application.yml` 里 `spring.profiles.active: local` 是**注释掉的**（`:19-20`），所以：

- 纯按 yml 启动 → local 不激活 → `cos.client.*` 为 null → `CosClientConfig` 字段全空 → COSClient 创建失败
- 现在能跑 → 大概率是 IDE 运行配置（方式⑤）激活了 local

修复：取消 `application.yml:19-20` 的注释，配置即自包含，换人换机器都不踩坑。

### 经验教训

1. **profile 文件是"补丁"不是"替换"**：激活 profile 后主配置照常全量生效，profile 文件只新增 / 覆盖冲突项。
2. **激活方式按场景选**：开发用 IDE / yml；CI 和容器化用环境变量；临时调试用命令行 `--`。
3. **`-D`（-jar 前）vs `--`（-jar 后）位置不能错**，写错不生效也不报错，排查要靠启动日志。
4. **看启动日志确认 profile**："The following profile is active" 是排查"配置没生效"的第一步。
5. **配置自包含优先**：能写在 `application.yml` 里的 active profile 就别只依赖 IDE，否则换人 / 换机器就踩坑。

---

## editSpace 和 updateSpace 的业务意义：权限不同只是表象

两个接口都是"改空间"，但**权限不同只是表象，本质区别是"能改哪些字段"**——`updateSpace` 是管理端全量更新，`editSpace` 是用户端受限编辑。

### 业务定位

| | `updateSpace`（`/space/update`） | `editSpace`（`/space/edit`） |
|---|---|---|
| **定位** | 管理端：**全量更新** | 用户端：**受限编辑** |
| **给谁用** | 管理员 | 普通用户（本人空间） |
| **能干什么** | 改空间名 + **改级别** + **改配额** | **只能改空间名** |

### 核心区别：可改字段范围（由 DTO 控制）

这是**比权限更重要的区别**，通过两个不同的 Request DTO 实现"字段级权限控制"：

```java
// SpaceUpdateRequest —— 管理员用
private Long id;
private String spaceName;
private Integer spaceLevel;   // 空间级别（普通/专业/旗舰）
private Long maxSize;         // 最大容量
private Long maxCount;        // 最大数量

// SpaceEditRequest —— 用户用（注释写明：目前仅允许编辑空间名称）
private Long id;
private String spaceName;
```

用户走 `editSpace` 时，后端接收的是 `SpaceEditRequest`，**压根没有 `spaceLevel`、`maxSize`、`maxCount` 这几个字段**。就算用户想传，也接不进来。这就是设计意图：

> **防止用户自己给自己升级空间级别、自己把配额改大（白嫖）。** 升级和配额调整是管理员 / 付费才能做的事，普通用户只能改个名字。

### 其他非权限区别

| 维度 | updateSpace | editSpace |
|------|-------------|-----------|
| **权限校验方式** | `@AuthCheck(ADMIN_ROLE)` —— 基于**角色**（是不是管理员） | `checkSpaceAuth(loginUser, oldSpace)` —— 基于**资源所有权**（本人或管理员） |
| **是否设 editTime** | ❌ 不设 | ✅ `space.setEditTime(new Date())` |
| **fillSpaceBySpaceLevel 意义** | 有意义：管理员改了 spaceLevel → 按新级别重新填充配额 | ⚠️ **实际无意义**（见下） |
| **校验顺序** | getById → copy → validSpace → fill → update | copy → fill → validSpace → 设 editTime → getById → checkAuth → update |

### ⚠️ editSpace 里 fillSpaceBySpaceLevel 是多余的

代码注释 `// 自动填充数据?? 编辑空间需要吗？` 的质疑是对的。`fillSpaceBySpaceLevel` 的逻辑是：

```java
SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
if (spaceLevelEnum != null) {   // spaceLevel 为 null 时，这里直接不进
    // 填充 maxSize / maxCount
}
```

而 `SpaceEditRequest` 根本没有 `spaceLevel` → `space.getSpaceLevel()` 永远是 null → `getEnumByValue(null)` 返回 null → 这个方法**什么都不做**。所以 editSpace 里这一行是空跑，可以删掉。

### 为什么要有两个接口（设计本质）

这是经典的**"管理端 vs 用户端"双接口模式**，同一份资源、两个入口，各自有不同的：

```
              ┌─ 字段范围（全量 vs 受限）        ← 防白嫖，最小权限
同一个 Space ─┼─ 权限模型（角色 vs 所有权）      ← 管理员能改任意空间，用户只能改自己的
              └─ 校验 / 填充逻辑（按级别补配额 vs 只改名）
```

好处：**把"危险操作"（改级别 / 配额）和"安全操作"（改名）在接口层就隔离开**，而不是靠一个接口里写一堆 if-else 判断"当前用户能不能改这个字段"。字段层面的隔离比运行时判断更安全、更清晰。

### 经验教训

1. **看两个"看起来重复"的接口，先别急着合并**：先看它们的 DTO 字段范围——往往一个管危险字段、一个管安全字段，是刻意的字段级权限设计。
2. **字段级权限优于运行时判断**：把敏感字段从用户端 DTO 里直接去掉，比在业务代码里 `if (isAdmin)` 判断更安全（用户根本传不进来，攻击面更小）。
3. **权限模型有两种**：`@AuthCheck` 是基于**角色**（你是谁），`checkSpaceAuth` 是基于**资源所有权**（这东西是不是你的）—— 管理端常用前者，用户端常用后者。
4. **方法里调用的工具方法要确认它的前置条件**：像 `fillSpaceBySpaceLevel` 依赖 `spaceLevel` 非空，用户端根本不传这个字段时调用它就是空跑——别盲目复制粘贴管理端的调用。

---

# 2026/06/24

## 上传图片到空间：额度校验与更新（弱校验 + 最终一致）

上传图片到空间时，**确实需要"校验额度 + 更新额度"两步**——上传前判断空间还有没有容量 / 数量，上传后把新图片的大小和数量累加进空间的 `totalSize` / `totalCount`。但这里讨论的核心是：**这两步用"弱校验"而不是"强校验"**。

### 核心思想：弱校验（最终一致）vs 强校验（加锁）

空间额度校验有两种做法，本项目选的是前者：

| | 强校验（加锁） | **弱校验（本项目方案）** |
|---|---|---|
| 上传时怎么做 | 加锁 → 校验 → 插图 → 累加额度 → 解锁（全包在事务里） | **不加锁**，粗略校验 → 插图 → 累加额度 |
| 并发下额度会不会被突破 | 绝不会 | **可能会，但只超一点点** |
| 性能 | 差（每次上传都要抢锁，串行） | 好（并发上传互不阻塞） |
| 复杂度 | 高（锁、事务、死锁） | 低 |
| 兜底 | 不需要 | 靠**限流 + 定时任务**事后纠正 |

设计哲学：**"上传时不为追求额度 100% 精确而付出加锁的性能代价，允许短暂轻微超额，事后用限流和定时任务兜底。"**

### 为什么"瞬间大量上传，影响也不大"

这是该方案的关键论证，有四层理由：

**① 超额是"有界的小额溢出"，不是无限突破**

并发竞态只会让额度被**少量**突破，不会失控：

```
某时刻：空间已用 95MB，上限 100MB，还剩 5MB
线程A：读 totalSize=95，95+2=97 ≤100 → 通过
线程B：读 totalSize=95，95+2=97 ≤100 → 通过   ← 读到的是同一个旧值！
线程C：读 totalSize=95，95+2=97 ≤100 → 通过   ← 同上
→ A/B/C 都通过，实际各传 2MB → 用了 101MB，超了 1MB
```

最坏情况 = "同一瞬间并发上传的图片总量"。这是**有上界的**，不是无底洞。就算用户瞬间并发传 100 张，顶多超 100 张的量，系统不会因此崩。

**② 强校验的代价太高，不划算**

每次上传都要给空间加锁（行锁或分布式锁），意味着**同一个空间的上传是串行的**——十个用户往同一个团队空间传图，得排队。对图片类应用，上传吞吐和延迟比"额度精确到字节"重要得多。为了堵住那 1MB 的溢出拖慢所有上传，不划算。

**③ 额度是"软业务规则"，不是"硬安全约束"**

| 场景 | 超额的后果 | 该用哪种校验 |
|---|---|---|
| **库存扣减** | 超卖 = 真金白银损失 | 必须强校验 |
| **账户余额 / 支付** | 透支 = 钱没了 | 必须强校验 |
| **空间额度** | 超几 MB 存储 = 几乎零成本 | **弱校验可接受** |

空间额度超一点点，损失的是"一点点对象存储的便宜空间"，没有真实资金 / 法律风险。低违规成本 → 没必要上重锁。

**④ 存储便宜**

溢出的那点容量，在对象存储（COS / OSS）上成本几乎可以忽略。这是"影响不大"的经济底座。

### 瞬间超额的原理：check-then-act 竞态

上面线程 A/B/C 的例子就是经典的 **"检查-再-执行"竞态**：

```
检查阶段：多个线程同时读到"同一个旧值"，都判断"还没超"
执行阶段：各自插入图片 + 累加 → 实际总和超出额度
```

要 100% 杜绝它，就得在"检查 + 累加"之间加锁 / 用原子操作（强校验），代价就是性能。本项目选择**接受这个小窗口**，事后兜底。

### 兜底机制①：限流（把"瞬间"的口子收窄）

给上传接口加限流（比如每用户每分钟最多 10 次上传）。作用是**限制竞态窗口内的并发量**——即使每次都漏检，限流把"一瞬间的并发上传数"卡死了，溢出量就被压在一个很小的范围内：

```
无限流：用户瞬间 1000 个并发请求 → 可能超 1000 张的量
有限流：每分钟 10 次              → 最多超 10 张的量
```

### 兜底机制②：定时任务（把额度"重新对账"，最终一致）

起一个定时任务（集群下要加分布式锁，前面笔记讲过），周期性地：

1. 扫描所有空间，从 `picture` 表**重新汇总**每个空间的实际 `SUM(picSize)` 和 `COUNT(*)`；
2. 和 space 表里存的 `totalSize` / `totalCount` 对比，**纠正偏差**（把存的不准的额度重新同步成真实值）；
3. 标记出**超额的空间**，做定制处理：告警、阻止后续上传、降级空间、通知用户扩容……

这就是"**最终一致**"——实时校验漏掉的，定时任务兜回来。哪怕某一刻空间真的超了，最迟在下个周期就会被发现和纠正。

### 三个机制怎么配合

```
上传时（弱校验，挡住绝大多数正常情况）
    ↓ 漏网之鱼（轻微超额）
限流（把漏网量压到很小）
    ↓
定时任务（周期对账，纠正额度 + 处理超额空间）
    → 最终一致 ✅
```

弱校验挡大头，限流压小头，定时任务收尾。三层配合，既快又稳。

### 适用边界：什么时候绝不能用弱校验

该方案成立的前提是**"违规成本低、可事后纠正"**。以下场景**必须强校验**：

- ❌ **库存超卖 / 秒杀扣减** → DB `UPDATE ... SET stock=stock-1 WHERE stock>0` 或 Redis Lua 原子脚本
- ❌ **账户扣款 / 转账** → 数据库行锁 + 事务
- ❌ **优惠券领取防超发** → 原子操作

> 这呼应项目里另一个地方：`addSpace` 用"加锁 + 事务"保证"一个用户一个私有空间"（那是个**硬约束**，不能违反，所以强校验）；而空间额度用弱校验——**同一个项目里，约束的"硬度"不同，手段就不同**。

### 代码大概长什么样（弱校验 + 原子累加）

```java
// 上传图片到空间 —— 弱校验 + 原子累加
public PictureVO uploadPictureToSpace(Long spaceId, ...) {
    Space space = getById(spaceId);
    long newSize = uploadResult.getPicSize();

    // ① 弱校验：不加锁，粗略判断（有并发窗口，但不追求绝对精确）
    if (space.getTotalSize() + newSize > space.getMaxSize()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间容量不足");
    }
    if (space.getTotalCount() + 1 > space.getMaxCount()) {
        throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间数量超限");
    }

    // ② 保存图片
    save(picture);

    // ③ 更新额度：用 setSql 原子累加，避免计数器本身丢更新
    update().eq("id", spaceId)
            .setSql("totalSize = totalSize + " + newSize)
            .setSql("totalCount = totalCount + 1")
            .update();
}
```

> ⚠️ 关键区分：**计数器更新是原子的**（`setSql("totalSize = totalSize + ?")` 在 DB 层加，不会丢更新），**只有"校验"那一步是松的**。所以额度数字本身不会错乱，只是"校验"可能放行几个本该拒绝的并发请求。

### 经验教训

1. **校验的"硬度"要和"违规成本"匹配**：资金 / 库存等高成本场景必须强校验，额度 / 配额等低成本场景可以弱校验 + 兜底，别一刀切上重锁。
2. **"check-then-act"天然有竞态**：检查和执行之间没锁就有窗口，要么加锁堵死（强校验），要么接受溢出事后兜底（弱校验），二选一，别假装没这个窗口。
3. **最终一致的三件套**：弱校验（实时挡大头）+ 限流（压小溢出量）+ 定时任务（周期对账纠正），是处理"低风险软约束"的经典组合拳。
4. **计数器更新要用原子操作**（`setSql("col = col + ?")`），别用"读出来 + 算 + 写回"的三步式，否则并发下计数器本身就会丢更新——这跟校验松不松是两个独立的问题。
5. **加锁是手段不是目的**：本项目 `addSpace` 加锁（硬约束）、额度不加锁（软约束），体现的是"按约束硬度选手段"，而不是"处处加锁才安全"。

---

## 编程式事务进阶：精准控制回滚与提交（setRollbackOnly）

承接前文"编程式事务 TransactionTemplate：谁负责回滚"。那篇只讲了**自动控制**（异常→回滚，return→提交），这里补充**手动控制 `status.setRollbackOnly()`** 以及完整的三种控制手段。

### execute 的核心规则

`transactionTemplate.execute(callback)` 的事务结局由三件事决定：

| 回调里发生什么 | 事务结果 | 调用方收到 |
|---|---|---|
| 抛异常 | 回滚 | 异常（继续往上抛） |
| 正常 return | 提交 | 返回值 |
| `status.setRollbackOnly()` | 回滚（即使后面正常 return） | 返回值（无异常） |

### 三种控制方式

| 控制方式 | 怎么写 | 事务结果 | 异常是否抛出 | 调用方能否感知失败 |
|---|---|---|---|---|
| **抛异常** | `throw new BusinessException(...)` | 回滚 | ✅ 抛出 | ✅ 能（catch / 全局异常处理器） |
| **setRollbackOnly** | `status.setRollbackOnly(); return 标识值;` | 回滚 | ❌ 不抛 | ⚠️ 只能靠返回值判断 |
| **正常 return** | `return 值;` | 提交 | — | — |

### status 对象：手动控制的手柄

回调的 `status` 是个 `TransactionStatus`，提供手动控制 API：

- `status.setRollbackOnly()` —— 手动标记回滚（不抛异常）
- `status.isRollbackOnly()` —— 查是否已被标记回滚
- `status.isCompleted()` —— 查事务是否已结束
- `status.isNewTransaction()` —— 查是不是新事务（嵌套事务时用）

### 关键认知：setRollbackOnly 回滚的是"整个事务"，不能保留一部分

`setRollbackOnly()` 标记的是**当前整个事务**回滚。所以：

```java
transactionTemplate.execute(status -> {
    step1();                       // 已执行
    if (某条件) {
        status.setRollbackOnly();  // ← 标记回滚
        return null;
    }
    step2();
    return true;
});
```

→ step1 和 step2 **全部回滚**，不存在"保留 step1、放弃 step2"。**事务的本质就是全成或全退，做不到保留一半。**

### 抛异常 vs setRollbackOnly 的真正区别

两者**数据结果完全相同**（都是整个事务回滚），唯一区别在**失败信号怎么传递**：

- **抛异常**：回滚 + 把异常抛给调用方 → 调用方靠 catch 感知失败。
- **setRollbackOnly + return**：回滚 + 返回一个值（不抛）→ 调用方靠返回值判断。

> ⚠️ 用 `setRollbackOnly` 时：调用方拿不到异常，**必须通过返回值（如返回 null / false / 业务标识）告诉它失败了**，否则调用方可能误以为成功。

### setRollbackOnly 的正当用法

适合"想回滚、但不想用异常控制流程、想用返回值表达业务结果"的场景：

```java
String result = transactionTemplate.execute(status -> {
    step1();
    if (!业务条件满足) {
        status.setRollbackOnly();      // 条件不满足 → 回滚 step1
        return "CONDITION_NOT_MET";     // 用返回值（而非异常）表达结果
    }
    step2();
    return "SUCCESS";
});
// 根据返回值分支处理，不依赖异常控制流
```

### 当前 deletePicture 代码的控制分析（纯自动控制）

```java
transactionTemplate.execute(status -> {
    boolean result = this.removeById(pictureId);
    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);            // ① 删失败 → 抛 → 回滚
    Long spaceId = oldPicture.getSpaceId();
    if (spaceId != null) {
        boolean update = spaceService.lambdaUpdate()
                .eq(Space::getId, spaceId)
                .setSql("totalSize = totalSize - " + oldPicture.getPicSize())
                .setSql("totalCount = totalCount - 1")
                .update();
        ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败"); // ② 额度失败 → 抛 → 回滚
    }
    return true;                                                        // ③ 全成功 → 提交
});
```

- 虽然写了 `status ->`，但**全程没用到 `status`** —— 纯"自动控制"，手动开关闲置。
- 控制靠的是**抛异常点的位置**：删除图片和释放额度被绑成一个原子单元。
- **② 的价值**：删除成功但额度更新失败 → 抛异常 → 回滚 → **删除也被撤销**，绝不会出现"图删了、额度没还"的中间状态。这就是事务原子性的保证。

### 异常类型小坑（顺带，和 @Transactional 对比）

| | 抛 RuntimeException | 抛 checked Exception |
|---|---|---|
| `@Transactional`（默认） | 回滚 | **不回滚**（默认提交），需配 `rollbackFor` 才回滚 |
| `TransactionTemplate.execute` | 回滚 | 也回滚（包成 `UndeclaredThrowableException` 再抛） |

但 `TransactionCallback` 接口方法**没声明 `throws`**，普通 lambda 只能抛 RuntimeException。所以 `ThrowUtils.throwIf` 抛的 `BusinessException`（继承 RuntimeException）走的是"RuntimeException → 回滚"，和 `@Transactional` 默认行为一致；checked 异常那条分支实际很少触发。

### 经验教训

1. **自动控制（默认）**：异常→回滚，正常 return→提交，够覆盖大多数场景（本项目 `deletePicture` 就用这种）。
2. **手动控制 `setRollbackOnly`**：回滚整个事务但不抛异常，靠返回值表达结果——适合"想回滚但不想用异常控制流程"的场景。
3. **事务是原子的**：`setRollbackOnly` 回滚全部，不存在"保留一部分"。想"部分成功"只能把那部分挪出事务（最终一致），那是另一个设计选择。
4. **用 `setRollbackOnly` 必须用返回值通知失败**，否则调用方误以为成功。
5. **回滚的本质是"撤销已执行的写操作"**：`removeById` 已执行，失败时靠事务回滚把它撤销——这就是事务防"中间态"的核心价值。

---

## 枚举转换的精简姿势：`@EnumValue` + `BeanUtil`

补充前文"`@TableField(typeHandler = ...)`"那篇。那篇讲的是 MyBatis **原生**的枚举处理器（`EnumTypeHandler`/`EnumOrdinalTypeHandler`，要逐个字段标 `@TableField`）。这里讲更精简的 **MyBatis-Plus 专属注解 `@EnumValue`**，以及"枚举→VO"的属性复制法。

### 先厘清：这是两个不同的转换

```
数据库(int: 0/1/2)  ←【转换 A】→  枚举(SpaceLevelEnum)  ←【转换 B】→  VO(SpaceLevel, 含 text/maxCount)
```

- **转换 A（库 ↔ 枚举）**：DB 只存数字，**有注解能全自动**（重点）
- **转换 B（枚举 → VO）**：VO 比 DB 多了 text/maxCount 这些元数据，DB 帮不上，**靠属性复制**

### 转换 A：库 ↔ 枚举 —— 用 `@EnumValue`（最精简，注解）

MyBatis-Plus 内置枚举处理。在枚举里**标一下哪个字段对应数据库列**：

```java
import com.baomidou.mybatisplus.annotation.EnumValue;

public enum SpaceLevelEnum {
    COMMON("普通版", 0, 100, 100L * 1024 * 1024),
    PROFESSIONAL("专业版", 1, 1000, 1000L * 1024 * 1024),
    FLAGSHIP("旗舰版", 2, 10000, 10000L * 1024 * 1024);

    private final String text;

    @EnumValue          // ← 标记：这个 value 字段就是数据库存的那一列
    private final int value;

    private final long maxCount;
    private final long maxSize;
}
```

然后**实体里直接用枚举类型**，不再是 `Integer`：

```java
public class Space {
    // 改前：private Integer spaceLevel;  (手动 getEnumByValue / getValue)
    private SpaceLevelEnum spaceLevel;   // ← 直接枚举类型
}
```

这样 MP 的 `MybatisEnumTypeHandler` **自动**完成双向转换：

- 写库：`enum → 0/1/2`（取 `@EnumValue` 字段的值）
- 读库：`0/1/2 → enum`（按 value 反查枚举）

**全程不用写 `getEnumByValue()` / `getValue()`，一个注解搞定。**

> 配置：MP 3.5.2+ 默认就用 `MybatisEnumTypeHandler`，一般不用配。老版本可在 yml 加：
> `mybatis-plus.configuration.default-enum-type-handler=com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler`

#### 三种枚举处理器对比（含原生）

| 方式 | 库里存什么 | 稳不稳 | 精简度 |
|------|-----------|--------|--------|
| MyBatis `EnumTypeHandler` | 枚举**名字**（"COMMON"） | 稳，但占空间 | 要每个字段标 `@TableField(typeHandler=...)` |
| MyBatis `EnumOrdinalTypeHandler` | 枚举**序号**（0/1/2，按定义顺序） | **不稳**，枚举换顺序数据就错 | 同上 |
| **MP `@EnumValue`** ★ | 你指定的**业务值**（0/1/2） | **稳**（显式 value，不是序号） | **一个注解，最精简** |

`@EnumValue` 既精简又比 `EnumOrdinalTypeHandler` 安全——存的是**业务 value 不是 ordinal**，枚举重排也不影响数据。

#### 替代写法：`IEnum<T>` 接口（不用注解）

不想加注解，就让枚举实现 MP 的 `IEnum` 接口，效果一样：

```java
public enum SpaceLevelEnum implements IEnum<Integer> {
    ...;
    @Override
    public Integer getValue() { return value; }   // 告诉 MP 用这个值入库
}
```

`@EnumValue` 和 `IEnum` 二选一，前者是注解、后者是接口，功能等价。

### 转换 B：枚举 → VO —— 用属性复制

VO（`SpaceLevel` 含 value/text/maxCount/maxSize）比 DB 丰富，DB 只能帮到 value，剩下三个字段得从枚举拿。精简法：

#### 方法①：`BeanUtil.copyProperties`（Hutool，一行）

枚举字段名（text/value/maxCount/maxSize）和 VO 字段名**完全对应**，按名字复制即可：

```java
// 单个
SpaceLevel vo = BeanUtil.copyProperties(spaceLevelEnum, SpaceLevel.class);

// 批量（返回所有级别给前端）
List<SpaceLevel> list = Arrays.stream(SpaceLevelEnum.values())
        .map(e -> BeanUtil.copyProperties(e, SpaceLevel.class))
        .collect(Collectors.toList());
```

比手写 `new SpaceLevel(e.getValue(), e.getText(), e.getMaxCount(), e.getMaxSize())` 短，也不怕参数传反。

#### 方法②：MapStruct（编译期生成，类型安全）

字段多、转换频繁时，用 MapStruct 定义个接口，编译期自动生成转换代码：

```java
@Mapper
public interface SpaceLevelMapper {
    SpaceLevel toVO(SpaceLevelEnum enumValue);
    List<SpaceLevel> toVOList(List<SpaceLevelEnum> list);
}
```

调用：`spaceLevelMapper.toVO(enumValue)`。性能最好（纯 getter/setter，没有反射），但要引依赖 + 写接口。

### 套到本项目的改造

现状（`Space.spaceLevel` 是 `Integer`，全手动）：

```java
// 读库后手动转枚举
SpaceLevelEnum levelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
// 手动构造 VO
new SpaceLevel(enum.getValue(), enum.getText(), enum.getMaxCount(), enum.getMaxSize());
```

改造后：

```java
// 1. 枚举加 @EnumValue，实体改成枚举类型 → 库↔枚举全自动，干掉 getEnumByValue
private SpaceLevelEnum spaceLevel;

// 2. 枚举→VO 用 BeanUtil → 干掉手写构造
BeanUtil.copyProperties(spaceLevelEnum, SpaceLevel.class)
```

两步都从"手动"变"自动 / 一行"。

### 经验教训

1. **`@EnumValue` 是枚举映射库列的"官方精简法"**：标记哪个字段入库，实体直接用枚举类型，MP 自动双向转换，省掉所有 `getEnumByValue`/`getValue` 手写代码。
2. **存业务 value，别存 ordinal**：`@EnumValue` 存的是显式 value（0/1/2），枚举重排数据不变；`EnumOrdinalTypeHandler` 存的是位置序号，重排就错——能用 `@EnumValue` 就别用 ordinal。
3. **"枚举→VO"和"库↔枚举"是两件事**：`@EnumValue` 只管后者；VO 因为比 DB 多字段（text/maxCount 等元数据），仍需 `BeanUtil`/`MapStruct` 复制，别指望一个注解包打天下。
4. **属性复制靠"字段同名"**：`BeanUtil.copyProperties` 能一行转换的前提是枚举和 VO 字段名、类型对得上；对不上时要么改名、要么手写，别强行用。
5. **MyBatis-Plus 的注解优于 MyBatis 原生**：`@EnumValue`（MP）比逐字段标 `@TableField(typeHandler=EnumTypeHandler.class)`（原生）更省、更安全，能用就用。
