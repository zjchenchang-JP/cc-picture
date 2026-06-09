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
}
