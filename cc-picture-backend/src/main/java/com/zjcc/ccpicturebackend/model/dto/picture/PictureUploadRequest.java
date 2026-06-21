package com.zjcc.ccpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 上传图片请求 URL、尺寸、大小、格式（从文件自动获取）
 * 场景：想把一张模糊的图片换成高清的 上传新图片文件替换旧的
 *     保留老记录的 id，替换图片文件相关的所有属性
 */
@Data
public class PictureUploadRequest implements Serializable {
  
    /**  
     * 图片 id（用于修改）  
     */  
    private Long id;

    /**
     * 文件地址
     * 扩展字段；支持通过url上传图片
     */
    private String fileUrl;

    /**
     * 图片名称 - 扩展功能字段 批量修改上传图片名
     * 图片名称是在 uploadPicture 方法中传入并设置给 Picture 图片对象的
     * 所以需要给该方法接受的参数 PictureUploadRequest 类中补充 picName 参数
     */
    private String picName;

    /**
     * 空间 id
     */
    private Long spaceId;
  
    private static final long serialVersionUID = 1L;  
}

