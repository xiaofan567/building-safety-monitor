package com.qst.smartbuildings.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Data
@TableName("admin_user")
@Schema(description = "管理员用户实体")
public class AdminUser implements Serializable, UserDetails {

    private static final long serialVersionUID = 1L;

    @TableId(value = "user_id", type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long userId;

    @Schema(description = "用户角色列表")
    private transient Collection<String> roles;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "密码")
    private String password;

    @Schema(description = "安全符")
    private String salt;

    @Schema(description = "头像")
    private String img;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "真实姓名")
    private String realname;

    @Schema(description = "员工编号")
    private String num;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "性别：0 未选择，1 男，2 女")
    private Integer sex;

    @Schema(description = "部门ID")
    private Integer deptId;

    @Schema(description = "岗位")
    private String post;

    @Schema(description = "状态：0 禁用，1 正常，2 未激活")
    private Integer status;

    @Schema(description = "直属上级ID")
    private Long parentId;

    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

    @Schema(description = "最后登录IP")
    private String lastLoginIp;

    @Schema(description = "旧用户ID")
    private Long oldUserId;

    @Schema(description = "租户ID")
    private Integer tenantId;

    @Schema(description = "项目ID")
    private Integer projectId;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (roles != null && !roles.isEmpty()) {
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .toList();
        }
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }
}