package com.neighborlink.auth_service.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/user/test")
    public String userTest() {
        return "USER endpoint accessed successfully";
    }

    @GetMapping("/admin/test")
    public String adminTest() {
        return "ADMIN endpoint accessed successfully";
    }
}