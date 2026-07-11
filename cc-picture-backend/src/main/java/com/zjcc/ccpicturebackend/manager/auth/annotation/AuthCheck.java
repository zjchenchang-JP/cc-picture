package com.zjcc.ccpicturebackend.manager.auth.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Spring AOP + 自定义注解 实现权限校验
 * @author zjchenchang
 * @createDate 2026/5/27 21:32
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuthCheck {
    /**
     * 注解属性
     * 用户必须有某种角色
     */
    String mustRole() default "";
}
