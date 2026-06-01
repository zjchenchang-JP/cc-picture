package com.zjcc.ccpicturebackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.exception.ThrowUtils;
import com.zjcc.ccpicturebackend.manager.FileManager;
import com.zjcc.ccpicturebackend.model.dto.file.UploadPictureResult;
import com.zjcc.ccpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.PictureVO;
import com.zjcc.ccpicturebackend.service.PictureService;
import com.zjcc.ccpicturebackend.mapper.PictureMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.util.Date;

/**
* @author 86187
* @description 针对表【picture(图片)】的数据库操作Service实现
* @createDate 2026-05-31 18:31:20
*/
@Slf4j
@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
    implements PictureService{

    @Resource
    private FileManager fileManager;

    @Override
    public PictureVO uploadPicture(MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest, User loginUser) {
        log.info("开始上传图片，用户ID = {}, pictureId = {}, 文件名 = {}",
                loginUser.getId(),
                pictureUploadRequest != null ? pictureUploadRequest.getId() : null,
                multipartFile != null ? multipartFile.getOriginalFilename() : null);

        ThrowUtils.throwIf(null == loginUser, ErrorCode.NO_AUTH_ERROR);
        // 判断新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片要校验图片是否存在
        if (pictureId != null) {
            // 说明是更新
            boolean exists = this.lambdaQuery().eq(Picture::getId, pictureId)
                    .exists();
            ThrowUtils.throwIf(!exists,ErrorCode.NOT_FOUND_ERROR,"图片不存在");
        }
        // 上传图片
        // 按照用户 id 划分 上传目录
        String uploadPathPrefix = String.format("public/%s", loginUser.getId());
        UploadPictureResult uploadPictureResult = fileManager.uploadPicture(multipartFile, uploadPathPrefix);

        // 防御性编程：校验返回结果
        ThrowUtils.throwIf(uploadPictureResult == null, ErrorCode.SYSTEM_ERROR, "图片上传失败");

        // 构造要入库的图片信息
        Picture picture = new Picture();
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(uploadPictureResult.getPicName());
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        // 如果 pictureId 不为空，表示更新，否则是新增
        if (pictureId != null) {
            // 如果是更新还需传入id 和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        boolean result = this.saveOrUpdate(picture);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR,"图片上传失败");
        log.info("图片上传成功，图片ID = {}, URL = {}, 用户ID = {}", picture.getId(), picture.getUrl(), loginUser.getId());
        return PictureVO.objToVo(picture);
    }
}




