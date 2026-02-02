package com.nageoffer.shortlink.project.service.impl;


import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkDO;
import com.nageoffer.shortlink.project.dao.mapper.ShortLinkMapper;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecyclePageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.RecycleBinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;

@Service
public class RecycleBinServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements RecycleBinService {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 将短链接移至回收站
     */
    @Override
    public Void saveRecycleBin(RecycleBinSaveReqDTO recycleBinSaveReqDTO) {
        this.lambdaUpdate()
                .eq(ShortLinkDO::getGid, recycleBinSaveReqDTO.getGid())
                .eq(ShortLinkDO::getFullShortUrl, recycleBinSaveReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag,0)
                .eq(ShortLinkDO::getEnableStatus,0)
                .set(ShortLinkDO::getEnableStatus,1)
                .set(ShortLinkDO::getUpdateTime,new Date())
                .update();

        stringRedisTemplate.delete( String.format(GOTO_SHORT_LINK_KEY,recycleBinSaveReqDTO.getFullShortUrl()));


        return null;
    }


    /**
     *分页查询回收站短链接
     */
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO) {
        LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
                .in(ShortLinkDO::getGid,shortLinkRecyclePageReqDTO.getGidList())
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelFlag,0)
                .orderByDesc(ShortLinkDO::getUpdateTime); // 加上这一行;
        IPage<ShortLinkDO> page = baseMapper.selectPage(shortLinkRecyclePageReqDTO, queryWrapper);
        return page.convert(each-> {
            ShortLinkPageRespDTO result = BeanUtil.copyProperties(each,ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });
    }

    /**
     * 回收站短链接恢复
     */
    @Override
    public void recoverRecycleBin(RecycleBinRecoverReqDTO recycleBinRecoverReqDTO) {
        this.lambdaUpdate()
                .eq(ShortLinkDO::getGid, recycleBinRecoverReqDTO.getGid())
                .eq(ShortLinkDO::getFullShortUrl, recycleBinRecoverReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getDelFlag,0)
                .eq(ShortLinkDO::getEnableStatus,1)
                .eq(ShortLinkDO::getDelTime,0L)
                .set(ShortLinkDO::getEnableStatus,0)
                .set(ShortLinkDO::getDelTime,System.currentTimeMillis())
                .set(ShortLinkDO::getUpdateTime,new Date())
                .update();

        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,recycleBinRecoverReqDTO.getFullShortUrl()));

    }
}
