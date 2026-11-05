package com.studentcareer.web.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StatusController {
    private final JdbcTemplate jdbc;

    public StatusController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/status")
    public StatusResponse status() {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return new StatusResponse("UP", "POSTGRESQL", "EXPLICIT_IMPORTS");
    }

    public record StatusResponse(String status, String storage, String dataMode) { }
}
