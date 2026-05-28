package com.zjcc.ccpicturebackend.constant;

public interface UserConstant {

    /**
     * 用户登录态 session key
     */
    String USER_LOGIN_STATE = "user_login";

    /**
     * 管理员创建的 新用户 默认密码
     */
    String DEFAULT_PASSWORD = "123456";

    //  region 权限
    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    // endregion
}
