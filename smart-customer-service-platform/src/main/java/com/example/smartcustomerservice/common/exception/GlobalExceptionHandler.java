package com.example.smartcustomerservice.common.exception;

import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.common.result.FieldErrorItem;
import com.example.smartcustomerservice.common.result.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ApiResult<Void> handleBusinessException(BusinessException exception) {
        return ApiResult.fail(exception.getCode(), exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResult<List<FieldErrorItem>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiResult<List<FieldErrorItem>> result = ApiResult.fail(ResultCode.BAD_REQUEST, "请求参数校验失败");
        result.setData(errors);
        return result;
    }

    @ExceptionHandler(BindException.class)
    public ApiResult<List<FieldErrorItem>> handleBindException(BindException exception) {
        List<FieldErrorItem> errors = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorItem(error.getField(), error.getDefaultMessage()))
                .toList();
        ApiResult<List<FieldErrorItem>> result = ApiResult.fail(ResultCode.BAD_REQUEST, "请求参数绑定失败");
        result.setData(errors);
        return result;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResult<Void> handleConstraintViolationException(ConstraintViolationException exception) {
        return ApiResult.fail(ResultCode.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ApiResult<Void> handleMissingServletRequestParameterException(MissingServletRequestParameterException exception) {
        return ApiResult.fail(ResultCode.BAD_REQUEST, "缺少必要参数：" + exception.getParameterName());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ApiResult<Void> handleAuthenticationException(AuthenticationException exception) {
        return ApiResult.fail(ResultCode.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ApiResult<Void> handleAccessDeniedException(AccessDeniedException exception) {
        return ApiResult.fail(ResultCode.FORBIDDEN);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ApiResult<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException exception) {
        return ApiResult.fail(ResultCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    public ApiResult<Void> handleException(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception, path={}", request.getRequestURI(), exception);
        return ApiResult.fail(ResultCode.INTERNAL_SERVER_ERROR);
    }
}
