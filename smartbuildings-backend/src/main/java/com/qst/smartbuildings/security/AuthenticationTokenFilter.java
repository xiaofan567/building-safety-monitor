package com.qst.smartbuildings.security;

import cn.hutool.json.JSONUtil;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Token认证过滤器
 * 从请求头提取Token，验证有效性并设置Spring Security认证上下文
 * 支持Redis存储的无状态Token认证机制
 *
 * @author smartbuildings-team
 * @since 2026-09-07
 */
public class AuthenticationTokenFilter extends OncePerRequestFilter implements Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationTokenFilter.class);

    @Autowired
    private RedisClient redisClient;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String token = request.getHeader(AuthConst.TOKEN_NAME);
        logger.debug("Processing request: {}", request.getRequestURI());
        response.setContentType(AuthConst.DEFAULT_CONTENT_TYPE);
        if (cn.hutool.util.StrUtil.isNotEmpty(token)) {
            Object obj = redisClient.get(token);
            AdminUser user = convertToAdminUser(obj);
            if (user != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }
        chain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return 2000;
    }

    private AdminUser convertToAdminUser(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof AdminUser) {
            return (AdminUser) obj;
        }
        String jsonStr = JSONUtil.toJsonStr(obj);
        return JSONUtil.toBean(jsonStr, AdminUser.class);
    }
}