package com.qst.smartbuildings.config;

import cn.hutool.json.JSONUtil;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.util.RedisClient;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 基于 Admin-Token 的登录认证过滤器
 * <p>
 * 从请求头 {@link AuthConst#TOKEN_NAME} 中读取 token，若 Redis 中存在对应登录态，
 * 则将认证信息写入 SecurityContext，使受保护接口通过认证。
 *
 * @author 智能建造团队
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private final RedisClient redisClient;

    public TokenAuthenticationFilter(RedisClient redisClient) {
        this.redisClient = redisClient;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 已经完成认证的请求无需重复处理
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = request.getHeader(AuthConst.TOKEN_NAME);
            if (StringUtils.hasText(token)) {
                Object stored = redisClient.get(AuthConst.USER_TOKEN_HEAD + token);
                if (stored != null) {
                    String principal = JSONUtil.parseObj(stored).getStr("userId");
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}