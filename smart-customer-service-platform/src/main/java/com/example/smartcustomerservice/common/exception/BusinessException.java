package com.example.smartcustomerservice.common.exception;

import com.example.smartcustomerservice.common.result.ErrorCode;
import com.example.smartcustomerservice.common.result.ResultCode;

// BusinessException 属于智能客服平台基础代码。
public class BusinessException extends RuntimeException {

    private final Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = ResultCode.BAD_REQUEST.getCode();
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public Integer getCode() {
        return code;
    }
}
