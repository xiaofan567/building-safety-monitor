package com.qst.smartbuildings.controller;

import com.qst.smartbuildings.common.AuthConst;
import com.qst.smartbuildings.common.AuthorizationCodeEnum;
import com.qst.smartbuildings.dto.LoginDTO;
import com.qst.smartbuildings.entity.AdminUser;
import com.qst.smartbuildings.entity.Result;
import com.qst.smartbuildings.exception.AuthException;
import com.qst.smartbuildings.service.AdminUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserController 单元测试")
class AdminUserControllerTest {

    @Mock
    private AdminUserService adminUserService;

    @InjectMocks
    private AdminUserController adminUserController;

    private LoginDTO validLoginDTO;
    private AdminUser mockUser;
    private Map<String, Object> loginResultMap;

    @BeforeEach
    void setUp() {
        validLoginDTO = new LoginDTO();
        validLoginDTO.setUsername("admin");
        validLoginDTO.setPassword("123456");

        mockUser = new AdminUser();
        mockUser.setUserId(1L);
        mockUser.setUsername("admin");
        mockUser.setRealname("管理员");
        mockUser.setStatus(1);

        loginResultMap = new HashMap<>();
        loginResultMap.put("adminToken", "test-token-abc123");
    }

    @Nested
    @DisplayName("login 接口测试")
    class LoginTests {

        @Test
        @DisplayName("正常流程：有效用户名密码应返回Token")
        void shouldReturnTokenWhenValidCredentials() {
            when(adminUserService.login("admin", "123456")).thenReturn(loginResultMap);

            Result<Map<String, Object>> result = adminUserController.login(validLoginDTO);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getMsg()).isEqualTo("success");
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData()).containsKey("adminToken");
            assertThat(result.getData().get("adminToken")).isEqualTo("test-token-abc123");
            verify(adminUserService).login("admin", "123456");
        }

        @Test
        @DisplayName("异常路径：用户名不存在时应抛出AuthException")
        void shouldThrowAuthExceptionWhenUserNotFound() {
            when(adminUserService.login("unknown", "123456"))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER));

            LoginDTO dto = new LoginDTO();
            dto.setUsername("unknown");
            dto.setPassword("123456");

            assertThatThrownBy(() -> adminUserController.login(dto))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("异常路径：用户名为空时应抛出AuthException")
        void shouldThrowAuthExceptionWhenUsernameEmpty() {
            when(adminUserService.login("", "123456"))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED));

            LoginDTO dto = new LoginDTO();
            dto.setUsername("");
            dto.setPassword("123456");

            assertThatThrownBy(() -> adminUserController.login(dto))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USERNAME_REQUIRED);
        }

        @Test
        @DisplayName("异常路径：密码错误时应抛出AuthException")
        void shouldThrowAuthExceptionWhenWrongPassword() {
            when(adminUserService.login("admin", "wrongpass"))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER));

            LoginDTO dto = new LoginDTO();
            dto.setUsername("admin");
            dto.setPassword("wrongpass");

            assertThatThrownBy(() -> adminUserController.login(dto))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN_NO_USER);
        }

        @Test
        @DisplayName("异常路径：账户被禁用时应抛出AuthException")
        void shouldThrowAuthExceptionWhenAccountDisabled() {
            when(adminUserService.login("admin", "123456"))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR));

            assertThatThrownBy(() -> adminUserController.login(validLoginDTO))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_USER_DISABLE_ERROR);
        }
    }

    @Nested
    @DisplayName("logout 接口测试")
    class LogoutTests {

        @Test
        @DisplayName("正常流程：有效Token应成功登出")
        void shouldLogoutSuccessfullyWithValidToken() {
            String token = "test-token-abc123";

            Result<?> result = adminUserController.logout(token);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getMsg()).isEqualTo("success");
            verify(adminUserService).logout(token);
        }

        @Test
        @DisplayName("正常流程：空Token也应调用logout方法")
        void shouldCallLogoutWithEmptyToken() {
            String token = "";

            Result<?> result = adminUserController.logout(token);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(0);
            verify(adminUserService).logout("");
        }

        @Test
        @DisplayName("异常路径：Service层抛出异常应向上传播")
        void shouldPropagateExceptionWhenServiceThrows() {
            String token = "invalid-token";
            doThrow(new RuntimeException("Redis连接失败"))
                    .when(adminUserService).logout(token);

            assertThatThrownBy(() -> adminUserController.logout(token))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("Redis连接失败");
        }
    }

    @Nested
    @DisplayName("queryLoginUser 接口测试")
    class QueryLoginUserTests {

        @Test
        @DisplayName("正常流程：有效Token应返回用户信息")
        void shouldReturnUserInfoWhenValidToken() {
            String token = "test-token-abc123";
            when(adminUserService.queryLoginUser(token)).thenReturn(mockUser);

            Result<AdminUser> result = adminUserController.queryLoginUser(token);

            assertThat(result).isNotNull();
            assertThat(result.getCode()).isEqualTo(0);
            assertThat(result.getMsg()).isEqualTo("success");
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().getUserId()).isEqualTo(1L);
            assertThat(result.getData().getUsername()).isEqualTo("admin");
            assertThat(result.getData().getRealname()).isEqualTo("管理员");
            verify(adminUserService).queryLoginUser(token);
        }

        @Test
        @DisplayName("异常路径：Token无效时应抛出AuthException")
        void shouldThrowAuthExceptionWhenTokenInvalid() {
            String token = "expired-token";
            when(adminUserService.queryLoginUser(token))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN));

            assertThatThrownBy(() -> adminUserController.queryLoginUser(token))
                    .isInstanceOf(AuthException.class)
                    .extracting("resultCode")
                    .isEqualTo(AuthorizationCodeEnum.AUTHORIZATION_LOGIN);
        }

        @Test
        @DisplayName("边界条件：Redis返回null时应抛出AuthException")
        void shouldThrowAuthExceptionWhenRedisReturnsNull() {
            String token = "null-token";
            when(adminUserService.queryLoginUser(token))
                    .thenThrow(new AuthException(AuthorizationCodeEnum.AUTHORIZATION_LOGIN));

            assertThatThrownBy(() -> adminUserController.queryLoginUser(token))
                    .isInstanceOf(AuthException.class);
        }
    }
}