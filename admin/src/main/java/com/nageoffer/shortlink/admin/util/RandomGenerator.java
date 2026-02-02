package com.nageoffer.shortlink.admin.util;

import java.security.SecureRandom;

/**
 * 分组id随机生成器
 */
public final class RandomGenerator {

    // 定义包含数字和字母的字符池
    private static final String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    public static String generateRandomStrin() {
        // 调用函数生成6位随机码
       return generateRandomString(6);
    }

    /**
     * 生成指定长度的随机字符串（包含数字和字母）
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            // 从字符池中随机获取一个索引
            int randomIndex = random.nextInt(CHARACTERS.length());
            // 将该字符追加到 StringBuilder
            sb.append(CHARACTERS.charAt(randomIndex));
        }
        return sb.toString();
    }
}

