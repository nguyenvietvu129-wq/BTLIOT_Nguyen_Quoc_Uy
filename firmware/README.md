# Tích hợp xác nhận LED vào firmware ESP32

Source hiện tại không chứa firmware, nên đây là module tích hợp, không phải sketch thay thế toàn bộ chương trình.
Giữ nguyên Wi-Fi, DHT, cảm biến ánh sáng, lịch gửi sensor và các callback khác.

1. Cài ArduinoJson 6/7 và PubSubClient trong Arduino IDE; chép `LedCommandHandler.h` cạnh sketch.
2. Sau khai báo MQTT client hiện có, thêm:

```cpp
#include "LedCommandHandler.h"
// Dùng các hằng GPIO đúng trong sketch của bạn, không dùng số chân đoán trước.
LedCommandHandler ledCommands(client, LED_YELLOW, LED_RED, LED_GREEN);
// Nếu mạch active-low, truyền LOW ở tham số cuối.
```

3. Trong `setup()`, gọi `ledCommands.begin()` một lần. Không gọi lại khi reconnect vì sẽ reset GPIO.
4. Trong callback MQTT hiện có, chuyển tiếp topic lệnh:

```cpp
void callback(char* topic, byte* payload, unsigned int length) {
  if (ledCommands.handle(topic, payload, length)) return;
  // Giữ nguyên xử lý các topic khác ở đây.
}
```

5. Sau mỗi lần kết nối broker, subscribe `iot/devices/command` QoS 1. Giữ `client.loop()` chạy thường xuyên,
   tránh delay chặn lâu hơn timeout. Giữ nguyên việc gửi dữ liệu `temperature`, `humidity`, `light`.
6. Xóa retained command cũ trên broker trước khi triển khai nếu từng dùng retained command.
   Backend mới luôn gửi command không retained. PubSubClient không cung cấp retained flag trong callback.

7. Khi đã nạp firmware tích hợp module này, đặt `mqtt.command.format=json` trong
   `backend/src/main/resources/application.properties` rồi khởi động lại backend.
   Chế độ `text` dành cho firmware cũ nhận `LED1 ON`; không có mã lệnh nên không thể xác nhận SUCCESS.

Lệnh: `{"commandId":"CMD-123","deviceCode":"LED3","action":"ON"}`.
ACK: `{"commandId":"CMD-123","deviceCode":"LED3","status":"ON","success":true}`.
ACK không retained. Không gửi ACK thành công trước `digitalWrite`.
`digitalRead` kiểm tra mức GPIO, không chứng minh bóng LED thực sự sáng.

Module nhớ 16 lệnh gần nhất trong RAM để tránh thực hiện lại do QoS redelivery; bộ nhớ này mất khi reset.
Nếu publish ACK thất bại thì backend timeout, không tự coi là thành công.
Không dùng bản tin `{"led1":"ON",...}` cũ để xác nhận một lệnh mới.
