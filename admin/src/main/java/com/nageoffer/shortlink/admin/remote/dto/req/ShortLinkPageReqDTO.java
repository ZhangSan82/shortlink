package com.nageoffer.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 短链接分页接收
 */
@Data
public class ShortLinkPageReqDTO extends Page {

    private String gid;


    /**
     * 排序标识
     */
    private String orderTag;



}
