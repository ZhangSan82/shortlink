package com.nageoffer.shortlink.project.dto.req;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import lombok.Data;

import java.util.List;

/**
 * 回收站短链接分页接收
 */
@Data
public class ShortLinkRecyclePageReqDTO extends Page<ShortLinkDO> {

    private List<String> gidList;



}
