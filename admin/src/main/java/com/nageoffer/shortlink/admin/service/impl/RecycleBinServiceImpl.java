package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.req.ShortLinkRecyclePageReqDTO;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.service.RecycleBinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecycleBinServiceImpl implements RecycleBinService {

    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};

    @Autowired
    private GroupService groupService;


    /***
     *回收站短链接分页查询
     * @param shortLinkRecyclePageReqDTO
     * @return
     */
    @Override
    public Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO) {

        List<GroupDO> groupDOList = groupService.lambdaQuery()
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .list();

        if(CollUtil.isEmpty(groupDOList)){
            throw new ServiceException("用户无分组信息");
        }

        shortLinkRecyclePageReqDTO.setGidList(groupDOList.stream().map(GroupDO::getGid).toList());

        return shortLinkRemoteService.pageRecycleShortLink(shortLinkRecyclePageReqDTO);
    }
}
