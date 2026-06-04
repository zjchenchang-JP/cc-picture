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
  
    private static final long serialVersionUID = 1L;  
}

