package com.feign.dubbo.example.api.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient("provider")
public interface DemoHelloService {

    @GetMapping("/hello")
    String sayHello(@RequestParam("msg") String msg);

}
