package com.minet.sacco.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DatabaseInfoController {

    @Value("${spring.datasource.url}")
    private String databaseUrl;

    @Value("${spring.datasource.username}")
    private String databaseUsername;

    @GetMapping("/database-info")
    public Map<String, String> getDatabaseInfo() {
        Map<String, String> info = new HashMap<>();
        info.put("databaseUrl", databaseUrl);
        info.put("databaseUsername", databaseUsername);
        info.put("message", "This endpoint shows which database the backend is connected to");
        return info;
    }
}
