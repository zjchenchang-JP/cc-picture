package com.zjcc.ccpicturebackend.aop;

import com.zjcc.ccpicturebackend.annotation.AuthCheck;
import com.zjcc.ccpicturebackend.exception.BusinessException;
import com.zjcc.ccpicturebackend.exception.ErrorCode;
import com.zjcc.ccpicturebackend.model.entity.User;
import com.zjcc.ccpicturebackend.model.enums.UserRoleEnum;
import com.zjcc.ccpicturebackend.service.UserService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * 切面类
 *
 * @Around ：环绕通知可以在目标方法执行前后执行一些逻辑。
 * @Before：在目标方法执行前执行的通知
 * @After：在目标方法执行后执行的通知
 * @AfterReturning：在目标方法执行后，返回结果后执行的通知
 * @AfterThrowing：在目标方法执行后，抛出异常后执行的通知
 */
@Aspect
@Component
public class AuthInterceptor {

    @Resource
    private UserService userService;

    /**
     * 执行拦截
     * @param joinPoint 切入点
     * @param authCheck 权限校验注解
     */
    @Around("@annotation(authCheck)")
    public Object doInterceptor(ProceedingJoinPoint joinPoint, AuthCheck authCheck) throws Throwable {
        String mustRole = authCheck.mustRole();
        // 基于 ThreadLocal 实现
        // // 获取当前请求
        // HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        // // 获取当前会话
        // HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 隐含条件 ： 添加了该注解的方法，都要校验登录；必须登录 （否则getLoginUser方法抛异常）
        User loginUser = userService.getLoginUser(request);
        UserRoleEnum mustRoleEnum = UserRoleEnum.getEnumByValue(mustRole);
        if (null == mustRoleEnum) {
            // 无需角色校验 直接放行
            joinPoint.proceed();
        }
        // 必须有权限
        // 判断当前用户权限 和 添加该注解的方法所要求权限是否匹配
        String userRole = loginUser.getUserRole();
        UserRoleEnum userRoleEnum = UserRoleEnum.getEnumByValue(userRole);
        if (null == userRoleEnum) {
            // 无权限 拒绝
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 要求必须有权限且是管理员权限
        if (UserRoleEnum.ADMIN.equals(mustRoleEnum) && !UserRoleEnum.ADMIN.equals(userRoleEnum)) {
            // 无管理员权限 拒绝
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 通过权限校验
        return joinPoint.proceed();
    }

}
