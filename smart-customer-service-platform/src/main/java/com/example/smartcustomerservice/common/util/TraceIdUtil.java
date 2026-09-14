package com.example.smartcustomerservice.common.util;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import org.slf4j.MDC;

import java.util.UUID;

// TraceIdUtil 属于智能客服平台基础代码。
public final class TraceIdUtil {

    private TraceIdUtil() {
    }

    public static String getTraceId() {
        String traceId = MDC.get(CommonConstants.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = newTraceId();
            MDC.put(CommonConstants.TRACE_ID, traceId);
        }
        return traceId;
    }

    public static String newTraceId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
