package com.example.elasticsearchinteractiveservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class Controller {

    @GetMapping("/data")
    public String getData() {
        log.info("/data endpoint called");
        return "aaa";
    }
}
