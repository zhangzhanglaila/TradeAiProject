package com.trade.common;

import lombok.Getter;

/**
 * 业务异常：用于抛出可预期的错误（如账号/密码错误、验证码错误等），
 * 由 GlobalExceptionHandler 捕获并返回明确的 code/message。
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }
}
