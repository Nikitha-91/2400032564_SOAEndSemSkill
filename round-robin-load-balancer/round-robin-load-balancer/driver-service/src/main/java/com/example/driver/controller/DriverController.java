package com.example.driver.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.driver.service.ForwardingService;
import com.example.driver.service.ForwardingService.Result;

@RestController
@RequestMapping("/driver")
public class DriverController {

    private final ForwardingService forwardingService;

    public DriverController(ForwardingService forwardingService) {
        this.forwardingService = forwardingService;
    }

    /** Every call is forwarded to the next microservice in Round Robin order. */
    @GetMapping("/request")
    public ResponseEntity<Map<String, Object>> handleRequest() {
        Result result = forwardingService.forward();
        return ResponseEntity.status(result.status()).body(result.body());
    }
}
