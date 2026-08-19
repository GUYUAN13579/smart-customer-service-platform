package com.example.smartcustomerservice.common.result;

public enum ResultCode implements ErrorCode {

    SUCCESS(0, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "没有操作权限"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不支持"),
    CONFLICT(409, "资源状态冲突"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),
    INTERNAL_SERVER_ERROR(500, "系统繁忙，请稍后重试"),

    USERNAME_OR_PASSWORD_ERROR(10001, "用户名或密码错误"),
    USER_DISABLED(10002, "用户已被禁用"),
    USERNAME_ALREADY_EXISTS(10003, "用户名已存在"),
    TOKEN_INVALID(10004, "Token 无效"),
    TOKEN_EXPIRED(10005, "Token 已过期"),

    TICKET_STATUS_INVALID(20001, "工单状态不允许当前操作"),
    TICKET_NOT_FOUND(20002, "工单不存在"),
    IDEMPOTENT_KEY_REQUIRED(20003, "缺少幂等请求头"),
    DUPLICATE_SUBMIT(20004, "请勿重复提交"),

    AI_SERVICE_UNAVAILABLE(30001, "AI 服务暂时不可用"),
    KNOWLEDGE_NOT_FOUND(30002, "未找到匹配知识"),

    CUSTOMER_NOT_FOUND(40001, "客户不存在"),

    SESSION_CLOSED(50001, "会话关闭"),
    SESSION_NOT_EXIST(50002, "会话不存在"),

    FILE_NOT_EXIST(60001, "文件不存在"),
    NOT_IMAGES(60002, "上传类型不是image")
    ;


    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
