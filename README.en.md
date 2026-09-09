# Smart Construction Site Safety Monitoring System

## Project Overview

The Smart Construction Site Safety Monitoring System is a comprehensive construction safety monitoring solution that integrates AI image recognition, equipment status monitoring, real-time data acquisition, and visualization functionalities. The system adopts a separated front-end and back-end architecture. The backend is based on Spring Boot + MyBatis-Plus, the frontend is based on Vue 3 + Vite, and it includes independent AI recognition modules and device simulation servers.

## Core Features

### AI Safety Recognition
- **Intelligent Image Detection**: Real-time safety monitoring based on YOLO/Damoyolo models
- **Hazardous Behavior Identification**: Automatic detection of safety hazards in construction zones
- **Tower Crane Safety Prediction**: Risk prediction model based on time-series data

### Equipment Monitoring Management
- **Tower Crane Monitoring**: Data monitoring for operating status, wind speed, load, angle, etc.
- **Construction Elevator Monitoring**: Safety warnings for overload, overspeed, inclination, etc.
- **High-Support Formwork Monitoring**: Real-time warnings for structural deformation and stress changes
- **Deep Excavation Monitoring**: Underwater level and support structure displacement monitoring
- **Environmental Monitoring**: Environmental parameters such as PM2.5, temperature/humidity, noise, etc.

### Data Management
- **Real-time Data Acquisition**: Real-time transmission via TCP/WebSocket persistent connections
- **Historical Data Storage**: Persistent storage of monitoring data
- **Alarm Record Management**: Inquiry and processing of equipment alarm information
- **Equipment Asset Management**: Equipment profile maintenance

## Technical Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                   Frontend Presentation Layer                    │
│              Vue 3 + Vite + Tailwind CSS + ECharts                │
└─────────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                      Backend Service Layer                   │
│       Spring Boot + MyBatis-Plus + Redis + Netty TCP Server    │
│  - RESTful API                                                │
│  - Token Authentication                                       │
│  - Equipment Data Processing                                  │
└───────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                     AI Recognition Layer                     │
│           Flask + TensorFlow/Keras + YOLO                     │
│  - Image Object Detection                                     │
│  - Tower Crane Safety Prediction                              │
└───────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────▼───────────────────────────────┐
│                    Device Simulation Layer                   │
│             Node.js + WebSocket + TCP Server                  │
│  - Device Data Simulation Generation                          │
│  - Protocol Conversion Adaptation                             │
└───────────────────────────────────────────────────────────────┘
```

## Project Structure

```
building-safety-monitor/
├── AIRecognize/                    # AI Recognition Module
│   ├── app.py                      # Flask Application Entry
│   ├── detector.py                 # Image Detection Module
│   ├── model_loader.py             # Model Loading Management
│   ├── crane/                      # Tower Crane Safety Monitoring
│   │   ├── model.py                # Prediction Model Definition
│   │   ├── predict.py              # Prediction Interface
│   │   ├── predictor.py            # Predictor Class
│   │   ├── train.py                # Training Module
│   │   └── data_prep.py            # Data Preprocessing
│   └── model/                      # AI Model Files
│       └── last.pt
│
├── ConsSafetySystem/               # Frontend System
│   ├── src/views/                  # Vue Page Components
│   ├── src/api/                    # API Interface Encapsulation
│   ├── src/store/                  # Vuex State Management
│   ├── src/utils/                  # Utility Functions
│   └── src/styles/                 # Style Files
│
├── device-sim-server/              # Device Simulation Server
│   ├── server.js                   # Server Main Entry
│   ├── data-generators.js          # Data Generators
│   └── tcp-client.js               # TCP Client
│
├── smartbuildings-backend/         # Backend Service
│   ├── src/main/java/com/qst/smartbuildings/
│   │   ├── controller/             # REST Controllers
│   │   ├── entity/                 # Entity Classes
│   │   ├── service/                # Business Logic
│   │   ├── mapper/                 # Data Access
│   │   ├── tcpserver/              # TCP Server
│   │   └── config/                 # Configuration Classes
│   └── src/main/resources/         # Resource Configuration
│
└── layout-schematic.html           # Interface Layout Sketch
```

## Quick Start

### Environment Requirements

- **Node.js**: v16+
- **Python**: 3.8+
- **Java**: JDK 11+
- **MySQL**: 8.0+
- **Redis**: 6.0+

### Backend Service Deployment

1. Create database and execute initialization script:
```bash
mysql -u root -p < smartbuildings-backend/src/main/resources/db/migration/V2__init_full_db.sql
```

2. Configure database connection (`application.yml`):
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

3. Build and start:
```bash
cd smartbuildings-backend
mvn clean package -DskipTests
java -jar target/smartbuildings-*.jar
```

### AI Recognition Module Deployment

```bash
cd AIRecognize
pip install -r requirements.txt
python -m AIRecognize
```

### Frontend Deployment

```bash
cd ConsSafetySystem
npm install
npm run dev
```

### Device Simulation Service (For Development Debugging)

```bash
cd device-sim-server
npm install
node server.js
```

## API Documentation

### Authentication Interfaces

| Endpoint | Method | Description |
|------|------|------|
| `/login` | POST | User Login |
| `/logout` | POST | Logout |
| `/queryLoginUser` | POST | Get Current User Info |

### Equipment Management

| Endpoint | Method | Description |
|------|------|------|
| `/equipmentAssets/list` | POST | Query Equipment List |
| `/equipmentAssets/get/{assetId}` | POST | Query Equipment Details |
| `/equipmentAssets/save` | POST | Save Equipment |
| `/equipmentAssets/delete` | POST | Delete Equipment |

### Equipment Types

| Endpoint | Method | Description |
|------|------|------|
| `/equipmentType/queryTreeList` | POST | Query Type Tree |
| `/equipmentType/save` | POST | Save Type |
| `/equipmentType/delete` | POST | Delete Type |

### Monitoring Point Management

| Endpoint | Method | Description |
|------|------|------|
| `/equipmentMonitorPoint/list` | POST | Query Monitoring Point List |
| `/equipmentMonitorPoint/saveOrUpdate` | POST | Save Monitoring Point |
| `/equipmentMonitorPoint/delete` | POST | Delete Monitoring Point |

### Alarm Management

| Endpoint | Method | Description |
|------|------|------|
| `/monitorPointAlert/selectPage` | POST | Query Alarm Records |
| `/monitorPointAlert/update` | POST | Process Alarm |

### AI Alarms

| Endpoint | Method | Description |
|------|------|------|
| `/aiAlert/selectPage` | POST | Query AI Alarm List |
| `/aiAlert/get/{id}` | POST | Query Alarm Details |

### Cameras

| Endpoint | Method | Description |
|------|------|------|
| `/camera/list` | POST | Query Camera List |
| `/camera/update` | POST | Modify Camera Info |

## Configuration Instructions

### Device Simulation Configuration

Edit `device-sim-server/config/device-config.json`:

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

### AI Model Configuration

Configure model parameters in `AIRecognize/crane/config.py`.

## License

This project uses the [MIT License](LICENSE) open-source agreement.