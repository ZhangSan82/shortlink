package com.nageoffer.shortlink.project.common.enums;

import lombok.Getter;

/**
 * 有效期类型
 */
public enum VailDateTypeEnum {

    /**
     * 永久有效期
     */
    PERMANENT(0),

    /**
     * 自定义有效期
     */
    CUSTOM(1);

    @Getter
    private final int type;

    VailDateTypeEnum(int type) {
        this.type = type;
    }

}
