package com.feign.dubbo.example.provider.service.impl;

import com.feign.dubbo.example.api.service.DemoHelloService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoHelloServiceImpl implements DemoHelloService {

    @Override
    @GetMapping("/hello")
    public String sayHello(String msg) {
        return "hello" + msg;
    }
}
