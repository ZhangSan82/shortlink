package com.nageoffer.shortlink.project.common.convention.enums;

import com.nageoffer.shortlink.project.common.convention.errorcode.IErrorCode;


/**
 * 用户异常模块
 */
public enum UserErrorCodeEnum implements IErrorCode {


    USER_NULL("B000200","用户信息不存在"),

    USER_NAME_EXIST("B000201","用户名称存在"),

    USER_SAVE_ERROR("B000202","用户添加失败");
    private final String code;

    private final String message;

    UserErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }
}
