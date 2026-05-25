package com.civicai.sensor.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class SimulateReadingRequest {
    private UUID sensorId;
    private String name;
    private String location;
    private String type; // AIR_QUALITY or TRAFFIC
    private BigDecimal value;

    public SimulateReadingRequest() {}

    // Getters and Setters
    public UUID getSensorId() {
        return sensorId;
    }

    public void setSensorId(UUID sensorId) {
        this.sensorId = sensorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }
}
