package com.example.service1.controller;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProcessController {

    private static final String SERVICE_NAME = "Microservice-1";

    @Value("${server.port}")
    private int port;

    @GetMapping("/process")
    public Map<String, Object> process() {
        System.out.println("[" + SERVICE_NAME + "] Request received on port " + port);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("serviceName", SERVICE_NAME);
        response.put("port", port);
        response.put("message", "Request processed by " + SERVICE_NAME);
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }
}
