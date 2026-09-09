package com.qst.smartbuildings.config;

import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.entity.AdminUser;
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
import java.util.Map;

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
                Object value = redisClient.get(AuthConst.USER_TOKEN_HEAD + token);
                if (value != null) {
                    // 从 Redis 中解析出数字 userId 作为认证主体，使下游可解析出真实登录人 ID。
                    // Redis 使用 JSON 序列化（未开启类型标记），读回的可能是 AdminUser、数字、
                    // 反序列化后的 Map 或字符串，需兼容处理。
                    Long principal = resolveUserId(value);
                    if (principal != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * 从 Redis 中的登录态值解析出数字 userId。
     * 兼容 AdminUser 对象、数字、反序列化后的 Map，以及数字字符串。
     *
     * @param value Redis 中存储的值
     * @return 数字 userId；无法解析时返回 null
     */
    private Long resolveUserId(Object value) {
        if (value instanceof AdminUser user) {
            return user.getUserId();
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof Map<?, ?> map) {
            Object id = map.get("userId");
            if (id instanceof Number) {
                return ((Number) id).longValue();
            }
            if (id != null) {
                return Long.valueOf(id.toString());
            }
        }
        try {
            return Long.valueOf(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}