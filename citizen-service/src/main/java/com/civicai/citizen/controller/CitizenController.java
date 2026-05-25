package com.civicai.citizen.controller;

import com.civicai.citizen.model.Complaint;
import com.civicai.citizen.repository.ComplaintRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/citizens")
public class CitizenController {

    private static final Logger log = LoggerFactory.getLogger(CitizenController.class);

    private final ComplaintRepository complaintRepository;
    private final RestTemplate restTemplate;

    @Value("${services.sensor-service.url:http://sensor-service}")
    private String sensorServiceUrl;

    public CitizenController(ComplaintRepository complaintRepository, RestTemplate restTemplate) {
        this.complaintRepository = complaintRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/complaints")
    public ResponseEntity<Complaint> createComplaint(@RequestBody Complaint complaint) {
        log.info("Creating new complaint: {}", complaint.getTitle());
        Complaint saved = complaintRepository.save(complaint);
        return new ResponseEntity<>(saved, HttpStatus.CREATED);
    }

    @GetMapping("/complaints")
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        log.info("Retrieving all complaints");
        List<Complaint> complaints = complaintRepository.findAll();
        return ResponseEntity.ok(complaints);
    }

    @GetMapping("/sensors/status")
    @CircuitBreaker(name = "sensorServiceCircuitBreaker", fallbackMethod = "fallbackSensorStatus")
    public ResponseEntity<Map<String, Object>> getSensorServiceStatus() {
        log.info("Calling Sensor Service at {}/api/sensors/status", sensorServiceUrl);
        String url = sensorServiceUrl + "/api/sensors/status";
        @SuppressWarnings("unchecked")
        Map<String, Object> status = restTemplate.getForObject(url, Map.class);
        return ResponseEntity.ok(status);
    }

    public ResponseEntity<Map<String, Object>> fallbackSensorStatus(Throwable throwable) {
        log.error("Sensor Service is down! Fallback method invoked. Reason: {}", throwable.getMessage());
        return ResponseEntity.ok(Map.of(
                "status", "TEMPORARILY_UNAVAILABLE",
                "message", "Sensor service is not reachable. This is a fallback response from Citizen Service.",
                "circuitBreakerTriggered", true
        ));
    }
}
