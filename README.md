

# 智慧工地安全监测系统 (Building Safety Monitor)

## 项目简介

智慧工地安全监测系统是一个综合性的建筑施工安全监控解决方案，集成了 AI 图像识别、设备状态监测、实时数据采集和可视化展示等功能。系统采用前后端分离架构，后端基于 Spring Boot + MyBatis-Plus，前端基于 Vue 3 + Vite，并包含独立的 AI 识别模块和设备模拟服务器。

## 核心功能

### AI 安全识别
- **智能图像检测**：基于 YOLO/Damoyolo 模型的实时安全监测
- **危险行为识别**：自动检测施工区域的安全隐患
- **吊塔安全预测**：基于时序数据的风险预测模型

### 设备监测管理
- **塔式起重机监测**：运行状态、风速、载重、角度等数据监控
- **施工升降机监测**：超重、超速、倾斜等安全预警
- **高支模监测**：结构变形、应力变化实时预警
- **深基坑监测**：地下水位、支护结构位移监测
- **环境监测**：PM2.5、温湿度、噪音等环境参数

### 数据管理
- **实时数据采集**：TCP/WebSocket 长连接实时传输
- **历史数据存储**：监测数据持久化存储
- **报警记录管理**：设备报警信息查询与处理
- **设备资产管理**：设备档案维护

## 技术架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端展示层                                │
│  Vue 3 + Vite + Tailwind CSS + ECharts                          │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        后端服务层                              │
│  Spring Boot + MyBatis-Plus + Redis + Netty TCP Server        │
│  - RESTful API                                                │
│  - Token 认证鉴权                                             │
│  - 设备数据处理                                               │
└───────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        AI 识别层                              │
│  Flask + TensorFlow/Keras + YOLO                              │
│  - 图像目标检测                                               │
│  - 吊塔安全预测                                               │
└───────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                        设备模拟层                             │
│  Node.js + WebSocket + TCP Server                             │
│  - 设备数据模拟生成                                           │
│  - 协议转换适配                                               │
└───────────────────────────────────────────────────────────────┘
```

## 项目结构

```
building-safety-monitor/
├── AIRecognize/                    # AI 识别模块
│   ├── app.py                      # Flask 应用入口
│   ├── detector.py                 # 图像检测模块
│   ├── model_loader.py             # 模型加载管理
│   ├── crane/                      # 吊塔安全监测
│   │   ├── model.py                # 预测模型定义
│   │   ├── predict.py              # 预测接口
│   │   ├── predictor.py            # 预测器类
│   │   ├── train.py                # 训练模块
│   │   └── data_prep.py            # 数据预处理
│   └── model/                      # AI 模型文件
│       └── last.pt
│
├── ConsSafetySystem/               # 前端系统
│   ├── src/views/                  # Vue 页面组件
│   ├── src/api/                    # API 接口封装
│   ├── src/store/                  # Vuex 状态管理
│   ├── src/utils/                  # 工具函数
│   └── src/styles/                 # 样式文件
│
├── device-sim-server/              # 设备模拟服务器
│   ├── server.js                   # 服务主入口
│   ├── data-generators.js          # 数据生成器
│   └── tcp-client.js               # TCP 客户端
│
├── smartbuildings-backend/         # 后端服务
│   ├── src/main/java/com/qst/smartbuildings/
│   │   ├── controller/             # REST 控制器
│   │   ├── entity/                 # 实体类
│   │   ├── service/                # 业务逻辑
│   │   ├── mapper/                 # 数据访问
│   │   ├── tcpserver/              # TCP 服务器
│   │   └── config/                 # 配置类
│   └── src/main/resources/         # 资源配置
│
└── layout-schematic.html           # 界面布局草图
```

## 快速开始

### 环境要求

- **Node.js**: v16+
- **Python**: 3.8+
- **Java**: JDK 11+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### 后端服务部署

1. 创建数据库并执行初始化脚本：
```bash
mysql -u root -p < smartbuildings-backend/src/main/resources/db/migration/V2__init_full_db.sql
```

2. 配置数据库连接（`application.yml`）：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/smartbuildings
    username: your_username
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379
```

3. 构建并启动：
```bash
cd smartbuildings-backend
mvn clean package -DskipTests
java -jar target/smartbuildings-*.jar
```

### AI 识别模块部署

```bash
cd AIRecognize
pip install -r requirements.txt
python -m AIRecognize
```

### 前端部署

```bash
cd ConsSafetySystem
npm install
npm run dev
```

### 设备模拟服务（开发调试用）

```bash
cd device-sim-server
npm install
node server.js
```

## API 文档

### 认证接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/login` | POST | 用户登录 |
| `/logout` | POST | 退出登录 |
| `/queryLoginUser` | POST | 获取当前用户信息 |

### 设备管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/equipmentAssets/list` | POST | 查询设备列表 |
| `/equipmentAssets/get/{assetId}` | POST | 查询设备详情 |
| `/equipmentAssets/save` | POST | 保存设备 |
| `/equipmentAssets/delete` | POST | 删除设备 |

### 设备类型

| 接口 | 方法 | 说明 |
|------|------|------|
| `/equipmentType/queryTreeList` | POST | 查询类型树 |
| `/equipmentType/save` | POST | 保存类型 |
| `/equipmentType/delete` | POST | 删除类型 |

### 监测点管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/equipmentMonitorPoint/list` | POST | 查询监测点列表 |
| `/equipmentMonitorPoint/saveOrUpdate` | POST | 保存监测点 |
| `/equipmentMonitorPoint/delete` | POST | 删除监测点 |

### 报警管理

| 接口 | 方法 | 说明 |
|------|------|------|
| `/monitorPointAlert/selectPage` | POST | 查询报警记录 |
| `/monitorPointAlert/update` | POST | 处理报警 |

### AI 报警

| 接口 | 方法 | 说明 |
|------|------|------|
| `/aiAlert/selectPage` | POST | 查询 AI 报警列表 |
| `/aiAlert/get/{id}` | POST | 查询报警详情 |

### 摄像头

| 接口 | 方法 | 说明 |
|------|------|------|
| `/camera/list` | POST | 查询摄像头列表 |
| `/camera/update` | POST | 修改摄像头信息 |

## 配置说明

### 设备模拟配置

编辑 `device-sim-server/config/device-config.json`：

```json
{
  "devices": [
    {
      "id": "tower_crane_01",
      "type": "tower_crane",
      "enabled": true
    }
  ]
}
```

### AI 模型配置

在 `AIRecognize/crane/config.py` 中配置模型参数。

## 许可证

本项目采用 [MIT License](LICENSE) 开源协议。