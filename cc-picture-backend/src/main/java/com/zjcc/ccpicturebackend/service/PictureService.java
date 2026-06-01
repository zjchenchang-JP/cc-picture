package com.zjcc.ccpicturebackend.service;

import com.zjcc.ccpicturebackend.model.dto.picture.PictureUploadRequest;
import com.zjcc.ccpicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

/**
* @author 86187
* @description 针对表【picture(图片)】的数据库操作Service
* @createDate 2026-05-31 18:31:20
*/
public interface PictureService extends IService<Picture> {

    /**
     * 上传图片
     *
     * @param multipartFile
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(MultipartFile multipartFile,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

}
