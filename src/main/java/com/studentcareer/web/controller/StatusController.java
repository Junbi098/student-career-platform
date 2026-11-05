package com.studentcareer.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    @GetMapping("/api/status")
    public StatusResponse status() {
        return new StatusResponse("UP", "IN_MEMORY", "EXPLICIT_IMPORTS");
    }

    public record StatusResponse(String status, String storage, String dataMode) { }
}
