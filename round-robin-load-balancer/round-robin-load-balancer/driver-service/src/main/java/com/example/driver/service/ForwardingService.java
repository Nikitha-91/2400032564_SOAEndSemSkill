package com.example.driver.service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.example.driver.service.RoundRobinService.Selection;

/**
 * Picks a service with Round Robin, forwards the request to it and builds
 * the response sent back to the client.
 */
@Service
public class ForwardingService {

    private final RoundRobinService roundRobinService;
    private final RestTemplate restTemplate;

    public ForwardingService(RoundRobinService roundRobinService, RestTemplate restTemplate) {
        this.roundRobinService = roundRobinService;
        this.restTemplate = restTemplate;
    }

    /** Result wrapper so the controller can set the right HTTP status. */
    public record Result(HttpStatus status, Map<String, Object> body) {
    }

    @SuppressWarnings("unchecked")
    public Result forward() {
        Selection selection = roundRobinService.next();
        String serviceName = "Microservice-" + selection.serviceNumber();
        String targetUrl = selection.url() + "/process";

        System.out.printf("[Driver] Request #%d -> %s (%s)%n",
                selection.requestNumber(), serviceName, selection.url());

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("requestNumber", selection.requestNumber());
        body.put("selectedService", serviceName);

        try {
            Map<String, Object> serviceResponse = restTemplate.getForObject(targetUrl, Map.class);

            body.put("port", serviceResponse != null ? serviceResponse.get("port") : null);
            body.put("message", "Request successfully processed");
            body.put("serviceResponse", serviceResponse);
            body.put("timestamp", LocalDateTime.now().toString());
            return new Result(HttpStatus.OK, body);

        } catch (ResourceAccessException e) {
            // Connection refused / timeout -> the service is down or unreachable
            System.err.printf("[Driver] Request #%d FAILED: %s is unavailable (%s)%n",
                    selection.requestNumber(), serviceName, e.getMessage());

            body.put("port", extractPort(selection.url()));
            body.put("message", serviceName + " is currently unavailable. Please try again.");
            body.put("error", "SERVICE_UNAVAILABLE");
            body.put("timestamp", LocalDateTime.now().toString());
            return new Result(HttpStatus.SERVICE_UNAVAILABLE, body);

        } catch (RestClientException e) {
            // The service answered, but with an error (4xx / 5xx) or an unreadable body
            System.err.printf("[Driver] Request #%d FAILED: %s returned an error (%s)%n",
                    selection.requestNumber(), serviceName, e.getMessage());

            body.put("port", extractPort(selection.url()));
            body.put("message", serviceName + " returned an error while processing the request.");
            body.put("error", "BAD_GATEWAY");
            body.put("timestamp", LocalDateTime.now().toString());
            return new Result(HttpStatus.BAD_GATEWAY, body);
        }
    }

    private Integer extractPort(String url) {
        try {
            return java.net.URI.create(url).getPort();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
