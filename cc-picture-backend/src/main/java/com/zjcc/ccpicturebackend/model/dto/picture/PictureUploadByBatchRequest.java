package com.zjcc.ccpicturebackend.model.dto.picture;

import lombok.Data;

/**
 * 从Bing 批量抓取图片 请求
 * 搜索关键词：便于找到需要的数据
 * 抓取数量：单次要抓取的条数，不建议超过 30 条（接口单次返回的图片有限）
 */
@Data
public class PictureUploadByBatchRequest {  
  
    /**  
     * 搜索词  
     */  
    private String searchText;  
  
    /**  
     * 抓取数量  
     */  
    private Integer count = 10;

    /**
     * 名称前缀
     * 支持抓取和创建图片时批量对某批图片命名，名称前缀默等于搜索关键词
     * 之前我们导入系统的图片名称都是由对方的 URL 决定的，名称可能乱七八糟，而且不利于我们得知数据是在那一批被导入的
     * 管理员在执行任务前指定 名称前缀 (即导入到系统中的图片名称)
     */
    private String namePrefix;

}
