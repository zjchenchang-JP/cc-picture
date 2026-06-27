package com.zjcc.ccpicturebackend.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 批量编辑图片元数据的结果
 */
@Data
public class BatchEditResult {

    /**
     * 成功更新的条数
     */
    private int successCount;

    /**
     * 失败的条数
     */
    private int failedCount;

    /**
     * 失败的图片 id 列表（前端可拿它调原接口重试）
     */
    private List<Long> failedPictureIds;

    /**
     * 失败原因列表
     */
    private List<String> errors;
}
