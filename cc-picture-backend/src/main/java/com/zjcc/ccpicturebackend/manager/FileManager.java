package com.zjcc.ccpicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.zjcc.ccpicturebackend.config.CosClientConfig;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * 通用COS操作类无法满足需求
 * 1）需要校验格式
 * 2）需要指定存储路径，就像 Redis 的 key 的路径一样，指定路径防止冲突
 * 3）需要获取上传的图片信息
 */
@Service
@Slf4j
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
     * 上传图片
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
