package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convention.exception.ClientException;
import com.nageoffer.shortlink.project.common.convention.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.VailDateTypeEnum;
import com.nageoffer.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.*;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.mq.producer.DelayShortLinkStatsProducer;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import com.nageoffer.shortlink.project.toolUtil.HashUtil;
import com.nageoffer.shortlink.project.toolUtil.LinkUtil;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URL;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;
import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortLinkServiceImpl extends ServiceImpl<ShortLinkMapper, ShortLinkDO> implements ShortLinkService {

    private  final RBloomFilter<String> shortLinkRegisterCachePenetrationBloomFilter;
    private final ShortLinkGotoMapper  shortLinkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper  linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final ReactiveHealthContributor reactiveHealthContributor;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final DelayShortLinkStatsProducer delayShortLinkStatsProducer;

    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocaleAmapKey;

    @Value("${short-link.domain.default}")
    private String defaultDomain;

    /**
     * 创建短链接
     */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO shortLinkCreateReqDTO) {
        verificationWhitelist(shortLinkCreateReqDTO.getOriginUrl());
        String shortLinkSuffix = generateSuffix(shortLinkCreateReqDTO);
        String fullShortUrl = StrBuilder.create(defaultDomain)
                .append("/")
                .append(shortLinkSuffix)
                .toString();
        ShortLinkDO shortLinkDO = BeanUtil.copyProperties(shortLinkCreateReqDTO, ShortLinkDO.class);
        shortLinkDO.setFullShortUrl(fullShortUrl);
        shortLinkDO.setDomain(defaultDomain);
        shortLinkDO.setShortUri(shortLinkSuffix);
        shortLinkDO.setEnableStatus(0);
        shortLinkDO.setFavicon(getFavicon(shortLinkCreateReqDTO.getOriginUrl()));
        shortLinkDO.setTotalPv(0);
        shortLinkDO.setDelTime(0L);
        shortLinkDO.setTotalUv(0);
        shortLinkDO.setTotalUip(0);

        ShortLinkGotoDO shortLinkGotoDO = ShortLinkGotoDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(shortLinkCreateReqDTO.getGid()).build();

        log.info("保存前 shortLinkDO: {}", shortLinkDO);
        try {
            this.save(shortLinkDO);
            shortLinkGotoMapper.insert(shortLinkGotoDO);
        } catch (DuplicateKeyException e) {
                log.warn("短链接:{} 重复入库",fullShortUrl);
                throw new ServiceException("短链接生成重复");
        }
        //创建短链接时,短链接预热
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), // 必须加同样的前缀
                shortLinkCreateReqDTO.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(shortLinkCreateReqDTO.getValidDate()),TimeUnit.MILLISECONDS);

        shortLinkRegisterCachePenetrationBloomFilter.add(fullShortUrl);

        ShortLinkCreateRespDTO shortLinkCreateRespDTO = ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(shortLinkDO.getOriginUrl())
                .gid(shortLinkDO.getGid())
                .build();
        log.info("shortLinkCreateRespDTO: {}", shortLinkCreateRespDTO);

        return shortLinkCreateRespDTO;
    }

    /**
     *分页查询短链接
     */
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO) {
       LambdaQueryWrapper<ShortLinkDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkDO.class)
               .eq(ShortLinkDO::getGid,shortLinkPageReqDTO.getGid())
               .eq(ShortLinkDO::getEnableStatus,0)
               .eq(ShortLinkDO::getDelFlag,0)
               .orderByDesc(ShortLinkDO::getCreateTime); // 加上这一行;
       IPage<ShortLinkDO> page = baseMapper.selectPage(shortLinkPageReqDTO, queryWrapper);

       return getShortLinkUvPvUip(page);
//       return page.convert(each-> {
//           ShortLinkPageRespDTO result = BeanUtil.copyProperties(each,ShortLinkPageRespDTO.class);
//           result.setDomain("http://" + result.getDomain());
//           return result;
//       });
    }

    /**
     *查询短链接分组内数量
     */
    @Override
    public List<ShortLinkGroupCountQueryRespDTO> listGroupShortLinkCount(List<String> gidList) {
        QueryWrapper<ShortLinkDO> queryWrapper = Wrappers.query(new ShortLinkDO())
                .select("gid as gid , count(*) as shortLinkCount")
                .in("gid",gidList)
                .eq("del_flag",0)
                .eq("del_time",0L)
                .eq("enable_status",0)
                .groupBy("gid");
        List<Map<String, Object>> mapList = baseMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(mapList,ShortLinkGroupCountQueryRespDTO.class);
    }

    /**
     * 修改短链接
     * @param shortLinkUpdateReqDTO
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO shortLinkUpdateReqDTO) {
        //TODO无法修改gid
        ShortLinkDO shortLinkDO = lambdaQuery()
                .eq(ShortLinkDO::getGid, shortLinkUpdateReqDTO.getOriginGid())
                .eq(ShortLinkDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                .eq(ShortLinkDO::getEnableStatus, 0)
                .eq(ShortLinkDO::getDelFlag, 0)
                .one();

        if (shortLinkDO == null) {
            throw new ClientException("短链接不存在");
        }


        if (Objects.equals(shortLinkDO.getGid(), shortLinkUpdateReqDTO.getGid())) {
            LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                    .eq(ShortLinkDO::getGid, shortLinkUpdateReqDTO.getGid())
                    .eq(ShortLinkDO::getEnableStatus, 0)
                    .eq(ShortLinkDO::getDelFlag, 0)
                    .eq(ShortLinkDO::getFullShortUrl, shortLinkDO.getFullShortUrl())
                    .set(Objects.equals(shortLinkUpdateReqDTO.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()), ShortLinkDO::getValidDate, null);

            ShortLinkDO shortLinkDO1 = ShortLinkDO.builder()
                    .domain(shortLinkDO.getDomain())
                    .favicon(shortLinkDO.getFavicon())
                    .shortUri(shortLinkDO.getShortUri())
                    .clickNum(shortLinkDO.getClickNum())
                    .createdType(shortLinkDO.getCreatedType())
                    .gid(shortLinkUpdateReqDTO.getGid())
                    .originUrl(shortLinkUpdateReqDTO.getOriginUrl())
                    .describe(shortLinkUpdateReqDTO.getDescribe())
                    .validDateType(shortLinkUpdateReqDTO.getValidDateType())
                    .validDate(shortLinkUpdateReqDTO.getValidDate())
                    .updateTime(new Date())
                    .build();

            baseMapper.update(shortLinkDO1, updateWrapper);
        } else {
//                LambdaUpdateWrapper<ShortLinkDO> updateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
//                        .eq(ShortLinkDO::getGid,shortLinkUpdateReqDTO.getGid())
//                        .eq(ShortLinkDO::getEnableStatus,0)
//                        .eq(ShortLinkDO::getDelFlag,0)
//                        .eq(ShortLinkDO::getFullShortUrl,shortLinkDO.getFullShortUrl());
//                baseMapper.delete(updateWrapper);
//                baseMapper.insert(shortLinkDO1);
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, shortLinkUpdateReqDTO.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            if (!rLock.tryLock()) {
                throw new ServiceException("短链接正在被访问，请稍后再试...");
            }
            try {
                LambdaUpdateWrapper<ShortLinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(ShortLinkDO.class)
                        .eq(ShortLinkDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(ShortLinkDO::getGid, shortLinkDO.getGid())
                        .eq(ShortLinkDO::getDelFlag, 0)
                        .eq(ShortLinkDO::getDelTime, 0L)
                        .eq(ShortLinkDO::getEnableStatus, 0);
                ShortLinkDO delShortLinkDO = ShortLinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .updateTime(new Date())
                        .build();
                delShortLinkDO.setDelFlag(1);
                baseMapper.update(delShortLinkDO, linkUpdateWrapper);
                ShortLinkDO shortLinkDO1 = ShortLinkDO.builder()
                        .domain(defaultDomain)
                        .originUrl(shortLinkUpdateReqDTO.getOriginUrl())
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .createdType(shortLinkDO.getCreatedType())
                        .validDateType(shortLinkUpdateReqDTO.getValidDateType())
                        .validDate(shortLinkUpdateReqDTO.getValidDate())
                        .describe(shortLinkUpdateReqDTO.getDescribe())
                        .shortUri(shortLinkDO.getShortUri())
                        .enableStatus(shortLinkDO.getEnableStatus())
                        .totalPv(shortLinkDO.getTotalPv())
                        .totalUv(shortLinkDO.getTotalUv())
                        .totalUip(shortLinkDO.getTotalUip())
                        .fullShortUrl(shortLinkDO.getFullShortUrl())
                        .favicon(getFavicon(shortLinkUpdateReqDTO.getOriginUrl()))
                        .delTime(0L)
                        .build();
                baseMapper.insert(shortLinkDO1);
//                LambdaQueryWrapper<LinkStatsTodayDO> statsTodayQueryWrapper = Wrappers.lambdaQuery(LinkStatsTodayDO.class)
//                        .eq(LinkStatsTodayDO::getFullShortUrl, shortLinkDO.getFullShortUrl())
//                        .eq(LinkStatsTodayDO::getGid, shortLinkDO.getGid())
//                        .eq(LinkStatsTodayDO::getDelFlag, 0);
//                List<LinkStatsTodayDO> linkStatsTodayDOList = linkStatsTodayMapper.selectList(statsTodayQueryWrapper);
//                if (CollUtil.isNotEmpty(linkStatsTodayDOList)) {
//                    linkStatsTodayMapper.deleteBatchIds(linkStatsTodayDOList.stream()
//                            .map(LinkStatsTodayDO::getId)
//                            .toList()
//                    );
//                    linkStatsTodayDOList.forEach(each -> each.setGid(shortLinkUpdateReqDTO.getGid()));
//                    linkStatsTodayService.saveBatch(linkStatsTodayDOList);
//                }

                //只有一张表,没有必要删添,直接修改
//                LambdaQueryWrapper<LinkAccessStatsDO> linkAccessStatsDOLambdaQueryWrapper = Wrappers.lambdaQuery(LinkAccessStatsDO.class)
//                        .eq(LinkAccessStatsDO::getGid, shortLinkDO.getGid())
//                        .eq(LinkAccessStatsDO::getFullShortUrl, shortLinkDO.getFullShortUrl())
//                        .eq(LinkAccessStatsDO::getDelFlag, 0);
//                List<LinkAccessStatsDO> linkAccessStatsDOList = linkAccessStatsMapper.selectList(linkAccessStatsDOLambdaQueryWrapper);
//                if(CollectionUtil.isNotEmpty(linkAccessStatsDOList)) {
//                    linkAccessStatsMapper.deleteBatchIds(linkAccessStatsDOList.stream()
//                            .map(LinkAccessStatsDO::getId)
//                            .toList());
//                }
//                linkAccessStatsDOList.forEach(each-> each.setGid(shortLinkUpdateReqDTO.getGid()));
//                linkAccessStatsImpl.saveBatch(linkAccessStatsDOList);

                LambdaUpdateWrapper<LinkAccessStatsDO> linkAccessStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessStatsDO.class)
                        .eq(LinkAccessStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkAccessStatsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkAccessStatsDO::getDelFlag, 0);
                LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkAccessStatsMapper.update(linkAccessStatsDO, linkAccessStatsUpdateWrapper);

                LambdaQueryWrapper<ShortLinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(ShortLinkGotoDO::getGid, shortLinkDO.getGid());
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(linkGotoQueryWrapper);
                shortLinkGotoMapper.deleteById(shortLinkGotoDO.getId());

                shortLinkGotoDO.setGid(shortLinkUpdateReqDTO.getGid());
                shortLinkGotoMapper.insert(shortLinkGotoDO);



                LambdaUpdateWrapper<LinkLocaleStatsDO> linkLocaleStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkLocaleStatsDO.class)
                        .eq(LinkLocaleStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkLocaleStatsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkLocaleStatsDO::getDelFlag, 0);
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkLocaleStatsMapper.update(linkLocaleStatsDO, linkLocaleStatsUpdateWrapper);


                LambdaUpdateWrapper<LinkOsStatsDO> linkOsStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkOsStatsDO.class)
                        .eq(LinkOsStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkOsStatsDO::getGid, shortLinkUpdateReqDTO.getGid())
                        .eq(LinkOsStatsDO::getDelFlag, 0);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkOsStatsMapper.update(linkOsStatsDO, linkOsStatsUpdateWrapper);



                LambdaUpdateWrapper<LinkBrowserStatsDO> linkBrowserStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkBrowserStatsDO.class)
                        .eq(LinkBrowserStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkBrowserStatsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkBrowserStatsDO::getDelFlag, 0);
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkBrowserStatsMapper.update(linkBrowserStatsDO, linkBrowserStatsUpdateWrapper);


                LambdaUpdateWrapper<LinkDeviceStatsDO> linkDeviceStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkDeviceStatsDO.class)
                        .eq(LinkDeviceStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkDeviceStatsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkDeviceStatsDO::getDelFlag, 0);
                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkDeviceStatsMapper.update(linkDeviceStatsDO, linkDeviceStatsUpdateWrapper);


                LambdaUpdateWrapper<LinkNetworkStatsDO> linkNetworkStatsUpdateWrapper = Wrappers.lambdaUpdate(LinkNetworkStatsDO.class)
                        .eq(LinkNetworkStatsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkNetworkStatsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkNetworkStatsDO::getDelFlag, 0);
                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkNetworkStatsMapper.update(linkNetworkStatsDO, linkNetworkStatsUpdateWrapper);


                LambdaUpdateWrapper<LinkAccessLogsDO> linkAccessLogsUpdateWrapper = Wrappers.lambdaUpdate(LinkAccessLogsDO.class)
                        .eq(LinkAccessLogsDO::getFullShortUrl, shortLinkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkAccessLogsDO::getGid, shortLinkDO.getGid())
                        .eq(LinkAccessLogsDO::getDelFlag, 0);
                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .gid(shortLinkUpdateReqDTO.getGid())
                        .build();
                linkAccessLogsMapper.update(linkAccessLogsDO, linkAccessLogsUpdateWrapper);
            } finally {
                rLock.unlock();
            }

                if (!Objects.equals(shortLinkDO.getValidDateType(), shortLinkUpdateReqDTO.getValidDateType())
                        || !Objects.equals(shortLinkDO.getValidDate(), shortLinkUpdateReqDTO.getValidDate())) {
                    stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, shortLinkUpdateReqDTO.getFullShortUrl()));
                    if (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())) {
                        if (Objects.equals(shortLinkUpdateReqDTO.getValidDateType(), VailDateTypeEnum.PERMANENT.getType()) || shortLinkUpdateReqDTO.getValidDate().after(new Date())) {
                            stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, shortLinkUpdateReqDTO.getFullShortUrl()));
                        }
                    }
                }
            }



    }

    /**
     * 短链接跳转
     * @param shorturl
     * @param request
     * @param response
     */
    @SneakyThrows
    @Override
    public void restoreUrl(String shorturl, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each-> !Objects.equals(each,80))
                .map(String::valueOf)
                .map(each->":"+each)
                .orElse("");
        String fullShortUrl = serverName+ serverPort+ "/"+shorturl;
        //首先判断缓存中有无
        String originalLink  = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl));
        if(StrUtil.isNotBlank(originalLink)){
            //shortLinkStats(fullShortUrl,null,request,response);
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, null, statsRecord);
            ((HttpServletResponse)response).sendRedirect(originalLink);
            return;

        }
        //判断过滤器中有无
        boolean contains = shortLinkRegisterCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }

        //判断是否在黑名单中
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse)response).sendRedirect("/page/notfound");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY,fullShortUrl));
        lock.lock();

        try {
            originalLink  = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY,fullShortUrl));
            if(StrUtil.isNotBlank(originalLink)){
                //shortLinkStats(fullShortUrl,null,request,response);
                ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
                shortLinkStats(fullShortUrl, null, statsRecord);
                ((HttpServletResponse)response).sendRedirect(originalLink);
                return;

            }
            LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                    .eq(ShortLinkGotoDO::getFullShortUrl,fullShortUrl);
            ShortLinkGotoDO  shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
            if(shortLinkGotoDO == null){
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl),"-",30, TimeUnit.MINUTES);
                //此处需要进行风控
                ((HttpServletResponse)response).sendRedirect("/page/notfound");
                return;
            }
            ShortLinkDO shortLinkDO = lambdaQuery()
                    .eq(ShortLinkDO::getGid,shortLinkGotoDO.getGid())
                    .eq(ShortLinkDO::getFullShortUrl,fullShortUrl)
                    .eq(ShortLinkDO::getEnableStatus,0)
                    .eq(ShortLinkDO::getDelFlag,0)
                    .one();


            if(shortLinkDO == null|| (shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())))
            {
                //有效期失效
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY,fullShortUrl),"-",30, TimeUnit.MINUTES);
                ((HttpServletResponse)response).sendRedirect("/page/notfound");
                return;
            }
                stringRedisTemplate.opsForValue().set(
                        String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), // 必须加同样的前缀
                        shortLinkDO.getOriginUrl(),
                        LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()),TimeUnit.MILLISECONDS);

            //shortLinkStats(fullShortUrl,shortLinkDO.getGid(),request,response);
            ShortLinkStatsRecordDTO statsRecord = buildLinkStatsRecordAndSetUser(fullShortUrl, request, response);
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), statsRecord);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }

    }


    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    @Override
    public void shortLinkStats(String fullShortUrl, String gid, ShortLinkStatsRecordDTO statsRecord) {
        fullShortUrl = Optional.ofNullable(fullShortUrl).orElse(statsRecord.getFullShortUrl());
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        if (!rLock.tryLock()) {
            delayShortLinkStatsProducer.send(statsRecord);
            return;
        }
        try {
            if (StrUtil.isBlank(gid)) {
                LambdaQueryWrapper<ShortLinkGotoDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
                        .eq(ShortLinkGotoDO::getFullShortUrl, fullShortUrl);
                ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
                gid = shortLinkGotoDO.getGid();
            }
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int weekValue = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .hour(hour)
                    .weekday(weekValue)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String, Object> localeParamMap = new HashMap<>();
            localeParamMap.put("key", statsLocaleAmapKey);
            localeParamMap.put("ip", statsRecord.getRemoteAddr());
            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL, localeParamMap);
            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
            String infoCode = localeResultObj.getString("infocode");
            String actualProvince = "未知";
            String actualCity = "未知";
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")) {
                String province = localeResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                LinkLocaleStatsDO linkLocaleStatsDO = LinkLocaleStatsDO.builder()
                        .province(actualProvince = unknownFlag ? actualProvince : province)
                        .city(actualCity = unknownFlag ? actualCity : localeResultObj.getString("city"))
                        .adcode(unknownFlag ? "未知" : localeResultObj.getString("adcode"))
                        .cnt(1)
                        .fullShortUrl(fullShortUrl)
                        .country("中国")
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocaleStatsMapper.shortLinklocaleStats(linkLocaleStatsDO);
            }
            LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                    .os(statsRecord.getOs())
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);

            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                    .browser(statsRecord.getBrowser())
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
                    .device(statsRecord.getDevice())
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
                    .network(statsRecord.getNetwork())
                    .cnt(1)
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .date(new Date())
                    .build();
            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
            LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                    .user(statsRecord.getUv())
                    .ip(statsRecord.getRemoteAddr())
                    .browser(statsRecord.getBrowser())
                    .os(statsRecord.getOs())
                    .network(statsRecord.getNetwork())
                    .device(statsRecord.getDevice())
                    .locale(StrUtil.join("-", "中国", actualProvince, actualCity))
                    .gid(gid)
                    .fullShortUrl(fullShortUrl)
                    .build();
            linkAccessLogsMapper.insert(linkAccessLogsDO);
            baseMapper.incrementStats(gid, fullShortUrl, 1, statsRecord.getUvFirstFlag() ? 1 : 0, statsRecord.getUipFirstFlag() ? 1 : 0);

        } catch (Throwable ex) {
            log.error("短链接访问量统计异常", ex);
        } finally {
            rLock.unlock();
        }
    }





    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.randomUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getRealIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .build();

    }



    private String generateSuffix(ShortLinkCreateReqDTO shortLinkCreateReqDTO) {
        int customGenerateCount = 0;
        String shortUrl;
        while(true){
            if(customGenerateCount >= 10){
                throw new ServiceException("短链接频繁生成,请稍后再试");
            }
            String originUrl = shortLinkCreateReqDTO.getOriginUrl();
            originUrl += UUID.randomUUID().toString();
            shortUrl =  HashUtil.hashToBase62(originUrl);
            //if (!shortLinkRegisterCachePenetrationBloomFilter.contains(shortLinkCreateReqDTO.getDomain() +"/" +shortUrl))
                if (!shortLinkRegisterCachePenetrationBloomFilter.contains(defaultDomain + "/" + shortUrl))
            {
                break;
            }
            customGenerateCount++;
        }
        return  shortUrl;
    }

//    private void shortLinkStats(String fullShortUrl,String gid,ServletRequest request, ServletResponse response) {
//        AtomicBoolean uvFirstFlag = new AtomicBoolean();
//        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
//        try {
//            AtomicReference<String> uv =  new AtomicReference<>();
//            Runnable addResponseCookies = () -> {
//                uv.set(UUID.randomUUID().toString());
//                Cookie uvCookie = new Cookie("uv",uv.get());
//                uvCookie.setMaxAge(60*60*24*30);
//                uvCookie.setPath(StrUtil.sub(fullShortUrl,fullShortUrl.indexOf("/"),fullShortUrl.length()));
//                ((HttpServletResponse)response).addCookie(uvCookie);
//                uvFirstFlag.set(Boolean.TRUE);
//                stringRedisTemplate.opsForSet().add("short-link:stats:uv:"+fullShortUrl,uv.get());
//            };
//            if (ArrayUtil.isNotEmpty(cookies)) {
//                Arrays.stream(cookies)
//                        .filter(each->Objects.equals(each.getName(),"uv"))
//                        .findFirst()
//                        .map(Cookie::getValue)
//                        .ifPresentOrElse(each->{
//                            uv.set(each);
//                            Long uvadded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:"+fullShortUrl,each);
//                            uvFirstFlag.set(uvadded != null && uvadded > 0L);
//                        },addResponseCookies);
//            }
//            else{
//                addResponseCookies.run();
//            }
//            String remoteAddr = LinkUtil.getRealIp ((HttpServletRequest) request);
//            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:"+fullShortUrl,remoteAddr);
//            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
//
//            if (StrUtil.isBlank(gid)) {
//                LambdaQueryWrapper<ShortLinkGotoDO>  queryWrapper = Wrappers.lambdaQuery(ShortLinkGotoDO.class)
//                        .eq(ShortLinkGotoDO::getFullShortUrl,fullShortUrl);
//
//              ShortLinkGotoDO shortLinkGotoDO = shortLinkGotoMapper.selectOne(queryWrapper);
//              gid = shortLinkGotoDO.getGid();
//            }
//            int hourOfDay = LocalTime.now().getHour();
//            int dayOfWeek = LocalDate.now().getDayOfWeek().getValue();
//            LinkAccessStatsDO linkAccessStats = LinkAccessStatsDO.builder()
//                    .pv(1)
//                    .uv(uvFirstFlag.get()?1:0)
//                    .uip(uipFirstFlag?1:0)
//                    .hour(hourOfDay)
//                    .weekday(dayOfWeek)
//                    .fullShortUrl(fullShortUrl)
//                    .gid(gid)
//                    .date(new Date())
//                    .build();
//            linkAccessStatsMapper.shortLinkStats(linkAccessStats);
//
//
//            Map<String,Object> localeParamMap = new HashMap<>();
//            localeParamMap.put("key",statsLocaleAmapKey);
//            localeParamMap.put("ip",remoteAddr);
//            String localeResultStr = HttpUtil.get(AMAP_REMOTE_URL,localeParamMap);
//            JSONObject localeResultObj = JSON.parseObject(localeResultStr);
//            String infoCode = localeResultObj.getString("infocode");
//            LinkLocaleStatsDO linkLocaleStatsDO;
//            String actualProvince = "";
//            String actualCity = "";
//            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode,"10000")) {
//                String province  = localeResultObj.getString("province");
//                boolean unknownFlag = StrUtil.equals(province,"[]");
//
//
//                linkLocaleStatsDO = LinkLocaleStatsDO.builder()
//                        .province(actualProvince = unknownFlag?"未知":province)
//                        .city(actualCity = unknownFlag?"未知":localeResultObj.getString("city"))
//                        .adcode(unknownFlag?"未知":localeResultObj.getString("adcode"))
//                        .cnt(1)
//                        .fullShortUrl(fullShortUrl)
//                        .gid(gid)
//                        .date(new Date())
//                        .country("中国")
//                        .build();
//                linkLocaleStatsMapper.shortLinklocaleStats(linkLocaleStatsDO);
//            }
//
//            LinkOsStatsDO  linkOsStatsDO = LinkOsStatsDO.builder()
//                    .cnt(1)
//                    .gid(gid)
//                    .date(new Date())
//                    .fullShortUrl(fullShortUrl)
//                    .os(LinkUtil.getOs((HttpServletRequest) request))
//                    .build();
//            linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);
//
//            String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
//            LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
//                    .browser(browser)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkBrowserStatsMapper.shortLinkBrowserState(linkBrowserStatsDO);
//
//
//            String device = LinkUtil.getDevice(((HttpServletRequest) request));
//            LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
//                    .device(device)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
//
//            String network = LinkUtil.getNetwork(((HttpServletRequest) request));
//            LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
//                    .network(network)
//                    .cnt(1)
//                    .gid(gid)
//                    .fullShortUrl(fullShortUrl)
//                    .date(new Date())
//                    .build();
//            linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
//            LinkAccessLogsDO  linkAccessLogsDO =LinkAccessLogsDO.builder()
//                    .browser(browser)
//                    .gid(gid)
//                    .network(network)
//                    .device(device)
//                    .ip(remoteAddr)
//                    .locale(StrUtil.join("-","中国",actualProvince,actualCity))
//                    .os(LinkUtil.getOs((HttpServletRequest) request))
//                    .fullShortUrl(fullShortUrl)
//                    .user(uv.get())
//                    .build();
//
//            linkAccessLogsMapper.insert(linkAccessLogsDO);
//
//            baseMapper.incrementStats(gid,fullShortUrl,1,uvFirstFlag.get()?1:0,uipFirstFlag?1:0);
//
//
//
//        } catch (Exception e) {
//            log.error("短链接访问量统计异常",e);
//        }
//
//
//    }

    private IPage<ShortLinkPageRespDTO> getShortLinkUvPvUip(IPage<ShortLinkDO> page)
    {
        return page.convert(e->{
            LambdaQueryWrapper<LinkAccessStatsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessStatsDO.class)
                    .eq(LinkAccessStatsDO::getFullShortUrl,e.getFullShortUrl())
                    .eq(LinkAccessStatsDO::getGid,e.getGid());
            List<LinkAccessStatsDO> linkAccessStatsDOList = linkAccessStatsMapper.selectList(queryWrapper);
            ShortLinkPageRespDTO shortLinkPageRespDTO = BeanUtil.copyProperties(e,ShortLinkPageRespDTO.class);

            int totalPv = linkAccessStatsDOList.stream().mapToInt(LinkAccessStatsDO::getPv).sum();
            int totalUv = linkAccessStatsDOList.stream().mapToInt(LinkAccessStatsDO::getUv).sum();
            int totalUip = linkAccessStatsDOList.stream().mapToInt(LinkAccessStatsDO::getUip).sum();

            int todayPv = linkAccessStatsDOList.stream()
                    .filter(item->{
                        LocalDate localDate = item.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate today = LocalDate.now();
                        return localDate.isEqual(today);
                    })
                    .mapToInt(LinkAccessStatsDO::getPv).sum();

            int todayUv = linkAccessStatsDOList.stream()
                    .filter(item->{
                        LocalDate localDate = item.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate today = LocalDate.now();
                        return localDate.isEqual(today);
                    })
                    .mapToInt(LinkAccessStatsDO::getUv).sum();

            int todayUip = linkAccessStatsDOList.stream()
                    .filter(item->{
                        LocalDate localDate = item.getDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        LocalDate today = LocalDate.now();
                        return localDate.isEqual(today);
                    })
                    .mapToInt(LinkAccessStatsDO::getUip).sum();

            shortLinkPageRespDTO.setTotalPv(totalPv);
            shortLinkPageRespDTO.setTotalUv(totalUv);
            shortLinkPageRespDTO.setTotalUip(totalUip);
            shortLinkPageRespDTO.setTodayPv(todayPv);
            shortLinkPageRespDTO.setTodayUv(todayUv);
            shortLinkPageRespDTO.setTodayUip(todayUip);
            shortLinkPageRespDTO.setDomain("http://" + shortLinkPageRespDTO.getDomain());
            return shortLinkPageRespDTO;
        });
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }

    private static String getFavicon(String url) {
        // 1. 补全协议
        if (!url.startsWith("http")) {
            url = "http://" + url;
        }

        try {
            // 2. Jsoup 连接
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) ...") // 必须伪装 User-Agent
                    .timeout(3000)
                    .get();

            // 3. 尝试查找 <link> 标签
            // 查找 rel 包含 "icon" 的标签 (涵盖了 icon, shortcut icon, apple-touch-icon)
            Element iconElement = doc.select("head link[rel~=(?i)^(shortcut|icon|apple-touch-icon)$]").first();

            if (iconElement != null) {
                // 关键点：使用 abs:href 获取绝对路径
                // 如果网页里写的是 /img/logo.png，Jsoup 会自动拼接成 http://site.com/img/logo.png
                return iconElement.attr("abs:href");
            }

        } catch (Exception e) {
            // 解析失败，忽略
        }

        // 4. 兜底策略：如果 HTML 里没找到，尝试猜测根目录的 ico
        try {
            URL targetUrl = new URL(url);
            return targetUrl.getProtocol() + "://" + targetUrl.getHost() + "/favicon.ico";
        } catch (Exception e) {
            return null;
        }
    }

}
