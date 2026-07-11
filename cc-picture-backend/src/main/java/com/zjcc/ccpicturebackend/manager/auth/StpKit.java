package com.zjcc.ccpicturebackend.manager.auth;

import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.stereotype.Component;

/**
 * StpLogic 门面类，管理项目中所有的 StpLogic 账号体系 loginType
 * 添加 @Component 注解的目的是确保静态属性 DEFAULT 和 SPACE 被初始化
 * 定义空间账号体系
 * 使用 Sa-Token 提供的 多账号认证特性，可以将多套账号的授权给区分开，让它们互不干扰
 * 原有的 user表 权限校验依然使用自定义注解 + AOP 的方式实现。而团队空间权限校验，采用 Sa-Token 来管理
 */
@Component
public class StpKit {

    public static final String SPACE_TYPE = "space";

    /**
     * 默认原生会话对象，项目中目前没使用到
     */
    public static final StpLogic DEFAULT = StpUtil.stpLogic;

    /**
     * Space 会话对象，管理 Space 表所有账号的登录、权限认证
     */
    public static final StpLogic SPACE = new StpLogic(SPACE_TYPE);
}
