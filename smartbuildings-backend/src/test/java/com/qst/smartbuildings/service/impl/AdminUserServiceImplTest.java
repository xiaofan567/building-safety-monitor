package com.qst.smartbuildings.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.common.AuthorizationCodeEnum;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.PageEntity;
import com.qst.smartbuildings.exception.AuthException;
import com.qst.smartbuildings.mapper.AdminUserMapper;
import com.qst.smartbuildings.util.RedisClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserServiceImpl 单元测试")
class AdminUserServiceImplTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    @Mock
    private RedisClient redisClient;

    @InjectMocks
    private AdminUserServiceImpl adminUserService;

    private BCryptPasswordEncoder passwordEncoder;
    private AdminUser testUser;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();

        testUser = new AdminUser();
        testUser.setUserId(1L);
        testUser.setUsername("admin");
        testUser.setPassword(passwordEncoder.encode("123456"));
        testUser.setRealname("管理员");
        testUser.setStatus(1);
    }

    @Nested
    @DisplayName("login 方法测试")
    class LoginTests {

        @Test
        @DisplayName("正常流程：有效用户名密码应返回包含adminToken的Map")
        void shouldReturnTokenMapWhenValidCredentials() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            Map<String, Object> result = adminUserService.login("admin", "123456");

            assertThat(result).isNotNull();
            assertThat(result).containsKey("adminToken");
            assertThat(result.get("adminToken")).isNotNull();
            assertThat((String) result.get("adminToken")).isNotEmpty();

            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<AdminUser> userCaptor = ArgumentCaptor.forClass(AdminUser.class);
            verify(redisClient).set(tokenCaptor.capture(), userCaptor.capture(),
                    eq(AuthConst.TOKEN_EXPIRED_TIME), eq(TimeUnit.SECONDS));

            AdminUser storedUser = userCaptor.getValue();
            assertThat(storedUser.getUserId()).isEqualTo(1L);
            assertThat(storedUser.getUsername()).isEqualTo("admin");
            assertThat(storedUser.getPassword()).isNull();
        }

        @Test
        @DisplayName("异常路径：用户名为null时应抛出AUTHORIZATION_USERNAME_REQUIRED")
        void shouldThrowExceptionWhenUsernameIsNull() {
            assertThatThrownBy(() -> adminUserService.login(null, "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }

        @Test
        @DisplayName("异常路径：用户名为空字符串时应抛出AUTHORIZATION_USERNAME_REQUIRED")
        void shouldThrowExceptionWhenUsernameIsEmpty() {
            assertThatThrownBy(() -> adminUserService.login("", "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }

        @Test
        @DisplayName("异常路径：用户名为空白字符时应抛出AUTHORIZATION_USERNAME_REQUIRED")
        void shouldThrowExceptionWhenUsernameIsBlank() {
            assertThatThrownBy(() -> adminUserService.login("   ", "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }

        @Test
        @DisplayName("异常路径：用户不存在时应抛出AUTHORIZATION_LOGIN_NO_USER")
        void shouldThrowExceptionWhenUserNotFound() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> adminUserService.login("unknown", "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("异常路径：账户状态为0（禁用）时应抛出AUTHORIZATION_USER_DISABLE_ERROR")
        void shouldThrowExceptionWhenAccountDisabled() {
            AdminUser disabledUser = new AdminUser();
            disabledUser.setUserId(2L);
            disabledUser.setUsername("disabled");
            disabledUser.setPassword(passwordEncoder.encode("123456"));
            disabledUser.setStatus(0);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(disabledUser);

            assertThatThrownBy(() -> adminUserService.login("disabled", "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }

        @Test
        @DisplayName("异常路径：账户状态为2（未激活）时应抛出AUTHORIZATION_USER_DISABLE_ERROR")
        void shouldThrowExceptionWhenAccountNotActivated() {
            AdminUser inactiveUser = new AdminUser();
            inactiveUser.setUserId(3L);
            inactiveUser.setUsername("inactive");
            inactiveUser.setPassword(passwordEncoder.encode("123456"));
            inactiveUser.setStatus(2);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(inactiveUser);

            assertThatThrownBy(() -> adminUserService.login("inactive", "123456"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }

        @Test
        @DisplayName("边界条件：账户status为null时应正常登录（非禁用）")
        void shouldLoginWhenStatusIsNull() {
            AdminUser noStatusUser = new AdminUser();
            noStatusUser.setUserId(4L);
            noStatusUser.setUsername("nostatus");
            noStatusUser.setPassword(passwordEncoder.encode("123456"));
            noStatusUser.setStatus(null);
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(noStatusUser);

            Map<String, Object> result = adminUserService.login("nostatus", "123456");

            assertThat(result).isNotNull();
            assertThat(result).containsKey("adminToken");
        }

        @Test
        @DisplayName("异常路径：密码错误时应抛出AUTHORIZATION_LOGIN_NO_USER")
        void shouldThrowExceptionWhenWrongPassword() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            assertThatThrownBy(() -> adminUserService.login("admin", "wrongpassword"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("边界条件：密码为null时密码匹配失败抛出AUTHORIZATION_LOGIN_NO_USER")
        void shouldThrowExceptionWhenPasswordIsNull() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            assertThatThrownBy(() -> adminUserService.login("admin", null))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }
    }

    @Nested
    @DisplayName("queryLoginUser 方法测试")
    class QueryLoginUserTests {

        @Test
        @DisplayName("正常流程：有效Token返回AdminUser对象")
        void shouldReturnUserWhenValidToken() {
            AdminUser storedUser = new AdminUser();
            storedUser.setUserId(1L);
            storedUser.setUsername("admin");
            storedUser.setRealname("管理员");
            storedUser.setStatus(1);
            when(redisClient.get("valid-token")).thenReturn(storedUser);

            AdminUser result = adminUserService.queryLoginUser("valid-token");

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("admin");
            assertThat(result.getRealname()).isEqualTo("管理员");
        }

        @Test
        @DisplayName("异常路径：Token不存在时抛出AUTHORIZATION_LOGIN")
        void shouldThrowExceptionWhenTokenNotFound() {
            when(redisClient.get("invalid-token")).thenReturn(null);

            assertThatThrownBy(() -> adminUserService.queryLoginUser("invalid-token"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }

        @Test
        @DisplayName("边界条件：Redis返回JSON字符串也能正确转换")
        void shouldConvertJsonStringToAdminUser() {
            String jsonStr = "{\"userId\":1,\"username\":\"admin\",\"realname\":\"管理员\",\"status\":1}";
            when(redisClient.get("json-token")).thenReturn(jsonStr);

            AdminUser result = adminUserService.queryLoginUser("json-token");

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("边界条件：Redis返回空字符串导致转换失败抛出AUTHORIZATION_LOGIN")
        void shouldThrowExceptionWhenJsonConversionFails() {
            when(redisClient.get("bad-token")).thenReturn("");

            assertThatThrownBy(() -> adminUserService.queryLoginUser("bad-token"))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }
    }

    @Nested
    @DisplayName("logout 方法测试")
    class LogoutTests {

        @Test
        @DisplayName("正常流程：应调用RedisClient.delete删除Token")
        void shouldDeleteTokenFromRedis() {
            adminUserService.logout("valid-token");

            verify(redisClient).delete("valid-token");
        }

        @Test
        @DisplayName("边界条件：空Token也能正常调用delete")
        void shouldDeleteEmptyToken() {
            adminUserService.logout("");

            verify(redisClient).delete("");
        }

        @Test
        @DisplayName("边界条件：null Token也能正常调用delete")
        void shouldDeleteNullToken() {
            adminUserService.logout(null);

            verify(redisClient).delete(null);
        }
    }

    @Nested
    @DisplayName("getById 方法测试")
    class GetByIdTests {

        @Test
        @DisplayName("正常流程：有效ID返回AdminUser")
        void shouldReturnUserWhenValidId() {
            when(adminUserMapper.selectById(1L)).thenReturn(testUser);

            AdminUser result = adminUserService.getById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("边界条件：不存在的ID返回null")
        void shouldReturnNullWhenIdNotFound() {
            when(adminUserMapper.selectById(999L)).thenReturn(null);

            AdminUser result = adminUserService.getById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("page 分页查询方法测试")
    class PageTests {

        private PageEntity pageEntity;

        @BeforeEach
        void setUpPage() {
            pageEntity = new PageEntity();
            pageEntity.setPage(1);
            pageEntity.setLimit(10);
            pageEntity.setPageType(1);
        }

        @Test
        @DisplayName("正常流程：无条件分页查询返回Page对象")
        void shouldReturnPageWithoutFilters() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, null, null, null);

            assertThat(result).isNotNull();
            assertThat(result.getCurrent()).isEqualTo(1);
            assertThat(result.getSize()).isEqualTo(10);
        }

        @Test
        @DisplayName("正常流程：按用户名模糊查询")
        void shouldFilterByUsername() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, "admin", null, null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("正常流程：按真实姓名模糊查询")
        void shouldFilterByRealname() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, null, "管理员", null);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("正常流程：按状态精确查询")
        void shouldFilterByStatus() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, null, null, 1);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("正常流程：多条件组合查询")
        void shouldFilterByAllConditions() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, "admin", "管理员", 1);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("边界条件：用户名为空字符串时不添加条件")
        void shouldNotFilterByEmptyUsername() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserService.page(pageEntity, "", null, null);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("list 方法测试")
    class ListTests {

        @Test
        @DisplayName("正常流程：返回所有用户列表")
        void shouldReturnAllUsers() {
            AdminUser user1 = new AdminUser();
            user1.setUserId(1L);
            user1.setUsername("admin");
            AdminUser user2 = new AdminUser();
            user2.setUserId(2L);
            user2.setUsername("user2");
            List<AdminUser> expectedList = Arrays.asList(user1, user2);
            when(adminUserMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(expectedList);

            List<AdminUser> result = adminUserService.list();

            assertThat(result).isNotNull();
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("admin");
            assertThat(result.get(1).getUsername()).isEqualTo("user2");
        }

        @Test
        @DisplayName("边界条件：无用户时返回空列表")
        void shouldReturnEmptyListWhenNoUsers() {
            when(adminUserMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<AdminUser> result = adminUserService.list();

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("save 方法测试")
    class SaveTests {

        @Test
        @DisplayName("正常流程：保存用户并加密密码")
        void shouldSaveUserAndEncryptPassword() {
            AdminUser newUser = new AdminUser();
            newUser.setUsername("newuser");
            newUser.setPassword("plainpassword");
            newUser.setRealname("新用户");
            when(adminUserMapper.insert(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.save(newUser);

            assertThat(result).isTrue();
            ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
            verify(adminUserMapper).insert(captor.capture());
            AdminUser savedUser = captor.getValue();
            assertThat(savedUser.getPassword()).isNotEqualTo("plainpassword");
            assertThat(passwordEncoder.matches("plainpassword", savedUser.getPassword())).isTrue();
        }

        @Test
        @DisplayName("正常流程：密码为空时直接保存，不加密")
        void shouldSaveUserWithoutPasswordEncryption() {
            AdminUser newUser = new AdminUser();
            newUser.setUsername("nopwd");
            newUser.setRealname("无密码用户");
            when(adminUserMapper.insert(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.save(newUser);

            assertThat(result).isTrue();
            ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
            verify(adminUserMapper).insert(captor.capture());
            AdminUser savedUser = captor.getValue();
            assertThat(savedUser.getPassword()).isNull();
        }

        @Test
        @DisplayName("边界条件：插入失败返回false")
        void shouldReturnFalseWhenInsertFails() {
            AdminUser newUser = new AdminUser();
            newUser.setUsername("failuser");
            when(adminUserMapper.insert(any(AdminUser.class))).thenReturn(0);

            boolean result = adminUserService.save(newUser);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateById 方法测试")
    class UpdateByIdTests {

        @Test
        @DisplayName("正常流程：更新成功返回true")
        void shouldReturnTrueWhenUpdateSuccess() {
            AdminUser updateUser = new AdminUser();
            updateUser.setUserId(1L);
            updateUser.setRealname("新名字");
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.updateById(updateUser);

            assertThat(result).isTrue();
            verify(adminUserMapper).updateById(updateUser);
        }

        @Test
        @DisplayName("边界条件：更新失败返回false")
        void shouldReturnFalseWhenUpdateFails() {
            AdminUser updateUser = new AdminUser();
            updateUser.setUserId(999L);
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(0);

            boolean result = adminUserService.updateById(updateUser);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("removeById 方法测试")
    class RemoveByIdTests {

        @Test
        @DisplayName("正常流程：删除成功返回true")
        void shouldReturnTrueWhenDeleteSuccess() {
            when(adminUserMapper.deleteById(1L)).thenReturn(1);

            boolean result = adminUserService.removeById(1L);

            assertThat(result).isTrue();
            verify(adminUserMapper).deleteById(1L);
        }

        @Test
        @DisplayName("边界条件：删除不存在的ID返回false")
        void shouldReturnFalseWhenDeleteNonexistentId() {
            when(adminUserMapper.deleteById(999L)).thenReturn(0);

            boolean result = adminUserService.removeById(999L);

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("updateStatus 方法测试")
    class UpdateStatusTests {

        @Test
        @DisplayName("正常流程：更新状态成功返回true")
        void shouldReturnTrueWhenStatusUpdateSuccess() {
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.updateStatus(1L, 0);

            assertThat(result).isTrue();
            ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
            verify(adminUserMapper).updateById(captor.capture());
            AdminUser updated = captor.getValue();
            assertThat(updated.getUserId()).isEqualTo(1L);
            assertThat(updated.getStatus()).isEqualTo(0);
        }

        @Test
        @DisplayName("边界条件：更新失败返回false")
        void shouldReturnFalseWhenStatusUpdateFails() {
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(0);

            boolean result = adminUserService.updateStatus(1L, 2);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("正常流程：启用账户（status=1）")
        void shouldEnableAccount() {
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.updateStatus(1L, 1);

            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("updatePassword 方法测试")
    class UpdatePasswordTests {

        @Test
        @DisplayName("正常流程：修改密码成功返回true")
        void shouldUpdatePasswordSuccessfully() {
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(1);

            boolean result = adminUserService.updatePassword(1L, "newPassword123");

            assertThat(result).isTrue();
            ArgumentCaptor<AdminUser> captor = ArgumentCaptor.forClass(AdminUser.class);
            verify(adminUserMapper).updateById(captor.capture());
            AdminUser updated = captor.getValue();
            assertThat(updated.getUserId()).isEqualTo(1L);
            assertThat(passwordEncoder.matches("newPassword123", updated.getPassword())).isTrue();
        }

        @Test
        @DisplayName("异常路径：新密码为空时应抛出AuthException")
        void shouldThrowExceptionWhenNewPasswordIsEmpty() {
            assertThatThrownBy(() -> adminUserService.updatePassword(1L, ""))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("异常路径：新密码为null时应抛出AuthException")
        void shouldThrowExceptionWhenNewPasswordIsNull() {
            assertThatThrownBy(() -> adminUserService.updatePassword(1L, null))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("边界条件：更新失败返回false")
        void shouldReturnFalseWhenPasswordUpdateFails() {
            when(adminUserMapper.updateById(any(AdminUser.class))).thenReturn(0);

            boolean result = adminUserService.updatePassword(1L, "newPassword123");

            assertThat(result).isFalse();
        }
    }
}