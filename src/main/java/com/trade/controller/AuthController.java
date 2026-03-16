package com.trade.controller;

import com.trade.common.Result;
import com.trade.dto.LoginRequest;
import com.trade.dto.RegisterRequest;
import com.trade.service.AuthService;
import com.trade.vo.CaptchaVO;
import com.trade.vo.LoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 认证管理控制器
 *
 * 提供验证码获取、用户登录、用户注册功能
 */
@Api(tags = "认证管理")
@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    /**
     * 注入认证服务
     */
    @Autowired
    private AuthService authService;

    @ApiOperation("获取验证码")
    @GetMapping("/captcha")
    public Result<CaptchaVO> getCaptcha() {
        return Result.success(authService.generateCaptcha());
    }

    @ApiOperation("注册")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success();
    }

    @ApiOperation("登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginRequest request) {
        return Result.success(authService.login(request));
    }
}
