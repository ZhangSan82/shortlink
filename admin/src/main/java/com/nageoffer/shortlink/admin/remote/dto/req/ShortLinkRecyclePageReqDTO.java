package com.nageoffer.shortlink.admin.remote.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

/**
 * 回收站短链接分页接收
 */
@Data
public class ShortLinkRecyclePageReqDTO extends Page {

    private List<String> gidList;



}
