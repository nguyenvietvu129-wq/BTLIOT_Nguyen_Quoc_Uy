CREATE DATABASE IF NOT EXISTS iot_monitoring
    CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE iot_monitoring;

CREATE TABLE IF NOT EXISTS `user` (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS sensor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS device (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(100),
    current_status VARCHAR(50) NOT NULL DEFAULT 'OFF'
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS datasensor (
    id INT AUTO_INCREMENT PRIMARY KEY,
    sensors_id INT NOT NULL,
    value FLOAT NOT NULL,
    measured_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_datasensor_sensor FOREIGN KEY (sensors_id) REFERENCES sensor(id),
    INDEX idx_datasensor_time (measured_at),
    INDEX idx_datasensor_sensor_time (sensors_id, measured_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS activehistory (
    id INT AUTO_INCREMENT PRIMARY KEY,
    device_id INT NOT NULL,
    user_id INT NOT NULL,
    action VARCHAR(255) NOT NULL,
    status VARCHAR(50),
    executed_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_activehistory_device FOREIGN KEY (device_id) REFERENCES device(id),
    CONSTRAINT fk_activehistory_user FOREIGN KEY (user_id) REFERENCES `user`(id),
    INDEX idx_activehistory_time (executed_at),
    INDEX idx_activehistory_device_time (device_id, executed_at),
    INDEX idx_activehistory_user_time (user_id, executed_at)
) ENGINE=InnoDB;

-- Dữ liệu mẫu và tài khoản admin được DataInitializer tạo khi backend khởi động.
