# Multiple-RPC

在Spring cloud alibaba的基础上，以Nacos为注册中心，提供了两种服务调用方式，默认方式为 Feign，导入Dubbo包之后会使用Dubbo调用

## Required

- JDK1.8
- maven 3.6+

## 项目依赖

- Spring boot 2.1.8.RELEASE
- Spring cloud alibaba 2.1.0.RELEASE
- Spring cloud alibaba nacos starter 0.9.0.RELEASE
- Spring cloud starter openfeign 2.1.1.RELEASE

## Gettging Start

* 克隆本项目

```shell script
git clone https://github.com/gglaochen/invokeReplace.git
```

* 将本项目install到本地仓库

进入项目invokeReplace路径下执行

```shell script
mvn install -DskipTests
```

* 在项目pom文件中导入

```xml
<dependency>
    <groupId>com.feign.dubbo.example</groupId>
    <artifactId>starter</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

* 替换Dubbo调用方式

pom中导入

```xml
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo-spring-boot-starter</artifactId>
</dependency>
<dependency>
    <groupId>org.apache.dubbo</groupId>
    <artifactId>dubbo</artifactId>
</dependency>
```

## sample

* [multiple-rpc-demo](multiple-rpc-demo)