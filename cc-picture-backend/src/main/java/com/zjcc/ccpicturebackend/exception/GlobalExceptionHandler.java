package com.zjcc.ccpicturebackend.exception;

import com.zjcc.ccpicturebackend.common.BaseResponse;
import com.zjcc.ccpicturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody
 * @ControllerAdvice — 标记这是一个全局异常处理器(否则 每个接口都要单独写异常处理逻辑)
 * @ResponseBody — 把返回值直接序列化为 JSON 返回给前端（而不是跳转页面）
 */
@RestControllerAdvice // 全局捕获异常，统一返回格式
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error("BusinessException", e);
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error("RuntimeException", e);
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR, "系统错误");
    }
}


