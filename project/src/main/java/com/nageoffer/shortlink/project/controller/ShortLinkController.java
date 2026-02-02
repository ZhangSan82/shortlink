package com.nageoffer.shortlink.project.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.project.common.convention.result.Result;
import com.nageoffer.shortlink.project.common.convention.result.Results;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.handler.CustomBlockHandler;
import com.nageoffer.shortlink.project.service.ShortLinkService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shortlink/v1")
public class ShortLinkController {

    @Autowired
    private ShortLinkService shortLinkService;


    /**
     *创建短链接
     */
    @PostMapping("/create")
    @SentinelResource(
            value = "create_short-link",
            blockHandler = "createShortLinkBlockHandlerMethod",
            blockHandlerClass = CustomBlockHandler.class
    )
    public Result<ShortLinkCreateRespDTO> createShortLink(@RequestBody ShortLinkCreateReqDTO  shortLinkCreateReqDTO)
    {
        return Results.success(shortLinkService.createShortLink(shortLinkCreateReqDTO));
    }

    /**
     * 批量创建短链接
     */
    @PostMapping("/create/batch")
    public Result<ShortLinkBatchCreateRespDTO> batchCreateShortLink(@RequestBody ShortLinkBatchCreateReqDTO requestParam) {
        return Results.success(shortLinkService.batchCreateShortLink(requestParam));
    }

    /**
     *分页查询短链接
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkPageReqDTO shortLinkPageReqDTO)
    {
        return Results.success(shortLinkService.pageShortLink(shortLinkPageReqDTO));
    }


    /**
     *查询短链接分组内数量
     */
    @GetMapping("/count")
    public  Result<List<ShortLinkGroupCountQueryRespDTO>>
    listGroupShortLinkCount(@RequestParam("gidList") List<String> gidList)
    {
        return Results.success(shortLinkService.listGroupShortLinkCount(gidList));
    }

    /**
     * 修改短链接
     */
    @PostMapping("/update")
    public Result<Void>  updateShortLink(@RequestBody ShortLinkUpdateReqDTO shortLinkUpdateReqDTO)
    {
        shortLinkService.updateShortLink(shortLinkUpdateReqDTO);
        return Results.success();
    }





}
