# IoT Monitoring System

Hệ thống giám sát môi trường và điều khiển thiết bị IoT (Spring Boot + MySQL + Plain HTML/CSS/JS).

## 📁 Cấu trúc dự án

```text
iot-monitoring-system/
├── backend/
│   ├── src/main/java/com/iot/
│   │   ├── config/
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── SensorController.java
│   │   │   └── DeviceController.java
│   │   ├── service/
│   │   │   ├── SensorService.java
│   │   │   └── DeviceService.java
│   │   ├── repository/
│   │   │   ├── SensorRepository.java
│   │   │   ├── DeviceRepository.java
│   │   │   ├── DataSensorRepository.java
│   │   │   └── ActiveHistoryRepository.java
│   │   ├── model/
│   │   │   ├── Sensor.java
│   │   │   ├── Device.java
│   │   │   ├── DataSensor.java
│   │   │   └── ActiveHistory.java
│   │   └── dto/
│   │       ├── DashboardResponse.java
│   │       ├── PaginationResponse.java
│   │       ├── DataSensorDTO.java
│   │       ├── ActiveHistoryDTO.java
│   │       └── DeviceStateRequest.java
│   └── src/main/resources/
│       └── application.properties
├── frontend/
│   ├── index.html
│   ├── dashboard.html
│   ├── data-sensor.html
│   ├── active-history.html
│   ├── profile.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       ├── dashboard.js
│       ├── data-sensor.js
│       └── active-history.js
└── database/
    └── schema.sql
```

## 🚀 Hướng dẫn khởi chạy

### 1. Cơ sở dữ liệu (MySQL)
1. Mở MySQL Workbench hoặc CLI.
2. Thực thi file `database/schema.sql` để tạo cơ sở dữ liệu `iot_monitoring` và các bảng ban đầu:
```bash
mysql -u root -p < database/schema.sql
```

### 2. Backend (Spring Boot)
1. Cập nhật `username` và `password` kết nối MySQL trong file `backend/src/main/resources/application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=yourpassword
```
2. Mở thư mục `backend` và khởi chạy dự án bằng Maven:
```bash
cd backend
mvn spring-boot:run
```
Backend sẽ khởi chạy tại port `8081` (`http://localhost:8081`).

## API chính

- `GET /api/sensors` và `GET /api/devices`: danh mục cảm biến, thiết bị.
- `POST /api/datasensors/ingest` với body `{ "temperature": 28.5, "humidity": 65.2, "light": 420 }`: giả lập ESP32 gửi dữ liệu qua HTTP, không yêu cầu session.
- `GET /api/datasensors` và `GET /api/datasensors/search?sensorName=&measuredAt=&value=`: dữ liệu cảm biến, có phân trang.
- `GET /api/activehistories` và `GET /api/activehistories/search?deviceName=&username=&executedAt=`: lịch sử điều khiển, có phân trang.
- `GET /api/dashboard/summary`: dữ liệu mới nhất của từng cảm biến, biểu đồ và thiết bị.
- `POST /api/devices/{id}/state` với body `{ "state": "ON" }` hoặc `{ "state": "OFF" }`: điều khiển thiết bị và ghi lịch sử cho người dùng trong session.
- `POST /api/login`, `POST /api/logout`, `GET /api/me`: xác thực dựa trên session.

Collection và environment để import trực tiếp vào Postman nằm trong thư mục `postman/`.

### 3. Frontend
- Chạy frontend bằng Live Server tại `http://localhost:5500` (ví dụ `http://localhost:5500/login.html`). Không mở trực tiếp file HTML bằng `file://`, vì session cookie giữa frontend và backend sẽ không hoạt động đúng.
