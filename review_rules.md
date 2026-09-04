## 一、编码规范要求

### 1.1 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| 变量/参数 | 小驼峰 | `userName` |
| 全局常量 | 全大写下划线 | `MAX_RETRY_COUNT` |
| 方法 | 小驼峰，动词开头 | `getUserById()` |
| 类/接口/枚举 | 大驼峰 | `UserService`, `OrderStatus` |
| 包名 | 全小写，点分隔 | `com.example.order` |
| 实体类 | 大驼峰，与表名对应 | `User` |
| DTO/VO | 后缀 DTO/VO | `UserCreateDTO` |
| Mapper | 后缀 Mapper | `UserMapper` |
| Service | 后缀 Service / ServiceImpl | `UserServiceImpl` |
| Controller | 后缀 Controller | `UserController` |
| 配置类 | 后缀 Config | `SecurityConfig` |
| 过滤器 | 后缀 Filter | `JwtAuthenticationFilter` |
| 异常类 | 后缀 Exception | `BusinessException` |

### 1.2 代码格式

- **缩进**：4 个空格（禁用 Tab）
- **行长度**：≤120 字符（推荐 ≤100）
- **空行**：方法之间空 2 行；逻辑块前后空 1 行；类成员之间空 1 行
- **括号风格**：K&R（左括号不换行）
- **导入顺序**：Java 标准库 → 第三方库 → 项目内部包，每组内字母排序
- **注解**：类/方法/字段上的注解独占一行

### 1.3 注释规范

- **类注释**：说明职责、作者、创建时间
- **方法注释**：Javadoc 格式，说明功能、参数、返回值、异常（业务核心方法必须写）
- **复杂逻辑**：必须添加解释性注释
- **TODO 注释**：`// TODO: 责任人 - 截止日期 - 内容`
- **实体注解**：`@TableName`、`@TableId`、`@TableField` 等必须写清楚映射关系

## 二、质量审查规则

### 2.1 可读性规则

| 规则项 | 标准要求 |
|--------|----------|
| 方法长度 | ≤50 行（不含空行、注释） |
| 类长度 | ≤500 行 |
| 圈复杂度 | ≤10 |
| 嵌套深度 | ≤3 层（if/for/try） |
| 魔法数字/字符串 | 必须定义为常量或枚举 |
| 方法参数个数 | ≤5 个（超过封装为对象） |
| 代码重复 | 相同逻辑出现两次以上需抽取 |
| 空值处理 | 禁止隐式返回 null，使用 `Optional` 或 `@Nullable` |

### 2.2 安全性规则

| 审查领域 | 具体要求 |
|----------|----------|
| 输入验证 | 所有外部输入使用 `@Valid` + Jakarta Bean Validation（`@NotNull`, `@Size`, `@Pattern` 等） |
| SQL 注入 | 使用 `LambdaQueryWrapper`，禁用 `${}` 拼接；动态表/字段名必须白名单校验 |
| 认证授权 | Spring Security + JWT；接口使用 `@PreAuthorize`，禁止硬编码权限 |
| 密码存储 | `BCryptPasswordEncoder`（强度 ≥10） |
| 敏感数据 | 日志脱敏（手机号、身份证等）；生产环境强制 HTTPS |
| 会话管理 | JWT 过期时间：access ≤30min，refresh ≤7d；禁止 URL 传递 token |
| CSRF | 前后端分离可全局禁用 CSRF，但 JWT 需存 httpOnly cookie |
| CORS | 仅允许具体域名，禁止 `*` 与 `allowCredentials(true)` 同时使用 |
| 异常处理 | 全局异常不暴露堆栈、数据库细节；返回统一格式 `R` |
| 文件上传 | 白名单校验类型、限制大小（≤10MB）、重命名保存 |

### 2.3 性能规则

| 规则项 | 标准要求 |
|--------|----------|
| N+1 查询 | 杜绝循环内单条查询，使用 `selectBatchIds`、`JOIN` 或批量操作 |
| 分页 | 必须用 MyBatis-Plus 分页插件 |
| 索引 | `WHERE`/`ORDER BY`/`JOIN` 字段必有索引；审查 Explain 结果 |
| 缓存 | 高频读低变更数据使用 Spring Cache（Redis/Caffeine），防穿透/雪崩 |
| 算法复杂度 | ≤O(n log n)，避免循环内远程调用或数据库查询 |
| 资源释放 | IO/连接等使用 try-with-resources 或 finally 关闭 |
| 并发 | 共享变量使用 `Atomic`、`ConcurrentHashMap`、`Lock`；Controller 中不直接操作线程 |
| 事务 | `@Transactional` 控制粒度，避免长事务；只读事务设置 `readOnly = true` |
| 连接池 | HikariCP：maximumPoolSize 10~20，connectionTimeout ≤30s |

### 2.4 技术栈特定规则

| 技术 | 审查项 |
|------|--------|
| JDK 17 | 推荐 Record 定义不可变 DTO；使用 `LocalDateTime` 替代 `Date`；启用 switch 模式匹配 |
| SpringBoot 3.3 | 使用 Jakarta EE 命名空间（`jakarta.*`）；自动配置文件改为 `META-INF/spring/...imports` |
| MyBatis-Plus | Mapper 继承 `BaseMapper`；Service 继承 `IService`/`ServiceImpl`；逻辑删除 `@TableLogic`；乐观锁 `@Version` |
| Spring Security 6 | 使用 Lambda DSL 配置；BCryptPasswordEncoder；JWT 过滤器轻量化 |
| MySQL | InnoDB + utf8mb4；字段尽量 `NOT NULL`；禁止 `select *` |

## 三、输出格式要求

审查报告必须包含：总体质量评估 + 问题分类统计 + 详细问题列表（不含代码示例）。

### 3.1 总体质量评估（文本块）
```
整体质量评估
审查时间: 2026-04-24 14:30:00
审查文件数: 28
发现问题数: 18
质量等级: 优秀 / 良好 / 一般 / 较差
风险等级: 高危 / 中危 / 低危
```
### 3.2 问题分类统计（表格）

| 问题类型 | 后端数量 | 合计 | 占比 | 趋势 |
|----------|----------|------|------|------|
| 可读性问题 | 8 | 8 | 44.4% | ↓5% |
| 安全问题 | 5 | 5 | 27.8% | ↑3% |
| 性能问题 | 3 | 3 | 16.7% | 持平 |
| 其他问题 | 2 | 2 | 11.1% | ↓2% |

### 3.3 详细问题列表（每条含以下字段）

- **问题类型**: [可读性/安全性/性能/其他]
- **严重程度**: [高/中/低]
- **文件位置**: `文件路径:行号`
- **问题描述**: 具体说明
- **修复建议**: 改进措施

> 注：不需要提供代码示例和修复示例。

## 附录：审查工具链建议

| 类别 | 工具 | 配置 |
|------|------|------|
| 静态分析 | SonarQube / SpotBugs | 启用安全规则（如 SQL 注入） |
| 格式化 | Spotless (Google Java Format) | 提交前自动格式化 |
| SQL 监控 | p6spy + MyBatis-Plus 日志 | 开发环境慢 SQL >200ms |
| 安全扫描 | OWASP Dependency-Check | 每月执行 |
| 性能压测 | JMeter | 核心接口 QPS ≥1000，P99 ≤200ms |