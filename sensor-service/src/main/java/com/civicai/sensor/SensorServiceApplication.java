package com.civicai.sensor;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@SpringBootApplication
@EnableDiscoveryClient
public class SensorServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SensorServiceApplication.class, args);
    }

    @Bean
    public NewTopic airQualityTopic() {
        return TopicBuilder.name("city-sensors.air-quality")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic trafficTopic() {
        return TopicBuilder.name("city-sensors.traffic")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
