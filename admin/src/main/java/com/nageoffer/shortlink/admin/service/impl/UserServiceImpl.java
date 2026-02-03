package com.nageoffer.shortlink.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.admin.common.convention.exception.ClientException;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dao.mapper.UserMapper;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegiserReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.GroupService;
import com.nageoffer.shortlink.admin.service.UserService;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.nageoffer.shortlink.admin.common.constant.RedisCacheConstant.LOCK_USER_REGISER_KEY;
import static com.nageoffer.shortlink.admin.common.convention.enums.UserErrorCodeEnum.*;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, UserDO> implements UserService {

    @Autowired
    private RBloomFilter<String> userRegisterCachePenetrationBloomFilter;
    @Autowired
    private RedissonClient redissonClient;
    @Autowired
    private StringRedisTemplate  stringRedisTemplate;
    @Autowired
    private GroupService groupService;

    /**
     * 根据用户名查找用户
     * @param username
     * @return UserRespDTO
     */
    @Override
    public UserRespDTO getUserByName(String username) {
        UserDO userDO = this.lambdaQuery().eq(UserDO::getUsername, username).one();
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        UserRespDTO userRespDTO = new UserRespDTO();
        BeanUtils.copyProperties(userDO, userRespDTO);
        return userRespDTO;
    }

    /**
     * 查询用户名是否存在
     * @param username 用户名
     * @return 存在:true 不存在:fasle
     */
    @Override
    public Boolean hasUsername(String username) {
        return userRegisterCachePenetrationBloomFilter.contains(username);
    }


    /**
     * 用户注册
     * @param userRegiserReqDTO
     */
    @Override
    public void userRegister(UserRegiserReqDTO userRegiserReqDTO) {
        if (hasUsername(userRegiserReqDTO.getUsername())) {
            throw new ClientException(USER_NAME_EXIST);
        }
        RLock lock = redissonClient.getLock(LOCK_USER_REGISER_KEY +  userRegiserReqDTO.getUsername());
        if (!lock.tryLock()){
            //获取不到锁,抛异常
            throw new ClientException(USER_NAME_EXIST);
        }
        try {
            try {
                int inserted = baseMapper.insert(BeanUtil.toBean(userRegiserReqDTO, UserDO.class));
                if (inserted < 1)
                {
                    throw new ClientException(USER_SAVE_ERROR);
                }
            } catch (ClientException e) {
                throw new ClientException(USER_SAVE_ERROR);
            }
            userRegisterCachePenetrationBloomFilter.add(userRegiserReqDTO.getUsername());
            groupService.saveGroup(userRegiserReqDTO.getUsername(),"默认分组");
        } finally {
          lock.unlock();
        }
    }

    /**
     * 用户更新
     * @param userUpdateReqDTO
     */
    @Override
    public void updateUser(UserUpdateReqDTO userUpdateReqDTO) {
        //TODO 验证当前用户名是否为登录用户
        //转换对象
        UserDO userDO = BeanUtil.toBean(userUpdateReqDTO, UserDO.class);
        userDO.setUsername(null);

        boolean success = this.lambdaUpdate()
                .eq(UserDO::getUsername,userUpdateReqDTO.getUsername())
                .update(userDO);

        if (!success) {
            throw new ClientException(USER_NULL);
        }
    }

    /**
     * 用户登录
     * @param userLoginReqDTO
     * @return
     */
    @Override
    public UserLoginRespDTO login(UserLoginReqDTO userLoginReqDTO) {
        UserDO userDO = this.lambdaQuery()
                .eq(UserDO::getUsername, userLoginReqDTO.getUsername())
                .eq(UserDO::getPassword, userLoginReqDTO.getPassword())
                .one();
        //不存在抛异常
        if (userDO == null) {
            throw new ClientException(USER_NULL);
        }
        Map<Object ,Object> hasLoginMap = stringRedisTemplate.opsForHash().entries("login_" + userLoginReqDTO.getUsername());
        if (CollUtil.isNotEmpty(hasLoginMap)) {
            String token = hasLoginMap.keySet().stream()
                    .findFirst()
                    .map(Object::toString)
                    .orElseThrow(() -> new ClientException("用户登录错误"));
            return new UserLoginRespDTO(token);
        }
        //存在存入Redis缓存
        String  uuid = UUID.randomUUID().toString();
        stringRedisTemplate.opsForHash().put("login_" + userLoginReqDTO.getUsername(),uuid, JSON.toJSONString(userDO));
        stringRedisTemplate.expire("login_" + userLoginReqDTO.getUsername(),30L, TimeUnit.DAYS);
        return new UserLoginRespDTO(uuid);
    }

    /**
     * 判断用户是否登录
     * @param token
     * @return
     */
    @Override
    public Boolean checkLogin(String username,String token) {
        return stringRedisTemplate.opsForHash().get("login_"+username,token) != null;
    }
}
