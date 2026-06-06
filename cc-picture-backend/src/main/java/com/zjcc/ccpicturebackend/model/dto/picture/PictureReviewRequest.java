package com.zjcc.ccpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员审核功能 - 本质是改变审核状态
 * 不需要增加 reviewerId 和 reviewTime字段，这两个是由系统代码填充的，而不是由前端传递
 * 管理员上传 / 更新图片时，图片自动审核通过，并且自动填充审核参数; 审核原因为 “管理员自动过审”
 */
@Data
public class PictureReviewRequest implements Serializable {
  
    /**  
     * 待审核图片 id
     */  
    private Long id;  
  
    /**  
     * 状态：0-待审核, 1-通过, 2-拒绝  
     */  
    private Integer reviewStatus;  
  
    /**  
     * 审核信息  
     */  
    private String reviewMessage;  
  
  
    private static final long serialVersionUID = 1L;  
}
