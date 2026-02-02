package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecyclePageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;

public interface RecycleBinService extends IService<ShortLinkDO> {

    /**
     * 将短链接移至回收站
     */
    Void saveRecycleBin(RecycleBinSaveReqDTO recycleBinSaveReqDTO);


    /**
     *分页查询回收站短链接
     */
    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO);

    /**
     * 回收站短链接恢复
     */
    void recoverRecycleBin(RecycleBinRecoverReqDTO recycleBinRecoverReqDTO);
}
