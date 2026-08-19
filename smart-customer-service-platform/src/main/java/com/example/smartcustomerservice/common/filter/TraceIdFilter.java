package com.example.smartcustomerservice.common.filter;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.util.TraceIdUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TraceIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = request.getHeader(CommonConstants.TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = TraceIdUtil.newTraceId();
        }

        MDC.put(CommonConstants.TRACE_ID, traceId);
        response.setHeader(CommonConstants.TRACE_ID, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(CommonConstants.TRACE_ID);
        }
    }
}
