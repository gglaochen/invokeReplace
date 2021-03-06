package com.feign.dubbo.example.core;

import feign.Feign;
import org.apache.dubbo.config.AbstractConfig;
import org.apache.dubbo.config.spring.beans.factory.annotation.DubboBuilder;
import org.apache.dubbo.config.spring.beans.factory.annotation.FeignClient2DubboPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Collections;
import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.*;

@Configuration
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true, havingValue = "true")
@ConditionalOnClass(AbstractConfig.class)
public class ImportDubboAutoConfiguration {

    @Bean
    public FeignClient2DubboPostProcessor feignClient2DubboPostProcessor(Environment env) {
        Set<String> basePackages = env.getProperty(DUBBO_SCAN_PREFIX + BASE_PACKAGES_PROPERTY_NAME, Set.class, Collections.emptySet());
        return new FeignClient2DubboPostProcessor(basePackages);
    }

    @Bean
    public Feign.Builder feignDubboBuilder() {
        return new DubboBuilder();
    }

}
