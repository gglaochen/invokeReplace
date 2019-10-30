package com.feign.dubbo.example.core;

import com.feign.dubbo.example.core.context.DubboBuilder;
import com.feign.dubbo.example.core.context.FeignClient2DubboPostProcessor;
import feign.Feign;
import org.apache.dubbo.config.AbstractConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.apache.dubbo.spring.boot.util.DubboUtils.*;

@Configuration
@ConditionalOnProperty(prefix = DUBBO_PREFIX, name = "enabled", matchIfMissing = true, havingValue = "true")
@ConditionalOnClass(AbstractConfig.class)
public class ImportDubboAutoConfiguration {

    @ConditionalOnClass(ConfigurationPropertySources.class)
    @ConditionalOnProperty(prefix = DUBBO_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
    @Bean
    public FeignClient2DubboPostProcessor feignClient2DubboPostProcessor(Environment env) {
        String dubboScanPkg = env.getProperty(DUBBO_SCAN_PREFIX + BASE_PACKAGES_PROPERTY_NAME, String.class);
        return new FeignClient2DubboPostProcessor(dubboScanPkg);
    }

    @Bean
    public Feign.Builder dubboBuilder() {
        return new DubboBuilder();
    }

}
