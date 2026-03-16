package com.trade.common;

import lombok.Getter;

/**
 * 业务错误码约定：
 * - 200：成功
 * - 500：系统异常（不可预期）
 * - 40xxx/41xxx：可预期业务异常
 */
@Getter
public enum ErrorCode {

    AUTH_INVALID_CREDENTIALS(40101, "账号或密码错误"),
    AUTH_INVALID_CAPTCHA(40102, "验证码错误"),
    AUTH_CAPTCHA_REQUIRED(40103, "需要验证码"),
    AUTH_EMAIL_EXISTS(40104, "邮箱已注册"),

    PARAM_INVALID(40001, "参数错误"),

    KG_PARAM_INVALID(42001, "知识图谱参数错误"),
    KG_AI_DISABLED(42002, "AI 功能未启用"),
    KG_AI_PARSE_FAILED(42003, "AI 输出解析失败"),
    KG_FORBIDDEN(42004, "无权访问该图谱"),
    KG_NOT_FOUND(42005, "图谱历史不存在"),
    KG_NEO4J_FAILED(42006, "图谱存储/查询失败"),
    KG_BUILDING(42007, "图谱构建中"),
    KG_BUILD_FAILED(42008, "图谱构建失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
