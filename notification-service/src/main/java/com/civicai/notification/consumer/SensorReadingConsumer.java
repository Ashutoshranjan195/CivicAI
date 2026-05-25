package com.civicai.notification.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SensorReadingConsumer {

    private static final Logger log = LoggerFactory.getLogger(SensorReadingConsumer.class);

    // Simple cache to guarantee message processing idempotency
    private final Set<String> processedReadingIds = ConcurrentHashMap.newKeySet();

    @KafkaListener(
            topics = {"${kafka.topics.air-quality:city-sensors.air-quality}", "${kafka.topics.traffic:city-sensors.traffic}"},
            groupId = "${spring.kafka.consumer.group-id:notification-group}"
    )
    public void consume(ConsumerRecord<String, Map<String, Object>> record) {
        log.info("Received event from topic: {}. Key (SensorId): {}", record.topic(), record.key());
        
        Map<String, Object> payload = record.value();
        if (payload == null) {
            log.warn("Empty payload received, ignoring.");
            return;
        }

        String readingId = (String) payload.get("readingId");
        if (readingId == null) {
            log.warn("Missing 'readingId' in payload, ignoring.");
            return;
        }

        // Idempotency check
        if (processedReadingIds.contains(readingId)) {
            log.warn("Idempotency Triggered: Reading ID {} has already been processed. Skipping.", readingId);
            return;
        }

        String sensorName = (String) payload.get("sensorName");
        String location = (String) payload.get("location");
        String type = (String) payload.get("type");
        Double value = (Double) payload.get("value");

        log.info("Processing reading: ID={}, Sensor={}, Type={}, Value={}, Location={}", 
                readingId, sensorName, type, value, location);

        // Simulate DLQ failure scenario if location is "FAIL_SIMULATION"
        if ("FAIL_SIMULATION".equalsIgnoreCase(location)) {
            log.error("Simulation Triggered: Simulating failure for reading ID: {}. Throwing exception...", readingId);
            throw new RuntimeException("Simulated notification gateway connection error!");
        }

        // Logic check: alert triggers
        if ("AIR_QUALITY".equalsIgnoreCase(type) && value > 100.0) {
            log.warn("🚨 [ALERT] High Pollutant Count detected at {}! Air Quality Value: {}. Notification sent to local EPA.", 
                    location, value);
        } else if ("TRAFFIC".equalsIgnoreCase(type) && value > 500.0) {
            log.warn("🚨 [ALERT] Heavy Congestion detected at {}! Vehicle Count: {}. Notification sent to Traffic Control.", 
                    location, value);
        } else {
            log.info("Reading within safe limits. No action needed.");
        }

        // Track processed ID
        processedReadingIds.add(readingId);
    }
}
