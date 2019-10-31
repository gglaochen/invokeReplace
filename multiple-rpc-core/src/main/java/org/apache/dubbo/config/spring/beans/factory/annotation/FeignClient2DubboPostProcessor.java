package org.apache.dubbo.config.spring.beans.factory.annotation;

import com.alibaba.dubbo.config.annotation.Service;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.common.utils.CollectionUtils;
import org.apache.dubbo.config.spring.ServiceBean;
import org.apache.dubbo.config.spring.context.annotation.DubboClassPathBeanDefinitionScanner;
import org.apache.dubbo.config.spring.util.ObjectUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.config.*;
import org.springframework.beans.factory.support.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.apache.dubbo.spring.boot.util.DubboUtils.BASE_PACKAGES_PROPERTY_NAME;
import static org.apache.dubbo.spring.boot.util.DubboUtils.DUBBO_SCAN_PREFIX;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR;
import static org.springframework.core.annotation.AnnotationUtils.findAnnotation;
import static org.springframework.util.ClassUtils.resolveClassName;

@Data
@Slf4j
@ConditionalOnClass(ConfigurationPropertySources.class)
@ConditionalOnProperty(prefix = DUBBO_SCAN_PREFIX, name = BASE_PACKAGES_PROPERTY_NAME)
public class FeignClient2DubboPostProcessor implements BeanDefinitionRegistryPostProcessor, ResourceLoaderAware, EnvironmentAware, BeanClassLoaderAware {

    private Set<String> basePackages;

    private ResourceLoader resourceLoader;

    private Environment environment;

    private ClassLoader classLoader;

    private static final String SEPARATOR = ":";

    public FeignClient2DubboPostProcessor(Set<String> basePackages) {
        this.basePackages = basePackages;
    }

    /**
     * 在应用初始化之后，修改上下文中的内部bean定义。这一步骤处于所有的常规bean初始化之后，且尚未被实例之前，这一阶段允许添加一些bean定义
     *
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

    /**
     * 注册被{@link org.springframework.cloud.openfeign.FeignClient}标注的Bean
     *
     * @param fullPathPkgs 扫描包全路径
     * @param registry     上下文中使用的bean定义注册表
     */
    private void registerServiceBeans(Set<String> fullPathPkgs, BeanDefinitionRegistry registry) {

        /* 创建Dubbo扫描器 */
        DubboClassPathBeanDefinitionScanner scanner = new DubboClassPathBeanDefinitionScanner(registry, environment, resourceLoader);

        /* 创建beanNameGenerator */
        BeanNameGenerator beanNameGenerator = resloveBeanNameGenerator(registry);

        /* 配置scanner的基本信息 */
        scanner.setBeanNameGenerator(beanNameGenerator);
        scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class, true, true));

        for (String fullPathPkg : fullPathPkgs) {
            scanner.scan(fullPathPkg);

            /* 创建所有被FeignClient注解标注的Bean */
            Set<BeanDefinitionHolder> beanDefinitionHolders = createRpcBeanDefineHolder(scanner, fullPathPkg, registry, beanNameGenerator);

            beanDefinitionHolders.forEach(beanDefinitionHolder -> registerServiceBean(beanDefinitionHolder, registry, scanner));
        }

        return;

    }

    private void registerServiceBean(BeanDefinitionHolder beanDefinitionHolder, BeanDefinitionRegistry registry, DubboClassPathBeanDefinitionScanner scanner) {
        Class<?> beanClass = resolveClass(beanDefinitionHolder);

        Service service = findAnnotation(beanClass, Service.class);
        Class<?> interfaceClass = resolveServiceInterfaceClass(beanClass, service);

        String annotatedServiceBeanName = beanDefinitionHolder.getBeanName();

        AbstractBeanDefinition beanDefinition = buildServiceBeanDefine(service, interfaceClass, annotatedServiceBeanName);

        String beanName = generatorBeanName(service, interfaceClass, annotatedServiceBeanName);

        if (scanner.checkCandidate(beanName, beanDefinition)) {
            registry.registerBeanDefinition(beanName, beanDefinition);
        }
    }

    private String generatorBeanName(Service service, Class<?> interfaceClass, String annotatedServiceBeanName) {
        StringBuilder beanNameBuilder = new StringBuilder(ServiceBean.class.getSimpleName());
        beanNameBuilder.append(SEPARATOR).append(annotatedServiceBeanName);

        beanNameBuilder.append(SEPARATOR).append(interfaceClass.getName());

        String version = service.version();

        if (StringUtils.hasText(version)) {
            beanNameBuilder.append(SEPARATOR).append(version);
        }

        String group = service.group();

        if (StringUtils.hasText(group)) {
            beanNameBuilder.append(SEPARATOR).append(group);
        }

        return beanNameBuilder.toString();
    }

    private AbstractBeanDefinition buildServiceBeanDefine(Service service, Class<?> interfaceClass, String annotatedServiceBeanName) {
        BeanDefinitionBuilder builder = rootBeanDefinition(ServiceBean.class);

        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();

        MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();

        String[] ignoreAttributeNames = ObjectUtils.of("provider", "monitor", "application", "module", "registry", "protocol", "interface");

        propertyValues.addPropertyValues(new AnnotationPropertyValuesAdapter(service, environment, ignoreAttributeNames));

        builder.addPropertyReference("ref", environment.resolvePlaceholders(annotatedServiceBeanName));

        builder.addPropertyValue("interface", interfaceClass.getName());

        if (null != service) {
            if (StringUtils.hasText(service.provider())) {
                builder.addPropertyValue("provider", service.provider());
            }

            if (StringUtils.hasText(service.monitor())) {
                builder.addPropertyValue("monitor", service.monitor());
            }

            if (StringUtils.hasText(service.application())) {
                builder.addPropertyValue("application", service.application());
            }

            if (StringUtils.hasText(service.module())) {
                builder.addPropertyValue("module", service.module());
            }

            String[] registry = service.registry();
            List<RuntimeBeanReference> registerRuntimeBeanReferences = toRuntimeBeanReference(registry);

            if (CollectionUtils.isNotEmpty(registerRuntimeBeanReferences)) {
                builder.addPropertyValue("registries", registerRuntimeBeanReferences);
            }

            String[] protocol = service.protocol();
            List<RuntimeBeanReference> protocolRuntimeBeanReference = toRuntimeBeanReference(protocol);

            if (CollectionUtils.isNotEmpty(protocolRuntimeBeanReference)) {
                builder.addPropertyValue("protocols", protocolRuntimeBeanReference);
            }

        }

        return builder.getBeanDefinition();
    }

    private List<RuntimeBeanReference> toRuntimeBeanReference(String[] registry) {
        List<RuntimeBeanReference> runtimeBeanReferences = new ManagedList<>();

        if (!org.springframework.util.ObjectUtils.isEmpty(registry)) {
            for (String re : registry) {
                runtimeBeanReferences.add(new RuntimeBeanReference(environment.resolvePlaceholders(re)));
            }
        }

        return runtimeBeanReferences;
    }

    private Class<?> resolveServiceInterfaceClass(Class<?> beanClass, Service service) {
        Class<?> interfaceClass = null;

        if (null != service) {
            interfaceClass = service.interfaceClass();
            if (void.class.equals(interfaceClass)) {
                String interfaceName = service.interfaceName();
                if (StringUtils.hasText(interfaceName)) {
                    interfaceClass = resolveClassName(interfaceName, classLoader);
                }
            }
        }

        if (null == interfaceClass) {
            Class<?>[] interfaces = beanClass.getInterfaces();
            if (interfaces.length > 0) {
                interfaceClass = interfaces[0];
            }
        }
        return interfaceClass;
    }

    private Class<?> resolveClass(BeanDefinitionHolder beanDefinitionHolder) {
        BeanDefinition beanDefinition = beanDefinitionHolder.getBeanDefinition();
        String beanClassName = beanDefinition.getBeanClassName();
        return resolveClassName(beanClassName, classLoader);
    }

    private Set<BeanDefinitionHolder> createRpcBeanDefineHolder(ClassPathBeanDefinitionScanner scanner, String fullPathPkg, BeanDefinitionRegistry registry, BeanNameGenerator beanNameGenerator) {
        Set<BeanDefinition> components = scanner.findCandidateComponents(fullPathPkg);
        LinkedHashSet<BeanDefinitionHolder> beanDefinitionHolders = new LinkedHashSet<>(components.size());

        components.forEach(component -> {
            String beanName = beanNameGenerator.generateBeanName(component, registry);
            BeanDefinitionHolder beanDefinitionHolder = new BeanDefinitionHolder(component, beanName);
            beanDefinitionHolders.add(beanDefinitionHolder);
        });

        return beanDefinitionHolders;
    }

    private BeanNameGenerator resloveBeanNameGenerator(BeanDefinitionRegistry registry) {
        BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();
        BeanNameGenerator beanNameGeneratorTmp = null;

        if (registry instanceof SingletonBeanRegistry) {
            SingletonBeanRegistry beanRegistry = SingletonBeanRegistry.class.cast(registry);
            beanNameGeneratorTmp = (BeanNameGenerator) beanRegistry.getSingleton(CONFIGURATION_BEAN_NAME_GENERATOR);
        }

        return Optional.ofNullable(beanNameGeneratorTmp).orElse(beanNameGenerator);
    }

    private Set<String> reslovePlaceholders(Set<String> basePackages) {
        final Set<String> fullPathPkgs = new LinkedHashSet<String>(basePackages.size());
        basePackages.stream().filter(StringUtils::hasText).forEach(pkg -> fullPathPkgs.add(environment.getProperty(pkg, String.class)));
        return fullPathPkgs;
    }

    /**
     * 在应用初始化之后，修改上下文中的内部bean定义。允许修改或添加尚未初始化bean的属性
     *
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

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }
}
