package com.zjcc.ccpicturebackend.model.vo.space.analyze;

import lombok.Data;

import java.io.Serializable;

/**
 * 空间使用量排行分析 请求封装类
 * 仅管理员可使用，返回值前 N 个空间的信息
 */
@Data
public class SpaceRankAnalyzeRequest implements Serializable {

    /**
     * 排名前 N 的空间
     */
    private Integer topN = 10;

    private static final long serialVersionUID = 1L;
}
