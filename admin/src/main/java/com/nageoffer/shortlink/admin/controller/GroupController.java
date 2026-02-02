package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSaveReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/shortlink/admin/v1/group")
public class GroupController {

    @Autowired
    private GroupService groupService;

    /**
     * 新增短链接分组
     */
    @PostMapping
    public Result<Void> save(@RequestBody ShortLinkGroupSaveReqDTO shortLinkGroupSaveReqDTO)
    {
        groupService.saveGroup(shortLinkGroupSaveReqDTO.getName());
        return Results.success();
    }

    /**
     * 查询短链接分组集合
     */
    @GetMapping
    public Result<List<ShortLinkGroupRespDTO>> groupList()
    {
        return Results.success(groupService.groupList());
    }

    /**
     * 修改短链接分组名称
     */
    @PutMapping
    public Result<Void> updateGroup(@RequestBody ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO)
    {
        groupService.updateGroup(shortLinkGroupUpdateReqDTO);
        return Results.success();
    }

    /**
     *删除短链接
     */
    @DeleteMapping
    public Result<Void> deleteGroup(@RequestParam String gid)
    {
        groupService.deleteGroup(gid);
        return Results.success();
    }


    /**
     * 短链接分组排序
     */
    @PostMapping("/sort")
    public Result<Void> sortGroup(@RequestBody List<ShortLinkGroupSortReqDTO> shortLinkGroupSortReqDTO)
    {
        groupService.sortGroup(shortLinkGroupSortReqDTO);
        return Results.success();
    }


}
