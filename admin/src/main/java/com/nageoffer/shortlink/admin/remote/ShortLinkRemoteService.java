package com.nageoffer.shortlink.admin.remote;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.remote.dto.req.*;
import com.nageoffer.shortlink.admin.remote.dto.resp.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ShortLinkRemoteService {



    /**
     * 创建短链接
     */
    default ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO shortLinkCreateReqDTO){
        String resultBodyStr = HttpUtil.post("http://127.0.0.1:8001/api/shortlink/v1/create",JSON.toJSONString(shortLinkCreateReqDTO));
        return JSON.parseObject(resultBodyStr, new  TypeReference<>(){

        });

    }

    /**
     * 修改短链接
     * @param shortLinkUpdateReqDTO
     */
    default void updateShortLink(ShortLinkUpdateReqDTO shortLinkUpdateReqDTO){
        HttpUtil.post("http://127.0.0.1:8001/api/shortlink/v1/update",JSON.toJSONString(shortLinkUpdateReqDTO));
    }


    /**
     *分页查询短链接
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("gid", shortLinkPageReqDTO.getGid());
        map.put("orderTag", shortLinkPageReqDTO.getOrderTag());
        map.put("current", shortLinkPageReqDTO.getCurrent());
        map.put("size", shortLinkPageReqDTO.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/page",map);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {

        });
    }
    /**
     * 查询短链接总量
     */
    default Result<List<ShortLinkGroupCountQueryRespDTO>> listGroupShortLinkCount(List<String> gidList)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("gidList", gidList);
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/count",map);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {

        });
    }


    /**
     * 根据网址获取网站标题
     * @param url
     * @return
     */
    default Result<String> getTitleByUrl(String url)
    {
        String resultStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/title?url="+url);
        return JSON.parseObject(resultStr, new  TypeReference<>(){});
    }


    /**
     * 将短链接移至回收站
     */
    default void saveRecycleBin(RecycleBinSaveReqDTO recycleBinSaveReqDTO){
        HttpUtil.post("http://127.0.0.1:8001/api/shortlink/v1/recycle-bin/save",JSON.toJSONString(recycleBinSaveReqDTO));
    }

    /**
     *分页查询短链接
     */
    default Result<IPage<ShortLinkPageRespDTO>> pageRecycleShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("gidList", shortLinkRecyclePageReqDTO.getGidList());
        map.put("current", shortLinkRecyclePageReqDTO.getCurrent());
        map.put("size", shortLinkRecyclePageReqDTO.getSize());
        String resultPageStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/recycle-bin/page",map);
        return JSON.parseObject(resultPageStr, new TypeReference<>() {

        });
    }

    /**
     * 回收站短链接恢复
     */
    default void recoverRecycleBin(RecycleBinRecoverReqDTO recycleBinRecoverReqDTO){
        HttpUtil.post("http://127.0.0.1:8001/api/shortlink/v1/recycle-bin/recover",JSON.toJSONString(recycleBinRecoverReqDTO));
    }


    /**
     * 访问单个短链接指定时间内监控数据
     *
     * @param requestParam 访问短链接监控请求参数
     * @return 短链接监控信息
     */
    default Result<ShortLinkStatsRespDTO> oneShortLinkStats(ShortLinkStatsReqDTO requestParam) {
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/stats", BeanUtil.beanToMap(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }


    /**
     * 访问单个短链接指定时间内访问记录监控数据
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> shortLinkStatsAccessRecord(ShortLinkStatsAccessRecordReqDTO requestParam){
        Map<String,Object> stringObjectMap = BeanUtil.beanToMap(requestParam,false,true);
        stringObjectMap.remove("orders");
        stringObjectMap.remove("records");
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/access-record",stringObjectMap);
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }

    /**
     * 访问分组短链接指定时间内监控数据
     *
     * @param requestParam 访分组问短链接监控请求参数
     * @return 分组短链接监控信息
     */
    default Result<ShortLinkStatsRespDTO> groupShortLinkStats(ShortLinkGroupStatsReqDTO requestParam) {
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/stats/group", BeanUtil.beanToMap(requestParam));
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }

    /**
     * 访问分组短链接指定时间内监控访问记录数据
     *
     * @param requestParam 访问分组短链接监控访问记录请求参数
     * @return 分组短链接监控访问记录信息
     */
    default Result<IPage<ShortLinkStatsAccessRecordRespDTO>> groupShortLinkStatsAccessRecord(ShortLinkGroupStatsAccessRecordReqDTO requestParam) {
        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(requestParam, false, true);
        stringObjectMap.remove("orders");
        stringObjectMap.remove("records");
        String resultBodyStr = HttpUtil.get("http://127.0.0.1:8001/api/shortlink/v1/access-record/group", stringObjectMap);
        return JSON.parseObject(resultBodyStr, new TypeReference<>() {
        });
    }
}
