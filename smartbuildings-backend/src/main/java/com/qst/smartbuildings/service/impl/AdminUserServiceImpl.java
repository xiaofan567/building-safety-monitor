package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.PageEntity;
import com.qst.smartbuildings.mapper.AdminUserMapper;
import com.qst.smartbuildings.service.AdminUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class AdminUserServiceImpl extends ServiceImpl<AdminUserMapper, AdminUser> implements AdminUserService {

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