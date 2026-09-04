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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

    @Autowired
    private RedisClient redisClient;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

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
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        redisClient.set(AuthConst.USER_TOKEN_HEAD + token, user.getUserId().toString(),
                AuthConst.TOKEN_EXPIRED_TIME, TimeUnit.SECONDS);
        Map<String, Object> result = new HashMap<>();
        result.put("adminToken", token);
        return result;
    }

    @Override
    public AdminUser queryLoginUser(String token) {
        Object userIdObj = redisClient.get(AuthConst.USER_TOKEN_HEAD + token);
        if (userIdObj == null) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }
        Long userId = Long.valueOf(userIdObj.toString());
        AdminUser user = baseMapper.selectById(userId);
        if (user == null) {
            throw new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }
        user.setPassword(null);
        user.setSalt(null);
        return user;
    }

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

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        AdminUser adminUser = new AdminUser();
        adminUser.setUserId(userId);
        adminUser.setPassword(newPassword);
        return baseMapper.updateById(adminUser) > 0;
    }
}