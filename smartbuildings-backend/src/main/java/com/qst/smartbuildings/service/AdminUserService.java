package com.qst.smartbuildings.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.PageEntity;

import java.util.List;

public interface AdminUserService {

    AdminUser getById(Long userId);

    Page<AdminUser> page(PageEntity pageEntity, String username, String realname, Integer status);

    List<AdminUser> list();

    boolean save(AdminUser adminUser);

    boolean updateById(AdminUser adminUser);

    boolean removeById(Long userId);

    boolean updateStatus(Long userId, Integer status);

    boolean updatePassword(Long userId, String newPassword);
}