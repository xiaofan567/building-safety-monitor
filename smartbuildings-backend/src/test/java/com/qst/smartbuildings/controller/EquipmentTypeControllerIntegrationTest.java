package com.qst.smartbuildings.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qst.smartbuildings.config.ConfigExceptionHandler;
import com.qst.smartbuildings.entity.EquipmentType;
import com.qst.smartbuildings.mapper.EquipmentTypeMapper;
import com.qst.smartbuildings.service.EquipmentTypeService;
import com.qst.smartbuildings.service.impl.EquipmentTypeServiceImpl;
import com.qst.smartbuildings.vo.EquipmentTypeTreeVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * EquipmentTypeController REST API 集成测试
 *
 * <p>使用 MockMvc 装配真实的 {@link EquipmentTypeController} 与
 * {@link ConfigExceptionHandler}，在 HTTP 层面验证路由、JSON 绑定、@Valid 校验、
 * Result 包装以及全局异常处理器对错误码的映射。Service 层在控制器边界 mock，
 * 并发重复提交场景则使用真实的 {@link EquipmentTypeServiceImpl} 进行验证。</p>
 */
@DisplayName("EquipmentTypeController REST API 集成测试")
class EquipmentTypeControllerIntegrationTest {

    private EquipmentTypeService equipmentTypeService;
    private EquipmentTypeController controller;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        equipmentTypeService = mock(EquipmentTypeService.class);
        controller = new EquipmentTypeController(equipmentTypeService);
        objectMapper = new ObjectMapper();

        // 开启 Bean Validation，使 @Valid @RequestBody 校验在 standalone 模式下生效
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ConfigExceptionHandler())
                .setValidator(validator)
                .build();
    }

    private EquipmentType newType() {
        EquipmentType t = new EquipmentType();
        t.setTypeCode("EQ001");
        t.setTypeName("塔式起重机");
        t.setTypePid(0L);
        t.setSort(1);
        return t;
    }

    private int codeOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        return node.path("code").asInt(-1);
    }

    private String msgOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsByteArray());
        String msg = node.path("message").asText(null);
        return msg == null ? node.path("msg").asText(null) : msg;
    }

    /** 装填一条 JSON 请求体并发送 POST */
    private MvcResult sendPost(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsBytes(body)))
                .andReturn();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("/equipmentType/queryTreeList 类型树查询")
    class QueryTreeListTests {

        @Test
        @DisplayName("正常流程：返回类型树列表 code=0")
        void shouldReturnTreeWhenServiceHasData() throws Exception {
            EquipmentTypeTreeVO root = new EquipmentTypeTreeVO();
            root.setTypeId(1L);
            root.setTypeName("根节点");
            root.setTypePid(0L);
            List<EquipmentTypeTreeVO> list = Arrays.asList(root);
            when(equipmentTypeService.queryTreeList()).thenReturn(list);

            MvcResult result = mockMvc.perform(post("/equipmentType/queryTreeList"))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                    .andReturn();

            verify(equipmentTypeService).queryTreeList();
            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
            assertThat(data.isArray()).isTrue();
            assertThat(data.get(0).path("typeName").asText()).isEqualTo("根节点");
        }

        @Test
        @DisplayName("边界条件：空列表也应返回 code=0")
        void shouldReturnEmptyTreeWhenNoData() throws Exception {
            when(equipmentTypeService.queryTreeList()).thenReturn(List.of());

            mockMvc.perform(post("/equipmentType/queryTreeList"))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0));
            verify(equipmentTypeService).queryTreeList();
        }
    }

    @Nested
    @DisplayName("/equipmentType/get/{typeId} 设备类型详情查询")
    class GetTests {

        @Test
        @DisplayName("正常流程：返回类型详情 code=0")
        void shouldReturnDetail() throws Exception {
            EquipmentType t = newType();
            t.setTypeId(1L);
            when(equipmentTypeService.getById(1L)).thenReturn(t);

            MvcResult result = mockMvc.perform(post("/equipmentType/get/1"))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                    .andReturn();

            verify(equipmentTypeService).getById(1L);
            JsonNode data = objectMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
            assertThat(data.path("typeId").asLong()).isEqualTo(1L);
        }

        @Test
        @DisplayName("错误码路径：类型不存在时 Service 抛 IllegalArgumentException → HTTP 400 / code=400")
        void shouldReturn400WhenTypeNotFound() throws Exception {
            when(equipmentTypeService.getById(999L))
                    .thenThrow(new IllegalArgumentException("设备类型不存在"));

            MvcResult result = mockMvc.perform(post("/equipmentType/get/999"))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("设备类型不存在");
        }
    }

    @Nested
    @DisplayName("/equipmentType/save 新增或修改设备类型")
    class SaveTests {

        @Test
        @DisplayName("正常流程：合法新增返回 code=0")
        void shouldCreateType() throws Exception {
            EquipmentType t = newType(); // typeId 为 null → 新增

            mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsBytes(t)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0))
                    .andExpect(MockMvcResultMatchers.jsonPath("$.msg").value("success"));

            verify(equipmentTypeService).saveType(any(EquipmentType.class));
        }

        @Test
        @DisplayName("正常流程：带 typeId 修改返回 code=0")
        void shouldUpdateType() throws Exception {
            EquipmentType t = newType();
            t.setTypeId(5L);

            mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsBytes(t)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0));

            verify(equipmentTypeService).saveType(any(EquipmentType.class));
        }

        @Test
        @DisplayName("错误码路径：类型名称为空 → HTTP 400 / code=400")
        void shouldReturn400WhenNameEmpty() throws Exception {
            EquipmentType t = newType();
            t.setTypeName("");
            doThrow(new IllegalArgumentException("类型名称不能为空"))
                    .when(equipmentTypeService).saveType(any(EquipmentType.class));

            MvcResult result = mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsBytes(t)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("类型名称不能为空");
        }

        @Test
        @DisplayName("错误码路径：类型编号为空 → HTTP 400 / code=400")
        void shouldReturn400WhenCodeEmpty() throws Exception {
            EquipmentType t = newType();
            t.setTypeCode("  ");
            doThrow(new IllegalArgumentException("类型编号不能为空"))
                    .when(equipmentTypeService).saveType(any(EquipmentType.class));

            MvcResult result = mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsBytes(t)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("类型编号不能为空");
        }

        @Test
        @DisplayName("错误码路径：请求体为 null → HTTP 400 / code=400")
        void shouldReturn400WhenBodyNull() throws Exception {
            doThrow(new IllegalArgumentException("设备类型信息不能为空"))
                    .when(equipmentTypeService).saveType(any());

            EquipmentType t = newType();
            t.setTypeName(null);
            t.setTypeCode(null);

            MvcResult result = mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsBytes(t)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("设备类型信息不能为空");
        }

        @Test
        @DisplayName("错误码路径：空请求体触发 HttpMessageNotReadableException → HTTP 500 / code=500（未捕获异常兜底）")
        void shouldReturn500WhenEmptyBody() throws Exception {
            MvcResult result = mockMvc.perform(post("/equipmentType/save")
                            .contentType("application/json")
                            .content(""))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("/equipmentType/delete 删除设备类型")
    class DeleteTests {

        private String deleteBody(Long typeId) throws Exception {
            java.util.LinkedHashMap<String, Long> body = new java.util.LinkedHashMap<>();
            body.put("typeId", typeId);
            return objectMapper.writeValueAsString(body);
        }

        @Test
        @DisplayName("正常流程：合法删除返回 code=0")
        void shouldDeleteType() throws Exception {
            mockMvc.perform(post("/equipmentType/delete")
                            .contentType("application/json")
                            .content(deleteBody(7L)))
                    .andExpect(status().isOk())
                    .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(0));

            verify(equipmentTypeService).delete(7L);
        }

        @Test
        @DisplayName("错误码路径：@Valid 校验失败（typeId 为 null）→ HTTP 500 / code=500（当前异常处理器未单独处理 MethodArgumentNotValidException）")
        void shouldRejectNullTypeId() throws Exception {
            MvcResult result = mockMvc.perform(post("/equipmentType/delete")
                            .contentType("application/json")
                            .content(deleteBody(null)))
                    .andExpect(status().isInternalServerError())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(500);
            verifyNoMoreInteractions(equipmentTypeService);
        }

        @Test
        @DisplayName("错误码路径：类型不存在 → HTTP 400 / code=400")
        void shouldReturn400WhenTypeNotFound() throws Exception {
            doThrow(new IllegalArgumentException("设备类型不存在"))
                    .when(equipmentTypeService).delete(999L);

            MvcResult result = mockMvc.perform(post("/equipmentType/delete")
                            .contentType("application/json")
                            .content(deleteBody(999L)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("设备类型不存在");
        }

        @Test
        @DisplayName("错误码路径：类型下存在子类型 → HTTP 400 / code=400")
        void shouldReturn400WhenHasChildren() throws Exception {
            doThrow(new IllegalArgumentException("该类型下存在下级类型，无法删除"))
                    .when(equipmentTypeService).delete(7L);

            MvcResult result = mockMvc.perform(post("/equipmentType/delete")
                            .contentType("application/json")
                            .content(deleteBody(7L)))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            assertThat(codeOf(result)).isEqualTo(400);
            assertThat(msgOf(result)).contains("该类型下存在下级类型，无法删除");
        }
    }

    @Nested
    @DisplayName("并发重复提交边界测试（真实 EquipmentTypeServiceImpl + 模拟 mapper）")
    class ConcurrencyTests {

        private EquipmentTypeMapper mapper;
        private EquipmentTypeServiceImpl service;
        private final AtomicInteger insertCount = new AtomicInteger();

        @BeforeEach
        void setUp() {
            mapper = mock(EquipmentTypeMapper.class);
            service = new EquipmentTypeServiceImpl();
            ReflectionTestUtils.setField(service, "baseMapper", mapper);
            when(mapper.insert(any(EquipmentType.class))).then(invocation -> {
                insertCount.incrementAndGet();
                return 1;
            });

            // 模拟已登录用户 principal="1"
            Authentication auth = new TestingAuthenticationToken("1", null);
            auth.setAuthenticated(true);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        @AfterEach
        void clearSecurity() {
            SecurityContextHolder.clearContext();
        }

        @Test
        @Timeout(value = 30, unit = TimeUnit.SECONDS)
        @DisplayName("20线程并发提交相同 typeCode/typeName：全部成功、insert 被调用 20 次（当前实现无唯一性去重）")
        void concurrentDuplicateSubmitShouldNotThrow() throws Exception {
            int threads = 20;
            CountDownLatch ready = new CountDownLatch(threads);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService pool = Executors.newFixedThreadPool(threads);
            AtomicInteger failures = new AtomicInteger();

            for (int i = 0; i < threads; i++) {
                pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await(5, TimeUnit.SECONDS);
                        EquipmentType t = newType(); // 完全相同：typeCode=EQ001,typeName=塔式起重机
                        service.saveType(t);
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    }
                });
            }

            ready.await(10, TimeUnit.SECONDS);
            start.countDown();
            pool.shutdown();
            boolean finished = pool.awaitTermination(20, TimeUnit.SECONDS);

            assertThat(finished).isTrue();
            // 并发下所有调用均成功、无异常、无重复拦截
            assertThat(failures.get()).isZero();
            assertThat(insertCount.get()).isEqualTo(threads);
        }

        @Test
        @DisplayName("未登录（无 SecurityContext）时 saveType 也能正常落库，createUserId 为 null")
        void saveTypeWithNoAuthenticationUser() throws Exception {
            SecurityContextHolder.clearContext();

            service.saveType(newType());

            verify(mapper).insert(any(EquipmentType.class));
            assertThat(insertCount.get()).isEqualTo(1);
        }
    }
}