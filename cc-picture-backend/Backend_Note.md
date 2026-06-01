# 2025/05/31
## 如何实现图片上传和下载？
图片本质上是一种 “小型” 文件，那么我要思考：将文件上传到哪里？从哪里下载？

最简单的方式就是上传到后端项目所在的服务器，直接使用 Java 自带的文件读写 API 就能实现。但是，这种方式存在不少缺点，比如：

- 不利于扩展：单个服务器的存储是有限的，如果存满了，只能再新增存储空间或者清理文件。

- 不利于迁移：如果后端项目要更换服务器部署，之前所有的文件都要迁移到新服务器，非常麻烦。

- 不够安全：如果忘记控制权限，用户很有可能通过恶意代码访问服务器上的文件，而且想控制权限也比较麻烦，需要自己实现。

- 不利于管理：只能通过一些文件管理器进行简单的管理操作，但是缺乏数据处理、流量控制等多种高级能力。

因此，除了存储一些需要清理的临时文件之外，通常不会将用户上传并保存的文件（比如用户头像和图片）直接上传到服务器，而是更推荐使用专业的第三方存储服务，专业的工具做专业的事。其中，最常用的便是 对象存储

### 什么是对象存储？
对象存储是一种存储 海量文件 的 分布式 存储服务，具有高扩展性、低成本、可靠安全等优点。
比如开源的对象存储服务 MinIO，还有商业版的云服务，像亚马逊 S3（Amazon S3）、阿里云对象存储（OSS）、腾讯云对象存储（COS）等等

### 本项目採用 设计方案
创建图片其实包括了 2 个过程：上传图片文件 + 补充图片信息并保存到数据库中

有 2 种常见的处理方式：

- 1）先上传再提交数据：用户直接上传图片，系统生成图片的存储 URL；然后在用户填写其他相关信息并提交后，才保存图片记录到数据库中。

- 2）上传图片时直接保存记录：在用户上传图片后，系统立即生成图片的完整数据记录（包括图片 URL 和其他元信息），无需等待用户点击提交，图片信息就立刻存入了数据库中。之后用户再填写其他图片信息，相当于编辑了已有图片记录的信息。

方案 1 的优点是流程简单，但缺点是如果用户不提交，图片会残留在存储中，导致空间浪费；方案 2 则可以理解为保存了 “图片草稿”，即使用户不填写任何额外信息，也能找到之前的创建记录。
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

在我们的系统中，由于图片是核心资源，所以此处选择方案 2。 便于对图片进行溯源，还可以对图片上传做一些限制 —— 比如发现用户上传资源过多，就禁止上传

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
file = File.createTempFile(uploadPath, null);

// 将上传文件内容写入临时文件
multipartFile.transferTo(file);

// 使用临时文件上传到 COS
cosManager.putPictureObject(uploadPath, file);

// 无论成功失败，都删除临时文件
this.deleteTempFile(file); // 在 finally 块中
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
| 问题 | 答案 |
|------|------|
| 为什么要临时文件？ | COS SDK 需要 File 对象，不能直接用 MultipartFile |
| 为什么最后要删除？ | 临时文件只在传递数据时需要，上传后就没用了 |
| 不删除会怎样？ | 磁盘空间会被占满，最终服务器崩溃 |

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

| 对比项 | 方案1（先上传后提交） | 方案2（上传即保存） |
|--------|---------------------|-------------------|
| 数据库写入时机 | 用户点提交后 | 图片上传完成时 |
| 用户不提交会怎样 | 图片浪费占用空间 | 图片已保存，只是信息不完整 |
| 后续操作 | 创建新记录 | 编辑已有记录 |

**简单说：方案1是"暂存"，方案2是"直接存"**

---

## 生产环境代码完善建议

### 1. 空值校验

在 `PictureServiceImpl.java` 中，调用 `fileManager.uploadPicture()` 后需要做空值校验：

```java
UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);
ThrowUtils.throwIf(uploadPictureResult == null, ErrorCode.SYSTEM_ERROR, "图片上传失败");
```

**原因：防御性编程**
- 虽然 `FileManager.uploadPicture()` 正常返回时不会返回 null（失败会抛异常）
- 但如果未来代码变更，可能引入 bug
- 符合防御性编程原则

### 2. 日志完善

生产环境需要补充关键日志，便于排查问题：

#### PictureServiceImpl.java

| 位置 | 日志类型 | 记录内容 |
|------|---------|---------|
| 方法入口 | info | 用户ID、pictureId、文件名 |
| 方法出口（成功） | info | 图片ID、URL、用户ID |

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

| 位置 | 日志类型 | 记录内容 |
|------|---------|---------|
| 方法入口 | info | 文件名、大小、路径前缀 |
| COS上传成功 | info | 路径、耗时 |
| COS上传失败 | error | 路径、异常信息 |

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

| 方面 | 说明 |
|------|------|
| **用途** | 处理文件上传请求（multipart/form-data） |
| **"file"** | 绑定请求中 name="file" 的部分 |
| **绑定对象** | 通常绑定到 `MultipartFile` 类型 |

### 与 @RequestParam 的区别

```java
// @RequestParam - 用于普通表单参数
@PostMapping("/submit")
public void submit(@RequestParam("username") String username) { }

// @RequestPart - 用于文件上传
@PostMapping("/upload")
public void upload(@RequestPart("file") MultipartFile file) { }
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