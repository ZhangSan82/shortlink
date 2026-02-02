package com.nageoffer.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.nageoffer.shortlink.admin.common.convention.result.Result;
import com.nageoffer.shortlink.admin.common.convention.result.Results;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegiserReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserActualRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制
 */
@Slf4j
@RestController
@RequestMapping("/api/short-link/admin/v1/user")
public class UserController {
    @Autowired
    private UserService userService;

    /**
     * 根据用户名查找用户
     */
    @GetMapping("{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable String username)
    {
        UserRespDTO userRespDTO = userService.getUserByName(username);
        log.info("{}", userRespDTO);
        return Results.success(userRespDTO);

    }


    /**
     * 根据用户名查找无脱敏用户
     */
    @GetMapping("/actual/{username}")
    public Result<UserActualRespDTO> getActualUserByUsername(@PathVariable String username) {
        UserRespDTO userRespDTO = userService.getUserByName(username);
        UserActualRespDTO userActualRespDTO = new UserActualRespDTO();
        userActualRespDTO = BeanUtil.toBean(userRespDTO, UserActualRespDTO.class);
        log.info("{}", userActualRespDTO);
        return Results.success(userActualRespDTO);
    }

    /**
     * 判断用户名是否存在
     */
    @GetMapping("/has-username")
    public Result<Boolean> hasUsername(@RequestParam("username") String username) {
        return Results.success(userService.hasUsername(username));
    }

    /**
     * 注册用户
     */
    @PostMapping
    public Result<Void> register(@RequestBody UserRegiserReqDTO userRegiserReqDTO){
        userService.userRegister(userRegiserReqDTO);
        return Results.success();
    }

    /**
     * 用户更新
     */
    @PutMapping
    public Result<Void> updateUser(@RequestBody UserUpdateReqDTO userUpdateReqDTO){
        userService.updateUser(userUpdateReqDTO);
        return Results.success();
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO userLoginReqDTO){
        UserLoginRespDTO userLoginRespDTO = userService.login(userLoginReqDTO);
        return Results.success(userLoginRespDTO);
    }

    /**
     * 判断用户是否登录
     */
    @GetMapping("/check-login")
    public Result<Boolean> checklogin(@RequestParam("username") String username,@RequestParam("token") String token){
        return Results.success(userService.checkLogin(username,token));
    }


}
