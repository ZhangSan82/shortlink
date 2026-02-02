package com.nageoffer.shortlink.admin.common.constant;

/**
 * Redis缓存常量类
 */
public class RedisCacheConstant {

    /**
     * 用户注册分布式锁
     */
    public static final String LOCK_USER_REGISER_KEY = "short-link:lock_user_regiser:";


    /**
     * 分组创建分布式锁
     */
    public static final String LOCK_GROUP_CREATE_KEY = "short-link:lock_group-create:%s";
}
