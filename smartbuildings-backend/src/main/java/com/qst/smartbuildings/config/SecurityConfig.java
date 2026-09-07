package com.qst.smartbuildings.config;

import com.alibaba.fastjson.JSON;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.security.AuthenticationTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security安全配置类
 * 配置认证管理器、密码编码器、Token过滤器、URL权限控制等
 * 采用无状态Token认证机制，禁用Session、CSRF等传统Web安全特性
 *
 * @author smartbuildings-team
 * @since 2026-09-07
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OncePerRequestFilter authenticationTokenFilter() {
        return new AuthenticationTokenFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(AuthConst.OPEN_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(authenticationTokenFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(
                                ((request, response, authException) -> {
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write(JSON.toJSONString(Result.error(401,"未登录")));
                                    response.getWriter().flush();
                                }
                        ))
                        .accessDeniedHandler(
                                ((request, response, accessDeniedException) -> {
                                    response.setContentType("application/json;charset=UTF-8");
                                    response.getWriter().write(JSON.toJSONString(Result.error(403,"未授权")));
                                    response.getWriter().flush();
                                }
                        ))
                );
        return http.build();
    }
}