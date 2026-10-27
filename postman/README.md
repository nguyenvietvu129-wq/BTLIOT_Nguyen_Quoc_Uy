# Test API bằng Postman

1. Chạy MySQL và thực thi `database/schema.sql` nếu chưa có database.
2. Trong thư mục `backend`, chạy `mvn spring-boot:run`.
3. Import hai file JSON trong thư mục này vào Postman và chọn environment **IoT Monitoring - Local**.
4. Chạy request **Login** trước các API được bảo vệ. Postman tự giữ cookie `JSESSIONID`.

Tài khoản mặc định được tạo khi backend khởi động: `admin` / `123456`.

## API

| Method | Endpoint | Xác thực | Mục đích |
|---|---|---|---|
| POST | `/api/login` | Không | Đăng nhập và tạo session |
| GET | `/api/me` | Có | Kiểm tra session |
| POST | `/api/logout` | Không | Đăng xuất |
| GET | `/api/sensors` | Có | Danh sách cảm biến |
| GET | `/api/devices` | Có | Danh sách thiết bị |
| POST | `/api/datasensors/ingest` | Không | Giả lập gói telemetry từ ESP32 |
| GET | `/api/datasensors` | Có | Dữ liệu cảm biến phân trang |
| GET | `/api/datasensors/search` | Có | Tìm dữ liệu cảm biến |
| GET | `/api/dashboard/summary` | Có | Dữ liệu dashboard |
| POST | `/api/devices/{id}/state` | Có | Gửi lệnh `ON`/`OFF`, trả HTTP 202 |
| GET | `/api/activehistories/{id}` | Có | Poll kết quả lệnh |
| GET | `/api/activehistories` | Có | Lịch sử điều khiển |
| GET | `/api/activehistories/search` | Có | Tìm lịch sử điều khiển |

Lệnh thiết bị cần MQTT broker và ESP32 để chuyển từ `LOADING` sang `SUCCESS`. Nếu broker/ESP32 không hoạt động, kết quả sẽ là `FAILURE` sau timeout.
