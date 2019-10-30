package com.feign.dubbo.example.core.context;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SCAN_PREFIX;

@Data
@Slf4j
@AllArgsConstructor
@ConditionalOnClass(ConfigurationPropertySources.class)
@ConditionalOnProperty(prefix = DUBBO_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
public class FeignClient2DubboPostProcessor implements BeanDefinitionRegistryPostProcessor, ResourceLoaderAware, EnvironmentAware {

    private Set<String> basePackages;

    private ResourceLoader resourceLoader;

    private Environment environment;

    /**
     * 在应用初始化之后，修改上下文中的内部bean定义。这一步骤处于所有的常规bean初始化之后，且尚未被实例之前，这一阶段允许添加一些bean定义
     * @param registry 上下文中使用的bean定义的注册表
     * @throws BeansException 错误情况下会抛出异常
     */
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        Set<String> fullPathPkgs = reslovePlaceholders(basePackages);
        log.debug("FeignClient2DubboPostProcessor >>> fullPathPkgs : {}", fullPathPkgs);
        if (CollectionUtils.isNotEmpty(fullPathPkgs)) {
            registerServiceBeans(fullPathPkgs, registry);
        } else {
            log.warn("FeignClient2DubboPostProcessor >>> packagesToScan is empty , ServiceBean registry will be ignored!");
        }
    }

    private void registerServiceBeans(Set<String> fullPathPkgs, BeanDefinitionRegistry registry) {

    }

    private Set<String> reslovePlaceholders(Set<String> basePackages) {
        final Set<String> fullPathPkgs = new LinkedHashSet<String>(basePackages.size());
        basePackages.stream().filter(StringUtils::hasText).forEach(pkg -> fullPathPkgs.add(environment.getProperty(pkg, String.class)));
        return fullPathPkgs;
    }

    /**
     * 在应用初始化之后，修改上下文中的内部bean定义。允许修改或添加尚未初始化bean的属性
     * @param beanFactory application context中使用的bean factory
     * @throws BeansException 错误情况下会抛出异常
     */
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }
}
