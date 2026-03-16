package com.trade.service;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.trade.common.BusinessException;
import com.trade.common.ErrorCode;
import com.trade.dto.LoginRequest;
import com.trade.dto.RegisterRequest;
import com.trade.entity.SysUser;
import com.trade.mapper.SysUserMapper;
import com.trade.util.JwtUtil;
import com.trade.vo.CaptchaVO;
import com.trade.vo.LoginVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 认证服务类
 *
 * 提供验证码生成和用户登录功能
 *
 * @Service: 标识这是一个服务层组件
 */
@Service
public class AuthService {

    /**
     * 注入 Redis 模板，用于存储验证码
     */
    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    /**
     * 注入认证管理器，用于验证用户名密码
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * 注入 JWT 工具类，用于生成 Token
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 注入用户 Mapper
     */
    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Value("${app.captcha.fail-threshold:3}")
    private int captchaFailThreshold;

    @Value("${app.captcha.fail-ttl-minutes:30}")
    private int captchaFailTtlMinutes;

    /**
     * 生成验证码
     *
     * 使用 Hutool 的 LineCaptcha 生成 4 位验证码图片，
     * 将验证码文本存入 Redis（5 分钟过期），
     * 返回验证码图片的 base64 编码和唯一标识 uuid
     *
     * @return 验证码信息对象（uuid + base64 图片）
     */
    public CaptchaVO generateCaptcha() {
        // 生成 120x40 的 4 位验证码
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 20);
        String code = captcha.getCode();
        String uuid = IdUtil.simpleUUID();

        // 将验证码存入 Redis，key 为 "captcha:" + uuid，有效期 5 分钟
        redisTemplate.opsForValue().set("captcha:" + uuid, code, 5, TimeUnit.MINUTES);

        return new CaptchaVO(uuid, captcha.getImageBase64Data());
    }

    /**
     * 注册（邮箱作为 username）
     */
    public void register(RegisterRequest request) {
        String email = request.getEmail();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, email);
        SysUser exist = sysUserMapper.selectOne(wrapper);
        if (exist != null) {
            throw new BusinessException(ErrorCode.AUTH_EMAIL_EXISTS);
        }

        SysUser user = new SysUser();
        user.setUsername(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("USER");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());
        user.setDeleted(0);

        sysUserMapper.insert(user);
    }

    /**
     * 用户登录
     *
     * 规则：
     * - 当登录失败次数达到阈值时才要求验证码
     * - 业务错误以 BusinessException 抛出，返回明确错误码
     */
    public LoginVO login(LoginRequest request) {
        String username = request.getUsername();
        String failKey = "login_fail:" + username;

        Integer failCount = parseInt(redisTemplate.opsForValue().get(failKey));
        failCount = (failCount == null) ? 0 : failCount;

        boolean captchaRequired = failCount >= captchaFailThreshold;
        if (captchaRequired) {
            if (request.getUuid() == null || request.getUuid().isBlank()
                    || request.getCode() == null || request.getCode().isBlank()) {
                throw new BusinessException(ErrorCode.AUTH_CAPTCHA_REQUIRED);
            }

            String cacheCode = redisTemplate.opsForValue().get("captcha:" + request.getUuid());
            if (cacheCode == null || !cacheCode.equalsIgnoreCase(request.getCode())) {
                throw new BusinessException(ErrorCode.AUTH_INVALID_CAPTCHA);
            }
            redisTemplate.delete("captcha:" + request.getUuid());
        }

        // 使用 Spring Security 的 AuthenticationManager 验证用户名和密码
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            long next = failCount + 1L;
            redisTemplate.opsForValue().set(failKey, String.valueOf(next), captchaFailTtlMinutes, TimeUnit.MINUTES);
            throw new BusinessException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        // 登录成功：清理失败计数
        redisTemplate.delete(failKey);

        // 查询用户信息
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser sysUser = sysUserMapper.selectOne(wrapper);

        // 更新最后登录时间
        sysUser.setLastLoginTime(LocalDateTime.now());
        sysUserMapper.updateById(sysUser);

        // 生成 JWT Token
        String token = jwtUtil.generateToken(sysUser.getId(), sysUser.getUsername(), sysUser.getRole());
        return new LoginVO(token, sysUser.getId(), sysUser.getUsername(), sysUser.getRole());
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
