package com.zjcc.ccpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zjcc.ccpicturebackend.config.CosClientConfig;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用COS操作类无法满足需求, 创建本类
 * 1）需要校验格式
 * 2）需要指定存储路径，就像 Redis 的 key 的路径一样，指定路径防止冲突
 * 3）需要获取上传的图片信息
 */
@Service
@Slf4j
@Deprecated // 已废弃 改用抽象目标类 PictureUploadTemplate
public class FileManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;
    /**
     * 1 兆
     */
    private static final long ONE_M = 1024 * 1024L;

    // 允许上传的文件后缀
    // TODO
    // 1.补充更严格的校验，比如为支持的图片格式定义枚举，仅允许上传枚举定义的格式
    // 2. 可以用枚举类（FileUploadBizEnum）支持根据业务场景区分文件上传路径、校验规则等，从而复用 FileManager
    private static final List<String> ALLOW_FORMAT_LIST = Arrays.asList("jpeg", "jpg", "png", "webp");

    // ...

    /**
     * 上传本地图片
     *
     * @param multipartFile    文件
     * @param uploadPathPrefix 上传路径前缀 由调用方指定上传文件到哪个目录
     * @return
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验图片
        validPicture(multipartFile);

        String originFilename = multipartFile.getOriginalFilename();
        long fileSize = multipartFile.getSize();
        log.info("开始上传图片到COS，文件名 = {}, 大小 = {}bytes, 路径前缀 = {}", originFilename, fileSize, uploadPathPrefix);

        // 图片上传地址
        // 如果多个项目共享存储桶，可以给上传文件路径再加一个ProjectName前缀
        String uuid = RandomUtil.randomString(16);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        // 根据自己的需求定义文件上传地址
        // 此处给文件名前增加了上传日期和 16 位 uuid 随机数，便于了解文件上传时间并防止文件重复
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 创建临时文件
            file = File.createTempFile(uploadPath, null);
            multipartFile.transferTo(file);

            // 上传图片
            long startTime = System.currentTimeMillis();
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            long endTime = System.currentTimeMillis();
            log.info("COS上传成功，路径 = {}, 耗时 = {}ms", uploadPath, endTime - startTime);

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
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
        } catch (Exception e) {
            log.error("图片上传到对象存储失败，路径 = {}", uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 无论是否上传成功，都要删除创建的临时文件
            this.deleteTempFile(file);
        }
    }

    /**
     * 通过URL上传 图片
     *
     * @param fileUrl          文件URL
     * @param uploadPathPrefix 上传路径前缀 由调用方指定上传文件到哪个目录
     * @return
     * 支持输入一个远程 URL，直接将网上已有的图片导入到我们的系统中; 提高上传图片的效率
     * 1）下载图片：后端服务器从指定的远程 URL 下载图片到本地临时存储。对于 Java 项目，可以直接使用 Hutool 的 HttpUtil.downloadFile 方法一行代码完成。
     * <p>
     * 2）校验图片：跟验证本地文件一样，需要校验图片的格式、大小等。
     * 传统的校验思路是先把文件下载到本地，再对本地文件进行校验，有没有更节省资源的方法呢？
     * **其实可以先对 URL 本身进行校验。**首先是校验 URL 字符串本身的合法性，比如要是一个合理的 URL 地址。
     * 此外，可以先使用 HEAD 请求来获取 URL 对应文件的元信息（如文件大小、格式等）。HEAD 请求仅返回 HTTP 响应头信息，而不会下载文件的内容，大大降低了网络流量的消耗。
     * 此处不能使用 GET 请求，它会获取完整文件。
     * <p>
     * 3）上传图片：将校验通过的图片上传到对象存储服务，生成存储 URL
     */
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // 校验图片
        validPicture(fileUrl);
        // 构造图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename  = FileUtil.mainName(fileUrl);
        log.info("开始上传图片到COS，文件名 = {}, 路径前缀 = {}", originFilename, uploadPathPrefix);

        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);

        //创建临时文件
        File tempFile = null;
        try {
            // 从传入的URL 将文件下载到本地临时文件
            tempFile = File.createTempFile(uploadPath, null);
            HttpUtil.downloadFile(fileUrl, tempFile);

            // 上传图片到COS
            long startTime = System.currentTimeMillis();
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, tempFile);
            long endTime = System.currentTimeMillis();
            log.info("COS上传成功，路径 = {}, 耗时 = {}ms", uploadPath, endTime - startTime);

            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
            uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
            uploadPictureResult.setPicWidth(picWidth);
            uploadPictureResult.setPicHeight(picHeight);
            uploadPictureResult.setPicScale(picScale);
            uploadPictureResult.setPicFormat(imageInfo.getFormat());
            uploadPictureResult.setPicSize(FileUtil.size(tempFile));
            uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath);
            return uploadPictureResult;
        } catch (Exception e) {
            log.error("图片上传到对象存储失败，路径 = {}", uploadPath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 无论是否上传成功，都要删除创建的临时文件
            this.deleteTempFile(tempFile);
        }

    }

    /**
     * 校验文件
     * @param multipartFile multipart 文件
     * 由于文件校验规则较复杂，单独抽象为 validPicture 方法，对文件大小、类型进行校验
     */
    public void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1. 校验文件大小
        long fileSize = multipartFile.getSize();
        ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
        // 2. 校验文件后缀
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型错误");
    }

    /**
     * 校验 URL
     * @param fileUrl URL地址
     * 校验 URL 格式、协议、文件是否存在、文件格式、文件大小
     */
    public void validPicture(String fileUrl) {
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR, "文件地址不能为空");
        try {
            // 1.校验url格式
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "URL格式不正确");
        }
        // 2. 校验URL协议
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
            }
            // 5. 校验文件大小
            String contentLengthStr = response.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    long contentLength = Long.parseLong(contentLengthStr);
                    // 限制文件大小为 2MB
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


    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        // 删除临时文件
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }
}
