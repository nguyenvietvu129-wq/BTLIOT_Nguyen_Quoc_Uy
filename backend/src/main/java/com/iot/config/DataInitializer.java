// backend/src/main/java/com/iot/config/DataInitializer.java
package com.iot.config;

import com.iot.model.*;
import com.iot.repository.*;
import com.iot.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Random;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final DataSensorRepository dataSensorRepository;
    private final AuthService authService;

    @Override
    public void run(String... args) throws Exception {
        // Khởi tạo user mặc định nếu chưa có
        authService.createUserIfNotExist("admin", "123456");

        // Khởi tạo Sensor nếu bảng rỗng (dùng khi chạy lần đầu với DB mới)
        if (sensorRepository.count() == 0) {
            Sensor s1 = new Sensor();
            s1.setName("Cảm biến nhiệt độ");
            s1.setUnit("°C");
            sensorRepository.save(s1);

            Sensor s2 = new Sensor();
            s2.setName("Cảm biến độ ẩm");
            s2.setUnit("%");
            sensorRepository.save(s2);

            Sensor s3 = new Sensor();
            s3.setName("Cảm biến ánh sáng");
            s3.setUnit("lux");
            sensorRepository.save(s3);
        }

        // Khởi tạo Device nếu bảng rỗng
        if (deviceRepository.count() == 0) {
            Device d1 = new Device();
            d1.setName("LED xanh");
            d1.setType("LED");
            d1.setCurrentStatus("OFF");
            deviceRepository.save(d1);

            Device d2 = new Device();
            d2.setName("LED đỏ");
            d2.setType("LED");
            d2.setCurrentStatus("OFF");
            deviceRepository.save(d2);

            Device d3 = new Device();
            d3.setName("LED vàng");
            d3.setType("LED");
            d3.setCurrentStatus("OFF");
            deviceRepository.save(d3);
        }

        // Seed dữ liệu DataSensor mẫu nếu bảng rỗng
        if (dataSensorRepository.count() == 0) {
            Random random = new Random();
            // Lấy danh sách sensor đã có
            java.util.List<Sensor> sensors = sensorRepository.findAll();
            if (!sensors.isEmpty()) {
                LocalDateTime now = LocalDateTime.now().minusMinutes(30);
                // Seed 10 bản ghi luân phiên cho 3 sensor
                for (int i = 0; i < 10; i++) {
                    for (Sensor sensor : sensors) {
                        DataSensor ds = new DataSensor();
                        ds.setSensorsId(sensor.getId());
                        // Giá trị ngẫu nhiên tùy theo loại cảm biến
                        if (sensor.getName().contains("nhiệt")) {
                            ds.setValue(24.0f + random.nextFloat() * 8.0f);
                        } else if (sensor.getName().contains("ẩm")) {
                            ds.setValue(50.0f + random.nextFloat() * 30.0f);
                        } else {
                            ds.setValue(200.0f + random.nextFloat() * 600.0f);
                        }
                        ds.setMeasuredAt(now.plusMinutes(i * 3L));
                        dataSensorRepository.save(ds);
                    }
                }
            }
        }
    }
}
