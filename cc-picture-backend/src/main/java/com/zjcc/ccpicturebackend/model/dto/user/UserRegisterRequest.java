package com.zjcc.ccpicturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 请求封装类
 * 便于对请求参数统一验证和扩展
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
