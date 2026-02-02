package com.nageoffer.shortlink.project.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.nageoffer.shortlink.project.common.convention.result.Result;
import com.nageoffer.shortlink.project.common.convention.result.Results;
import com.nageoffer.shortlink.project.dto.req.RecycleBinRecoverReqDTO;
import com.nageoffer.shortlink.project.dto.req.RecycleBinSaveReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkRecyclePageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.RecycleBinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 回收站控制层
 */
@RestController
@RequestMapping("/api/shortlink/v1/recycle-bin")
public class RecycleBinController {
    @Autowired
    private RecycleBinService recycleBinService;

    /**
     * 将短链接移至回收站
     */
    @PostMapping("/save")
    public Result<Void> saveRecycleBin(@RequestBody RecycleBinSaveReqDTO recycleBinSaveReqDTO){
        return Results.success(recycleBinService.saveRecycleBin(recycleBinSaveReqDTO));

    }
    /**
     *分页查询回收站短链接
     */
    @GetMapping("/page")
    public Result<IPage<ShortLinkPageRespDTO>> pageShortLink(ShortLinkRecyclePageReqDTO shortLinkRecyclePageReqDTO)
    {
        return Results.success(recycleBinService.pageShortLink(shortLinkRecyclePageReqDTO));
    }

    /**
     * 回收站短链接恢复
     */
    @PostMapping("/recover")
    public Result<Void> recoverRecycleBin(@RequestBody RecycleBinRecoverReqDTO recycleBinRecoverReqDTO){
        recycleBinService.recoverRecycleBin(recycleBinRecoverReqDTO);
        return Results.success();

    }


}
