package com.example.driver.service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Core Round Robin logic.
 *
 * A single AtomicInteger counts every incoming request. The request number
 * modulo the number of services gives the index of the service to use:
 *
 *   request 1 -> index 0, request 2 -> index 1, request 3 -> index 2,
 *   request 4 -> index 0, ...
 *
 * AtomicInteger makes this safe when many requests arrive at the same time.
 */
@Service
public class RoundRobinService {

    private final List<String> serviceUrls;
    private final AtomicInteger requestCounter = new AtomicInteger(0);

    public RoundRobinService(@Value("${driver.services.urls}") List<String> serviceUrls) {
        if (serviceUrls == null || serviceUrls.isEmpty()) {
            throw new IllegalStateException("driver.services.urls must contain at least one URL");
        }
        this.serviceUrls = serviceUrls.stream().map(String::trim).toList();
    }

    /** Holds the outcome of one round robin decision. */
    public record Selection(int requestNumber, int serviceNumber, String url) {
    }

    /** Picks the next service. Thread-safe. */
    public Selection next() {
        // incrementAndGet gives 1, 2, 3, ... (and wraps safely to negatives after ~2 billion)
        int requestNumber = requestCounter.incrementAndGet();
        int index = Math.floorMod(requestNumber - 1, serviceUrls.size());
        return new Selection(requestNumber, index + 1, serviceUrls.get(index));
    }

    public int getServiceCount() {
        return serviceUrls.size();
    }
}
