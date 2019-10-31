package org.apache.dubbo.config.spring.beans.factory.annotation;

import feign.Feign;
import org.springframework.stereotype.Component;

@Component
public class DubboBuilder extends Feign.Builder {
}
