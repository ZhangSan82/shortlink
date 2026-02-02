package com.nageoffer.shortlink.admin.dto.resp;

import lombok.Data;

/**
 * 返回user
 */
@Data
public class UserActualRespDTO {
    /**
     * ID
     */

    private Long id;

    /**
     * 用户名
     */
    private String username;


    private String realName;


    /**
     * 手机号
     */

    private String phone;

    /**
     * 邮箱
     */
    private String mail;


}
