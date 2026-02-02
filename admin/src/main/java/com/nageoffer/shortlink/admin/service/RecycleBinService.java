package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecyclePageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService {

    /**
     * 回收站短链接分页查询
     * @param shortLinkRecyclePageReqDTO
     * @return
     */
    Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO);


}
