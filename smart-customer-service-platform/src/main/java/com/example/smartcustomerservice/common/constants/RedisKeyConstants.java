package com.example.smartcustomerservice.common.constants;

public final class RedisKeyConstants {

    public static final String PREFIX = "cs:";
    public static final String SESSION_CONTEXT = PREFIX + "session:%s";
    public static final String TICKET_IDEMPOTENT = PREFIX + "ticket:idempotent:%s";
    public static final String TICKET_LOCK = PREFIX + "ticket:lock:%s";
    public static final String HOT_KNOWLEDGE = PREFIX + "faq:hot:%s";
    public static final String RATE_LIMIT = PREFIX + "rate:%s";
    public static final String LOGIN_TOKEN = PREFIX + "login:token:%s";
    public static final String LOGIN_REFRESH_TOKEN = PREFIX + "login:refresh:%s";
    public static final String USER_PERMISSION = PREFIX + "user:permission:%s";

    private RedisKeyConstants() {
    }

    public static String format(String pattern, Object... args) {
        return String.format(pattern, args);
    }
}
