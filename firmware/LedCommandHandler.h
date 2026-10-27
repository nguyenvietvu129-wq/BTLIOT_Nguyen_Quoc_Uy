#pragma once
#include <Arduino.h>
#include <ArduinoJson.h>
#include <PubSubClient.h>

// Integrate with the existing firmware; GPIO numbers must come from your board wiring.
// Requires ArduinoJson 6/7 and PubSubClient. Call begin() once in setup().
class LedCommandHandler {
  struct Receipt { String id, code, action; bool success = false; };
  PubSubClient& mqtt;
  int pins[3];
  uint8_t onLevel;
  Receipt receipts[16];
  size_t nextReceipt = 0;

  void acknowledge(const Receipt& r) {
    StaticJsonDocument<256> ack;
    ack["commandId"] = r.id;
    ack["deviceCode"] = r.code;
    // Echo requested action even on failure; success describes GPIO execution.
    ack["status"] = r.action;
    ack["success"] = r.success;
    char buffer[256];
    serializeJson(ack, buffer, sizeof(buffer));
    mqtt.publish("iot/devices/status", buffer, false); // NEVER retained
  }

public:
  // LED1=yellow, LED2=red, LED3=green. Pass LOW for active-low boards.
  LedCommandHandler(PubSubClient& client, int yellowPin, int redPin, int greenPin,
                    uint8_t activeLevel = HIGH)
      : mqtt(client), pins{yellowPin, redPin, greenPin}, onLevel(activeLevel) {}

  void begin() {
    mqtt.setBufferSize(512);
    for (int pin : pins) {
      if (pin < 0) continue;
      pinMode(pin, OUTPUT);
      digitalWrite(pin, onLevel == HIGH ? LOW : HIGH);
    }
  }

  // Return false for unrelated topics, allowing existing callbacks to handle them.
  bool handle(char* topic, byte* payload, unsigned int length) {
    if (strcmp(topic, "iot/devices/command") != 0) return false;
    StaticJsonDocument<256> doc;
    if (deserializeJson(doc, payload, length)) return true;
    if (!doc["commandId"].is<const char*>() || !doc["deviceCode"].is<const char*>()
        || !doc["action"].is<const char*>()) return true;
    Receipt r;
    r.id = doc["commandId"].as<String>();
    r.code = doc["deviceCode"].as<String>();
    r.action = doc["action"].as<String>();
    if (!r.id.startsWith("CMD-") || r.id.length() <= 4 || r.id.length() > 23
        || (r.action != "ON" && r.action != "OFF")) return true;
    // QoS redelivery must not execute the same command twice during this boot.
    for (const Receipt& old : receipts) {
      if (old.id != r.id) continue;
      if (old.code == r.code && old.action == r.action) acknowledge(old);
      return true;
    }
    int index = r.code == "LED1" ? 0 : r.code == "LED2" ? 1 : r.code == "LED3" ? 2 : -1;
    if (index >= 0 && pins[index] >= 0) {
      uint8_t level = r.action == "ON" ? onLevel : (onLevel == HIGH ? LOW : HIGH);
      digitalWrite(pins[index], level);
      r.success = digitalRead(pins[index]) == level;
    }
    receipts[nextReceipt] = r;
    nextReceipt = (nextReceipt + 1) % 16;
    acknowledge(r);
    return true;
  }
};
