package com.nageoffer.shortlink.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.admin.dao.entity.UserDO;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegiserReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;

public interface UserService extends IService<UserDO> {
    /**
     * 根据用户名查找用户
     * @param username
     * @return UserRespDTO
     */
    UserRespDTO getUserByName(String username);

    /**
     * 查询用户名是否存在
     * @param username 用户名
     * @return 存在:true 不存在:fasle
     */
    Boolean hasUsername(String username);

    /**
     * 用户注册
     * @param userRegiserReqDTO
     */
    void userRegister(UserRegiserReqDTO userRegiserReqDTO);

    /**
     * 用户更新
     * @param userUpdateReqDTO
     */
    void updateUser(UserUpdateReqDTO userUpdateReqDTO);

    /**
     * 用户登录
     * @param userLoginReqDTO
     * @return
     */
    UserLoginRespDTO login(UserLoginReqDTO userLoginReqDTO);

    /**
     * 判断用户是否登录
     * @param token
     * @return
     */
    Boolean checkLogin(String username,String token);
}
