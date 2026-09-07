package com.qst.smartbuildings.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qst.smartbuildings.entity.AdminUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminUserMapper 单元测试")
class AdminUserMapperTest {

    @Mock
    private AdminUserMapper adminUserMapper;

    private AdminUser testUser;

    @BeforeEach
    void setUp() {
        testUser = new AdminUser();
        testUser.setUserId(1L);
        testUser.setUsername("admin");
        testUser.setPassword("encoded_password");
        testUser.setRealname("管理员");
        testUser.setStatus(1);
    }

    @Nested
    @DisplayName("insert 方法测试")
    class InsertTests {

        @Test
        @DisplayName("正常流程：插入用户返回影响行数1")
        void shouldInsertUserSuccessfully() {
            when(adminUserMapper.insert(testUser)).thenReturn(1);

            int result = adminUserMapper.insert(testUser);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("边界条件：插入失败返回0")
        void shouldReturnZeroWhenInsertFails() {
            when(adminUserMapper.insert(testUser)).thenReturn(0);

            int result = adminUserMapper.insert(testUser);

            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("selectById 方法测试")
    class SelectByIdTests {

        @Test
        @DisplayName("正常流程：根据有效ID返回用户")
        void shouldReturnUserWhenValidId() {
            when(adminUserMapper.selectById(1L)).thenReturn(testUser);

            AdminUser result = adminUserMapper.selectById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(1L);
            assertThat(result.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("边界条件：不存在的ID返回null")
        void shouldReturnNullWhenIdNotFound() {
            when(adminUserMapper.selectById(999L)).thenReturn(null);

            AdminUser result = adminUserMapper.selectById(999L);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("selectOne 方法测试")
    class SelectOneTests {

        @Test
        @DisplayName("正常流程：根据用户名查询返回用户")
        void shouldReturnUserByUsername() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(testUser);

            LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdminUser::getUsername, "admin");
            AdminUser result = adminUserMapper.selectOne(wrapper);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("admin");
        }

        @Test
        @DisplayName("边界条件：无匹配结果返回null")
        void shouldReturnNullWhenNoMatch() {
            when(adminUserMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

            LambdaQueryWrapper<AdminUser> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(AdminUser::getUsername, "unknown");
            AdminUser result = adminUserMapper.selectOne(wrapper);

            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("selectList 方法测试")
    class SelectListTests {

        @Test
        @DisplayName("正常流程：返回所有用户列表")
        void shouldReturnAllUsers() {
            AdminUser user2 = new AdminUser();
            user2.setUserId(2L);
            user2.setUsername("user2");
            List<AdminUser> users = Arrays.asList(testUser, user2);
            when(adminUserMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(users);

            List<AdminUser> result = adminUserMapper.selectList(new LambdaQueryWrapper<>());

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getUsername()).isEqualTo("admin");
            assertThat(result.get(1).getUsername()).isEqualTo("user2");
        }

        @Test
        @DisplayName("边界条件：无数据时返回空列表")
        void shouldReturnEmptyListWhenNoData() {
            when(adminUserMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            List<AdminUser> result = adminUserMapper.selectList(new LambdaQueryWrapper<>());

            assertThat(result).isNotNull();
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("selectPage 方法测试")
    class SelectPageTests {

        @Test
        @DisplayName("正常流程：分页查询返回Page对象")
        void shouldReturnPage() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            expectedPage.setRecords(Arrays.asList(testUser));
            expectedPage.setTotal(1);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserMapper.selectPage(
                    new Page<>(1, 10), new LambdaQueryWrapper<>());

            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            assertThat(result.getRecords()).hasSize(1);
        }

        @Test
        @DisplayName("边界条件：分页查询无数据返回空记录")
        void shouldReturnEmptyPageWhenNoData() {
            Page<AdminUser> expectedPage = new Page<>(1, 10);
            expectedPage.setRecords(Collections.emptyList());
            expectedPage.setTotal(0);
            when(adminUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                    .thenReturn(expectedPage);

            Page<AdminUser> result = adminUserMapper.selectPage(
                    new Page<>(1, 10), new LambdaQueryWrapper<>());

            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(0);
            assertThat(result.getRecords()).isEmpty();
        }
    }

    @Nested
    @DisplayName("updateById 方法测试")
    class UpdateByIdTests {

        @Test
        @DisplayName("正常流程：更新成功返回影响行数1")
        void shouldReturnOneWhenUpdateSuccess() {
            when(adminUserMapper.updateById(testUser)).thenReturn(1);

            int result = adminUserMapper.updateById(testUser);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("边界条件：更新失败返回0")
        void shouldReturnZeroWhenUpdateFails() {
            AdminUser nonexistentUser = new AdminUser();
            nonexistentUser.setUserId(999L);
            when(adminUserMapper.updateById(nonexistentUser)).thenReturn(0);

            int result = adminUserMapper.updateById(nonexistentUser);

            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("deleteById 方法测试")
    class DeleteByIdTests {

        @Test
        @DisplayName("正常流程：删除成功返回影响行数1")
        void shouldReturnOneWhenDeleteSuccess() {
            when(adminUserMapper.deleteById(1L)).thenReturn(1);

            int result = adminUserMapper.deleteById(1L);

            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("边界条件：删除不存在ID返回0")
        void shouldReturnZeroWhenDeleteNonexistentId() {
            when(adminUserMapper.deleteById(999L)).thenReturn(0);

            int result = adminUserMapper.deleteById(999L);

            assertThat(result).isEqualTo(0);
        }
    }
}