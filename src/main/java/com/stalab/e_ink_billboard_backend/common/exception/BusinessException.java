package com.stalab.e_ink_billboard_backend.common.exception;

import lombok.Getter;

/**
 * 自定义业务异常
 * 用法: throw new BusinessException("验证码错误");
 */
@Getter
public class BusinessException extends RuntimeException {
    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500; // 默认错误码
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
