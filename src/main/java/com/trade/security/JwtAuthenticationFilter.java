package com.trade.security;

import com.trade.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;

/**
 * JWT 认证过滤器
 * 
 * 每次请求都会经过此过滤器，从请求头中获取 JWT Token 并进行验证
 * 
 * @Component: 标识这是一个 Spring 组件
 * @OncePerRequestFilter: 确保每次请求只执行一次过滤
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    /**
     * 注入 JWT 工具类
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 执行过滤器核心过滤逻辑
     * 
     * 1. 从请求头 Authorization 中获取 Token
     * 2. 验证 Token 是否有效
     * 3. 如果有效，解析出用户信息并设置到 SecurityContext 中
     * 4. 继续执行后续过滤器
     * 
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param filterChain 过滤器链
     * @throws ServletException Servlet 异常
     * @throws IOException IO 异常
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 从请求头中获取 Authorization
        String token = request.getHeader("Authorization");
        
        // 检查 Token 是否存在且格式正确（Bearer 开头）
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀
            token = token.substring(7);
            
            // 验证 Token 是否有效
            if (jwtUtil.validateToken(token)) {
                // 从 Token 中解析用户信息
                Long userId = jwtUtil.getUserIdFromToken(token);
                String username = jwtUtil.getUsernameFromToken(token);
                String role = jwtUtil.getRoleFromToken(token);
                
                // 创建认证对象，包含用户 ID、权限信息
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                );
                authentication.setDetails(username);
                
                // 将认证信息设置到 SecurityContext 中，供后续使用
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        
        // 继续执行后续过滤器
        filterChain.doFilter(request, response);
    }
}
