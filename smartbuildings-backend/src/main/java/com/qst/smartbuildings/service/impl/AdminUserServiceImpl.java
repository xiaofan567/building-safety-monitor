package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.common.AuthorizationCodeEnum;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.PageEntity;
import com.qst.smartbuildings.exception.AuthException;
import com.qst.smartbuildings.mapper.AdminUserMapper;
import com.qst.smartbuildings.service.AdminUserService;
import com.qst.smartbuildings.util.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 管理员用户服务实现类
 * 负责用户认证（登录/登出）、用户信息管理、密码修改等核心业务逻辑
 * 使用Spring Security + Redis Token机制实现无状态认证
 *
 * @author smartbuildings-team
 * @since 2026-09-07
 */
@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    @Autowired
    private RedisClient redisClient;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 用户登录认证
     * 验证用户名密码，生成安全Token并存储到Redis
     *
     * @param username 用户名（必填）
     * @param password 密码（必填）
     * @return 包含adminToken的Map
     * @throws AuthException 用户名为空、用户不存在、账户禁用、密码错误时抛出
     */
    @Override
    public Map<String, Object> login(String username, String password) {
        if (!StringUtils.hasText(username)) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }
        AdminUser user = baseMapper.selectOne(
                new LambdaQueryWrapper<AdminUser>().eq(AdminUser::getUsername, username));
        if (user == null) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
        if (user.getStatus() != null && user.getStatus() != 1) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
        String token = generateSecureToken();
        AdminUser tokenUser = createTokenUser(user);
        redisClient.set(AuthConst.USER_TOKEN_HEAD + token, tokenUser,
                AuthConst.TOKEN_EXPIRED_TIME, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>();
        result.put("adminToken", token);
        return result;
    }

    /**
     * 根据Token查询当前登录用户信息
     *
     * @param token 用户Token（从请求头获取）
     * @return 当前登录用户信息（不含敏感字段）
     * @throws AuthException Token无效或已过期时抛出
     */
    @Override
    public AdminUser queryLoginUser(String token) {
        Object obj = redisClient.get(AuthConst.USER_TOKEN_HEAD + token);
        if (obj == null) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }
        AdminUser user = convertToAdminUser(obj);
        if (user == null) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }
        return user;
    }

    private AdminUser convertToAdminUser(Object obj) {
        if (obj instanceof AdminUser) {
            return (AdminUser) obj;
        }
        String jsonStr = cn.hutool.json.JSONUtil.toJsonStr(obj);
        return cn.hutool.json.JSONUtil.toBean(jsonStr, AdminUser.class);
    }

    /**
     * 用户登出，清除Redis中的Token
     *
     * @param token 用户Token
     */
    @Override
    public void logout(String token) {
        redisClient.delete(AuthConst.USER_TOKEN_HEAD + token);
    }

    @Override
    public AdminUser getById(Long userId) {
        return baseMapper.selectById(userId);
    }

    @Override
    public Page<AdminUser> page(PageEntity pageEntity, String username, String realname, Integer status) {
        LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(username)) {
            wrapper.like(AdminUser::getUsername, username);
        }
        if (StringUtils.hasText(realname)) {
            wrapper.like(AdminUser::getRealname, realname);
        }
        if (status != null) {
            wrapper.eq(AdminUser::getStatus, status);
        }
        return baseMapper.selectPage(pageEntity.parse(), wrapper);
    }

    @Override
    public List<AdminUser> list() {
        return baseMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public boolean save(AdminUser adminUser) {
        if (StringUtils.hasText(adminUser.getPassword())) {
            adminUser.setPassword(passwordEncoder.encode(adminUser.getPassword()));
        }
        return baseMapper.insert(adminUser) > 0;
    }

    @Override
    public boolean updateById(AdminUser adminUser) {
        return baseMapper.updateById(adminUser) > 0;
    }

    @Override
    public boolean removeById(Long userId) {
        return baseMapper.deleteById(userId) > 0;
    }

    @Override
    public boolean updateStatus(Long userId, Integer status) {
        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(userId);
        adminUser.setStatus(status);
        return baseMapper.updateById(adminUser) > 0;
    }

    /**
     * 修改用户密码（自动BCrypt加密）
     *
     * @param userId      用户ID
     * @param newPassword 新密码（必填，长度6-100）
     * @return 是否修改成功
     * @throws AuthException 新密码为空时抛出
     */
    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        if (!StringUtils.hasText(newPassword)) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(userId);
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        return baseMapper.updateById(adminUser) > 0;
    }

    private String generateSecureToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private AdminUser createTokenUser(AdminUser user) {
        AdminUser tokenUser = new AdminUser();
        tokenUser.setUserId(user.getUserId());
        tokenUser.setUsername(user.getUsername());
        tokenUser.setRealname(user.getRealname());
        tokenUser.setStatus(user.getStatus());
        return tokenUser;
    }
}