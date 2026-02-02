package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.biz.user.UserContext;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.dao.entity.GroupDO;
import com.nageoffer.shortlink.admin.dao.mapper.GroupMapper;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupSortReqDTO;
import com.nageoffer.shortlink.admin.dto.req.ShortLinkGroupUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.ShortLinkGroupRespDTO;
import com.nageoffer.shortlink.admin.remote.ShortLinkRemoteService;
import com.nageoffer.shortlink.admin.remote.dto.resp.ShortLinkGroupCountQueryRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.util.RandomGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.LOCK_GROUP_CREATE_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceImpl extends ServiceImpl<GroupMapper, GroupDO> implements GroupService {



    private final RedissonClient redissonClient;

    @Value("${short-link.group.max-num}")
    private Integer groupMaxNum;

    ShortLinkRemoteService shortLinkRemoteService = new ShortLinkRemoteService(){};

    /**
     * 新增短链接
     * @param groupName
     */
    @Override
    public void saveGroup(String groupName) {
      saveGroup(UserContext.getUsername(),groupName);
    }

    @Override
    public void saveGroup(String username, String groupName) {
//        String gids;
//        while(true){
//            gids = RandomGenerator.generateRandomStrin();
//            if (hasGroupGid(username,gids))
//                break;
//        }
//        GroupDO groupDO = GroupDO.builder()
//                .gid(gids)
//                .sortOrder(0)
//                .username(username)
//                .name(groupName)
//                .build();
//        this.save(groupDO);
        RLock lock = redissonClient.getLock(String.format(LOCK_GROUP_CREATE_KEY, username));
        lock.lock();
        try {
            LambdaQueryWrapper<GroupDO> queryWrapper = Wrappers.lambdaQuery(GroupDO.class)
                    .eq(GroupDO::getUsername, username)
                    .eq(GroupDO::getDelFlag, 0);
            List<GroupDO> groupDOList = baseMapper.selectList(queryWrapper);
            if (CollUtil.isNotEmpty(groupDOList) && groupDOList.size() == groupMaxNum) {
                throw new ClientException(String.format("已超出最大分组数：%d", groupMaxNum));
            }
            String gid;
            do {
                gid = RandomGenerator.generateRandomStrin();
            } while (!hasGroupGid(username, gid));
            GroupDO groupDO = GroupDO.builder()
                    .gid(gid)
                    .sortOrder(0)
                    .username(username)
                    .name(groupName)
                    .build();
            baseMapper.insert(groupDO);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 短链接查询
     */
//    @Override
//    public List<ShortLinkGroupRespDTO> groupList() {
//        List<GroupDO> groupDOS = this.lambdaQuery()
//                .eq(GroupDO::getUsername,UserContext.getUsername())
//                .eq(GroupDO::getDelFlag,0)
////              先按 sort_order (排序权重) 降序排列。
////              如果权重相同，再按 update_time (更新时间) 降序排列。
//                .orderByDesc(GroupDO::getSortOrder,GroupDO::getUpdateTime)
//                .list();
//        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkRemoteService.listGroupShortLinkCount(
//                groupDOS.stream().map(GroupDO::getGid).toList()
//        );
//        List<ShortLinkGroupRespDTO> listRespDTO =BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);
//        listRespDTO.forEach(groupDO -> {
//            Optional<ShortLinkGroupCountQueryRespDTO> first = listResult.getData().stream()
//                    .filter(item -> Objects.equals(item.getGid(), groupDO.getGid())).findFirst();
//            first.ifPresent(item -> groupDO.setShortLinkCount(item.getShortLinkCount()));
//        });
//
//
//        return listRespDTO;
//    }

    /**
     * 短链接查询
     */

    @Override
    public List<ShortLinkGroupRespDTO> groupList() {
        // 1. 查询当前用户所有未删除分组
        List<GroupDO> groupDOS = this.lambdaQuery()
                .eq(GroupDO::getUsername, UserContext.getUsername())
                .eq(GroupDO::getDelFlag, 0)
                .orderByDesc(GroupDO::getSortOrder, GroupDO::getUpdateTime)
                .list();

        // 如果没有分组，直接返回空列表
        if (CollectionUtils.isEmpty(groupDOS)) {
            return Collections.emptyList();
        }

        // 2. 批量远程查询每个分组的短链接数量
        Result<List<ShortLinkGroupCountQueryRespDTO>> listResult = shortLinkRemoteService.listGroupShortLinkCount(
                groupDOS.stream().map(GroupDO::getGid).toList()
        );
        log.info("统计结果: {}", listResult);  // ← 关键！看这里是否有数据

        // 3. 转换实体
        List<ShortLinkGroupRespDTO> listRespDTO = BeanUtil.copyToList(groupDOS, ShortLinkGroupRespDTO.class);

        // 4. 高效合并统计数量（优化核心）
        List<ShortLinkGroupCountQueryRespDTO> countList = Optional.ofNullable(listResult)
                .map(Result::getData)
                .orElse(Collections.emptyList());

        Map<String, Integer> countMap = countList.stream()
                .collect(Collectors.toMap(
                        ShortLinkGroupCountQueryRespDTO::getGid,
                        dto -> Optional.ofNullable(dto.getShortLinkCount()).orElse(0),
                        (v1, v2) -> v1
                ));

        listRespDTO.forEach(dto ->
                dto.setShortLinkCount(countMap.getOrDefault(dto.getGid(), 0))
        );

        return listRespDTO;
    }

    /**
     * 修改短链接分组名称
     */
    @Override
    public void updateGroup(ShortLinkGroupUpdateReqDTO shortLinkGroupUpdateReqDTO) {
        this.lambdaUpdate()
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getGid,shortLinkGroupUpdateReqDTO.getGid())
                .set(GroupDO::getName,shortLinkGroupUpdateReqDTO.getName())
                .update();
    }

    /**
     *删除短链接
     */
    @Override
    public void deleteGroup(String gid) {
        this.lambdaUpdate()
                .eq(GroupDO::getUsername,UserContext.getUsername())
                .eq(GroupDO::getDelFlag,0)
                .eq(GroupDO::getGid,gid)
                .set(GroupDO::getDelFlag,1)
                .update();
    }

    /**
     * 短链接分组排序
     */
    @Override
    public void sortGroup(List<ShortLinkGroupSortReqDTO> shortLinkGroupSortReqDTO) {
            shortLinkGroupSortReqDTO.forEach(groupDO -> {
                lambdaUpdate()
                        .eq(GroupDO::getGid,groupDO.getGid())
                        .eq(GroupDO::getUsername,UserContext.getUsername())
                        .eq(GroupDO::getDelFlag,0)
                        .set(GroupDO::getSortOrder,groupDO.getSortOrder())
                        .update();
            });
    }

    private boolean hasGroupGid(String username,String gid){
        GroupDO groupDO =  this.lambdaQuery()
                .eq(GroupDO::getGid, gid)
                .eq(GroupDO::getUsername, Optional.ofNullable(username).orElse(UserContext.getUsername()))
                .one();
        return groupDO == null;
    }
}
