package org.apache.dubbo.config.spring.beans.factory.annotation;

import feign.Feign;
import feign.Target;
import org.apache.dubbo.config.annotation.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import static org.apache.dubbo.config.spring.util.AnnotationUtils.getAttributes;
import static org.springframework.core.annotation.AnnotationAttributes.fromMap;

public class DubboBuilder extends Feign.Builder {

    @Autowired
    private ApplicationContext applicationContext;

    private Reference defaultReference;

    static final class DefaultReferenceClass {
        @Reference(check = false)
        String field;
    }

    public DubboBuilder() {
        // 产生@Reference 默认配置实例
        this.defaultReference = ReflectionUtils.findField(DubboBuilder.DefaultReferenceClass.class, "field").getAnnotation(Reference.class);
    }


    @Override
    public <T> T target(Target<T> target) {
        // 产生服务提供方接口远程代理 bean时
        // 使用dubbo产生的bean替换默认的feign产生的restful调用的bean
        ReferenceBeanBuilder beanBuilder = ReferenceBeanBuilder
                .create(fromMap(getAttributes(defaultReference, applicationContext.getEnvironment(), true)), applicationContext)
                .interfaceClass(target.type());

        try {
            T object = (T) beanBuilder.build().getObject();
            return object;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
