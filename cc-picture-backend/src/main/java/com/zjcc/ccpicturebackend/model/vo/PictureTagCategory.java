package com.zjcc.ccpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 图片 标签-分类列表 视图
 */
@Data
public class PictureTagCategory {

    /**
     * 标签列表
     */
    private List<String> tagList;

    /**
     * 分类列表
     */
    private List<String> categoryList;
}
