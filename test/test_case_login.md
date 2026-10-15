# 登录流程测试用例

> 项目：ConsSafetySystem（建筑安全智能监控平台）
> 模块：用户登录（`src/views/Login.vue`）
> 测试账号：正确用户名/密码 = `admin` / `qst123456`

## 1. 测试概述

本测试用例覆盖登录页面的完整登录流程，包括正常登录、参数校验、认证失败、权限拦截等场景。

### 测试环境

| 项目 | 值 |
| --- | --- |
| 前端服务地址 | `VITE_APP_Address=localhost:8080` |
| 后端登录接口 | `POST http://localhost:8080/login` |
| 当前用户信息接口 | `POST http://localhost:8080/queryLoginUser` |
| 退出登录接口 | `POST http://localhost:8080/logout` |

### 登录主流程（依据源码）

1. 访问根路径 `/`，路由重定向至 `/login`。
2. 填写账号、密码，点击「登 录」或按回车。
3. 前端表单校验（`username`、`password` 均必填，trigger=blur）。
4. 调用 `POST /login`，请求体 `{ username, password }`，`Content-Type: application/json`。
5. 后端返回 `{ code:0, msg:'success', data:{ adminToken:'xxx' } }`。
6. 前端从 `res.data.adminToken` 取出 token，调 `setToken` 存入 vuex 及 `localStorage['token']`。
7. 调用 `POST /queryLoginUser` 获取用户信息，`setUserInfo` 写入 vuex 及 `localStorage['userInfo']`。
8. 跳转 `/home`；路由守卫校验 `token` 存在后放行。

> DTO 响应约定：`code === 0` 为成功；`code !== 0` 时按 `msg` 提示错误（`302` 登录失效、`1005` 跳转 welcome、其余展示 msg）。

## 2. 用例明细

### TC-LOGIN-001 正常登录（正确账号密码）

| 项 | 内容 |
| --- | --- |
| 前置条件 | 本地存储中无 `token`，访问 `/` 重定向到登录页 |
| 操作步骤 | 1. 打开登录页；2. 输入账号 `admin`；3. 输入密码 `qst123456`；4. 点击「登 录」 |
| 预期结果 | ① 登录接口返回 `code:0`，`data.adminToken` 非空；② `localStorage['token']` 已写入；③ 调用 `queryLoginUser`；④ 跳转 `/home` 成功；⑤ 顶部提示「登录成功」 |
| 优先级 | P0 |

### TC-LOGIN-002 账号为空

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 账号留空；2. 密码输入 `qst123456`；3. 触发 blur 后点击「登 录」 |
| 预期结果 | 表单校验失败，提示「请输入账号」，不发请求，不跳转 |
| 优先级 | P1 |

### TC-LOGIN-003 密码为空

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 账号输入 `admin`；2. 密码留空；3. 点击「登 录」 |
| 预期结果 | 表单校验失败，提示「请输入密码」，不发请求，不跳转 |
| 优先级 | P1 |

### TC-LOGIN-004 账号/密码均为空

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 账号、密码均留空；2. 点击「登 录」 |
| 预期结果 | 同时提示「请输入账号」「请输入密码」，不发请求 |
| 优先级 | P1 |

### TC-LOGIN-005 错误的用户名

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 输入账号 `wronguser`；2. 输入密码 `qst123456`；3. 点击「登 录」 |
| 预期结果 | 登录接口失败返回 `code !== 0` 或 HTTP 4xx，拦截器 `errorMessage(msg)` 弹出错误提示；页面不跳转；`localStorage['token']` 保持为空 |
| 优先级 | P1 |

### TC-LOGIN-006 错误的密码

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 输入账号 `admin`；2. 输入密码 `wrongpass`；3. 点击「登 录」 |
| 预期结果 | 登录失败，返回错误码/错误提示，不写入 token，不跳转 /home |
| 优先级 | P1 |

### TC-LOGIN-007 回车键触发登录

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 输入账号 `admin`；2. 输入密码 `qst123456`；3. 在密码框按回车 |
| 预期结果 | 与点击按钮一致：触发 `handleLogin`，登录成功并跳转 /home |
| 优先级 | P2 |

### TC-LOGIN-008 未登录访问受保护页面（鉴权拦截）

| 项 | 内容 |
| --- | --- |
| 前置条件 | 本地存储无 `token` |
| 操作步骤 | 1. 直接访问 `/home`（或 `/equipment-type`、`/device-simulation`、`/daily-report`） |
| 预期结果 | 路由守卫 `beforeEach` 拦截：`requiresAuth && !token` 时 `next('/login')` 重定向到登录页 |
| 优先级 | P1 |

### TC-LOGIN-009 已登录访问登录页（反向跳转）

| 项 | 内容 |
| --- | --- |
| 前置条件 | 本地存储已有合法 `token` |
| 操作步骤 | 1. 访问 `/login` |
| 预期结果 | 路由守卫检测到 `token`，重定向至 `/home`，不再展示登录表单 |
| 优先级 | P2 |

### TC-LOGIN-010 退出登录后清除凭证

| 项 | 内容 |
| --- | --- |
| 操作步骤 | 1. 以 admin 成功登录；2. 调用退出登录（`POST /logout`）并触发 `removeAuth()` |
| 预期结果 | `localStorage['token']`、`userInfo` 被清除；`axios` 默认头 `Admin-Token` 被删除；再次访问受保护页面被重定向回登录页 |
| 优先级 | P2 |

### TC-LOGIN-011 登录接口响应缺少 adminToken

| 项 | 内容 |
| --- | --- |
| 异常场景 | 后端返回 `code:0` 但 `data.adminToken` 为空/缺失 |
| 操作步骤 | 修改响应后登录，观察页面行为 |
| 预期结果 | `if (!token)` 分支触发，提示「未获取到登录凭证」，不写入 token，不跳转 |
| 优先级 | P2 |

### TC-LOGIN-012 Token 过期 / 鉴权失败（网络层 401）

| 项 | 内容 |
| --- | --- |
| 异常场景 | 携带过期/无效 token 请求导致 HTTP 401 |
| 操作步骤 | 伪造 localStorage 中 token 后访问接口 |
| 预期结果 | 响应拦截器 `error.response.status === 401` 弹确认框「登录已过期，请重新登录」；确认后清除缓存并刷新回登录页 |
| 优先级 | P1 |

### TC-LOGIN-013 后端返回登录失效码 302

| 项 | 内容 |
| --- | --- |
| 异常场景 | 接口返回 `code:302` |
| 操作步骤 | 构造 `code:302` 响应（如异地登录或会话失效） |
| 预期结果 | ① `data.extra===1`（异地登录）提示「您的账号在别处登录，可能密码泄漏」；② 否则 `clearCacheEnterLogin()` 清除 token 并 `location.reload()` |
| 优先级 | P2 |

## 3. 涉及关键文件

| 文件 | 说明 |
| --- | --- |
| `src/views/Login.vue` | 登录页 UI 与 `handleLogin` 逻辑 |
| `src/api/login.js` | 登录/获取用户信息/退出的接口封装 |
| `src/utils/request.js` | Axios 实例与请求/响应拦截器、token 注入、错误码处理 |
| `src/utils/auth.js` | `addAuth` / `removeAuth` / `getAuth` 授权管理 |
| `src/store/index.js` | vuex 状态、`setToken`/`setUserInfo` 及 localStorage 持久化 |
| `src/router/index.js` | 路由配置与 `beforeEach` 鉴权守卫 |

## 4. 用例执行优先级汇总

| 优先级 | 用例编号 |
| --- | --- |
| P0 | TC-LOGIN-001 |
| P1 | TC-LOGIN-002、003、004、005、006、008、012 |
| P2 | TC-LOGIN-007、009、010、011、013 |