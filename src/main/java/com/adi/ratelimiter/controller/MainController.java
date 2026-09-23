package com.adi.ratelimiter.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {

    @GetMapping("/home")
    public String greet() {
        return "Hello Bud";
    }

    @GetMapping("/server")
    public String getServer() {
        return System.getenv("SERVER_ID");
    }
}
