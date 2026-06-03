package com.zjcc.ccpicturebackend.model.dto.picture;

import com.zjcc.ccpicturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.List;

/**
 * 图片查询请求
 * 搜索框：输入"风景" → 搜 name 和 introduction
 * 筛选条件：只想看 name 包含"风景"的图片 → 只搜 name
 */
@EqualsAndHashCode(callSuper = true) // 比较父类 + 当前类所有字段
@Data
public class PictureQueryRequest extends PageRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     * name：精确/模糊匹配 name 字段，用于筛选条件
     */
    private String name;

    /**
     * 简介
     * introduction：精确/模糊匹配 introduction 字段，用于筛选条件
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;

    /**
     * 文件体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 搜索词（同时搜名称、简介等）
     */
    private String searchText;

    /**
     * 用户 id
     */
    private Long userId;

    private static final long serialVersionUID = 1L;
}
