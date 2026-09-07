package com.qst.smartbuildings.common;

public class AuthConst {
    //请求中的token名
    public static final String TOKEN_NAME = "Admin-Token";
    //默认的content-type
    public static final String DEFAULT_CONTENT_TYPE = "application/json;charset=UTF-8";
    //    Redis中的token前缀
    public static final String USER_TOKEN_HEAD = "user_token_";
    //    Access Token过期时间（30分钟）
    public static final Integer TOKEN_EXPIRED_TIME = 1800;
    //    Refresh Token过期时间（7天）
    public static final Integer REFRESH_TOKEN_EXPIRED_TIME = 60*60*24*7;
    // 放行接口（无需认证即可访问）
    public static final String[] OPEN_URLS = new String[]{
            "/login",          // 登录接口
            "/relogin",        // 重新登录接口
            "/captcha",        // 验证码接口（如果有）
            "/api/public/**",   // 其他公开接口（支持通配符）
            "/ai_image/**"     // AI图片接口
    };
}