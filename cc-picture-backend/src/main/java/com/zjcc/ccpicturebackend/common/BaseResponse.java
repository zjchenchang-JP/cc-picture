package com.zjcc.ccpicturebackend.common;

import com.zjcc.ccpicturebackend.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 通用返回包装类
 * @param <T> 返回给前端数据类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    private int code;

    private T data;

    private String message;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}


