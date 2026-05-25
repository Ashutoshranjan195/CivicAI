package com.civicai.sensor.controller;

import com.civicai.sensor.dto.SimulateReadingRequest;
import com.civicai.sensor.model.Sensor;
import com.civicai.sensor.model.SensorReading;
import com.civicai.sensor.repository.SensorReadingRepository;
import com.civicai.sensor.repository.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sensors")
public class SensorController {

    private static final Logger log = LoggerFactory.getLogger(SensorController.class);

    private final SensorRepository sensorRepository;
    private final SensorReadingRepository sensorReadingRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public SensorController(SensorRepository sensorRepository, 
                            SensorReadingRepository sensorReadingRepository, 
                            KafkaTemplate<String, Object> kafkaTemplate) {
        this.sensorRepository = sensorRepository;
        this.sensorReadingRepository = sensorReadingRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        log.info("Sensor service status checked");
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "Sensor Service",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/simulate")
    public ResponseEntity<Map<String, Object>> simulateReading(@RequestBody SimulateReadingRequest request) {
        log.info("Simulating reading for sensor: {}, Type: {}", request.getName(), request.getType());

        if (request.getType() == null || (!request.getType().equalsIgnoreCase("AIR_QUALITY") && 
                !request.getType().equalsIgnoreCase("TRAFFIC"))) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "error", "Bad Request",
                    "message", "Invalid sensor type. Must be 'AIR_QUALITY' or 'TRAFFIC'."
            ));
        }

        UUID sensorId = request.getSensorId() != null ? request.getSensorId() : UUID.randomUUID();
        Sensor sensor = sensorRepository.findById(sensorId).orElseGet(() -> {
            Sensor s = new Sensor(
                    sensorId,
                    request.getName() != null ? request.getName() : "Simulated " + request.getType() + " Sensor",
                    request.getLocation() != null ? request.getLocation() : "City Center Zone A",
                    request.getType().toUpperCase()
            );
            return sensorRepository.save(s);
        });

        SensorReading reading = new SensorReading(
                UUID.randomUUID(),
                sensor,
                request.getValue(),
                LocalDateTime.now()
        );
        sensorReadingRepository.save(reading);

        // Prepare Kafka Payload
        Map<String, Object> payload = Map.of(
                "readingId", reading.getId().toString(),
                "sensorId", sensor.getId().toString(),
                "sensorName", sensor.getName(),
                "location", sensor.getLocation(),
                "type", sensor.getType(),
                "value", reading.getValue().doubleValue(),
                "timestamp", reading.getTimestamp().toString()
        );

        String topic = sensor.getType().equals("AIR_QUALITY") ? "city-sensors.air-quality" : "city-sensors.traffic";
        log.info("Publishing sensor reading event to topic: {}. Payload: {}", topic, payload);
        
        kafkaTemplate.send(topic, sensor.getId().toString(), payload);

        return ResponseEntity.ok(Map.of(
                "message", "Sensor reading simulated and published successfully",
                "readingId", reading.getId(),
                "sensorId", sensor.getId(),
                "topic", topic,
                "payload", payload
        ));
    }
}
