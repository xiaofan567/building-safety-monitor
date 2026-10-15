# EquipmentTypeController REST API —— 集成测试报告

> 被测接口：`com.qst.smartbuildings.controller.EquipmentTypeController`
> 测试类：`smartbuildings-backend/src/test/java/com/qst/smartbuildings/controller/EquipmentTypeControllerIntegrationTest.java`
> 生成日期：2026-09-23

---

## 1. 测试结论

| 指标 | 结果 |
| --- | --- |
| 执行结果 | **BUILD SUCCESS，全部通过** |
| 用例总数 | **16**（Failures: 0，Errors: 0，Skipped: 0） |
| 接口覆盖率 | **4 / 4 = 100%**（全部 REST 接口均被覆盖） |
| 错误码路径覆盖 | **0 / 400 / 500 全部覆盖** |

接口覆盖率与错误码覆盖均满足「不低于 90%、所有错误码路径均覆盖」的要求。

---

## 2. 接口覆盖率明细

`EquipmentTypeController` 共 4 个接口，全部覆盖：

| 方法 | REST 路径 | 用例数 | 覆盖 |
| --- | --- | --- | --- |
| queryTreeList | `POST /equipmentType/queryTreeList` | 2 | ✅ |
| get | `POST /equipmentType/get/{typeId}` | 2 | ✅ |
| save | `POST /equipmentType/save` | 6 | ✅ |
| delete | `POST /equipmentType/delete` | 4 | ✅ |

**覆盖率 = 4 / 4 = 100%**。

---

## 3. 错误码路径覆盖明细

Controller 返回/可能出现的 code 与其触发路径：

| code | HTTP 状态 | 触发场景 | 覆盖用例 | 是否覆盖 |
| --- | --- | --- | --- | --- |
| `0` (success) | 200 | 正常查询/保存/删除 | 全部正常用例 | ✅ |
| `400` (IllegalArgumentException) | 400 | get 类型不存在、save 名称/编号/body 为空、delete 类型不存在/存在下级 | 7 个 | ✅ |
| `500` (MethodArgumentNotValidException 兜底) | 500 | `delete` 请求体 `typeId=null` 触发 `@NotNull/@Valid` 校验失败 | 1 个 | ✅ |
| `500` (HttpMessageNotReadableException / 其他未捕获异常) | 500 | `save` 空请求体（非法 JSON） | 1 个 | ✅ |

`401`(AuthException)、`500`(EquipmentException) 不属于本 Controller 可达路径（Service 层不抛出），未计为必须覆盖项。

---

## 4. 用例列表

### 4.1 正常创建设备类型（save）
| # | 用例 | 断言 |
| --- | --- | --- |
| 1 | 合法新增（typeId 为 null） | HTTP 200，`code=0`，`msg=success`，`saveType` 被调用 |
| 2 | 带 typeId 修改 | HTTP 200，`code=0`，`saveType` 被调用 |

### 4.2 参数校验失败（错误码 `400`）
| # | 用例 | 断言 |
| --- | --- | --- |
| 3 | save 类型名称为空 → 抛 `IllegalArgumentException` | HTTP 400，`code=400`，`message` 含“类型名称不能为空” |
| 4 | save 类型编号为空 → 抛 `IllegalArgumentException` | HTTP 400，`code=400`，`message` 含“类型编号不能为空” |
| 5 | save 请求体 typeName/typeCode 为 null → 抛 `IllegalArgumentException` | HTTP 400，`code=400`，`message` 含“设备类型信息不能为空” |
| 6 | save 空请求体 → `HttpMessageNotReadableException` 兜底 | HTTP 500，`code=500` |
| 7 | get 类型不存在 | HTTP 400，`code=400`，`message` 含“设备类型不存在” |
| 8 | delete 类型不存在 | HTTP 400，`code=400`，`message` 含“设备类型不存在” |
| 9 | delete 存在下级类型 | HTTP 400，`code=400`，`message` 含“存在下级类型” |

### 4.3 `@Valid` 校验失败（错误码 `500`）
| # | 用例 | 断言 |
| --- | --- | --- |
| 10 | delete 请求体 `typeId=null` 触发 DTO `@NotNull` | HTTP 500，`code=500`（见下方「发现/建议」） |

### 4.4 正常查询
| # | 用例 | 断言 |
| --- | --- | --- |
| 11 | queryTreeList 有数据 | HTTP 200，`code=0`，`data` 为数组且含根节点 |
| 12 | queryTreeList 空列表 | HTTP 200，`code=0` |
| 13 | get 命中已存在类型 | HTTP 200，`code=0`，`data.typeId=1` |

### 4.5 并发重复提交（真实 Service + mock Mapper）
| # | 用例 | 断言 |
| --- | --- | --- |
| 14 | 20 线程并发提交相同 typeCode/typeName | 全部成功、无异常、`insert` 被调用 20 次 |
| 15 | 无 SecurityContext 时 saveType | 正常落库，`createUserId` 置空 |

> 注：镜头 14 反映当前实现**无唯一性去重**，并发重复提交不会被拦截（详见「发现/建议」）。

---

## 5. 测试方式与执行命令

采用 **MockMvc 装配真实 Controller + 真实全局异常处理器** 的集成方式，在 HTTP 层验证路由映射、JSON 绑定、`@Valid` 校验、`Result` 包装与错误码映射；Service 在 Controller 边界 mock，并发场景使用真实 `EquipmentTypeServiceImpl`。

由于本机网络无法访问 `maven.aliyun.com` / Maven Central，需在**离线模式**下执行：

```bash
cd smartbuildings-backend
mvn -o test \
  -Dtest=EquipmentTypeControllerIntegrationTest \
  -DfailIfNoTests=false \
  -s test-settings-offline.xml \
  -gs test-settings-offline.xml \
  -Poffline-surefire
```

> 说明：`test-settings-offline.xml` 指向本机已完整缓存的本地仓库 `~/.m2/repository` 并关闭镜像/网络；`-Poffline-surefire` 将 surefire 钉到已缓存的 `3.5.5`（默认 `3.5.4` 缺 `surefire-common` 依赖）。

最近一次执行结果摘要：

```
Tests run: 2 (queryTreeList)
Tests run: 2 (concurrency)
Tests run: 4 (delete)
Tests run: 2 (get)
Tests run: 6 (save)
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 6. 发现与建议（测试暴露出的问题）

1. **`@Valid` 校验失败返回 500 而非 400**：`EquipmentTypeDeleteDTO@NotNull` 触发 `MethodArgumentNotValidException`，但全局异常处理器 [ConfigExceptionHandler.java](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/main/java/com/qst/smartbuildings/config/ConfigExceptionHandler.java) 未单独处理，被 `@ExceptionHandler(Exception.class)` 兜底为 500。建议新增 `MethodArgumentNotValidException` 处理器返回 400。
2. **`save` 并发/重复提交无去重**：[EquipmentTypeServiceImpl.saveType](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/main/java/com/qst/smartbuildings/service/impl/EquipmentTypeServiceImpl.java) 对重复 `typeCode` 无唯一性校验（用例 14 验证 20 次并发全部落库）。建议在 `equipment_type.type_code` 加唯一索引 + 落库前查重，避免脏数据。
3. **空请求体返回 500（非 400）**：`save` 传空 body 触发 `HttpMessageNotReadableException`，同样被兜底为 500。建议对 JSON 反序列化异常单独映射为 400。

---

## 7. 相关文件

- 测试类：[EquipmentTypeControllerIntegrationTest.java](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/test/java/com/qst/smartbuildings/controller/EquipmentTypeControllerIntegrationTest.java)
- 被测控制器：[EquipmentTypeController.java](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/main/java/com/qst/smartbuildings/controller/EquipmentTypeController.java)
- 服务实现：[EquipmentTypeServiceImpl.java](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/main/java/com/qst/smartbuildings/service/impl/EquipmentTypeServiceImpl.java)
- 异常处理器：[ConfigExceptionHandler.java](file:///d:/claude/building-safety-monitor/smartbuildings-backend/src/main/java/com/qst/smartbuildings/config/ConfigExceptionHandler.java)