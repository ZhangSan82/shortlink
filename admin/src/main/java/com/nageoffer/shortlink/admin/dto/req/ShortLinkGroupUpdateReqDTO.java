package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/**
 * 短链接分组接收
 */
@Data
public class ShortLinkGroupUpdateReqDTO {

    /**
     * 分组标识
     */
    private String gid;
    private String name;
}
