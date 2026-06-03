package com.zjcc.ccpicturebackend.common;

import lombok.Data;

/**
 * 通用 分页请求包装类
 */
@Data
public class PageRequest {

    /**
     * 当前页号
     */
    private int current = 1;

    /**
     * 页面大小
     */
    private int pageSize = 10;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序顺序（默认降序）
     */
    private String sortOrder = "descend";

    public int getCurrent() {
        return Math.max(1, current);
    }

    public int getPageSize() {
        return Math.min(Math.max(1, pageSize), 100);  // 限制最大 100
    }

}
