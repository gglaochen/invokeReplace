package com.feign.dubbo.example.consumer.controller;

import com.feign.dubbo.example.api.service.DemoHelloService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConsumerDemoController {

    @Autowired
    private DemoHelloService demoHelloService;

    @GetMapping("/hello")
    public String sayHello() {
        return demoHelloService.sayHello("Hello World");
    }

}
