# test-code-generator

用于SpringBoot Web项目自动生成单元测试代码

## 功能清单

* 自动生成Controller对应的单元测试类代码
* 自动生成Spring中定义的Component对应的单元测试类代码
* 自动生成Mybatis的Mapper/Dao接口对应的单元测试代码
* 判断对应的单元测试类是否存在，不存在则创建，如果已存在，就判断每个方法是否都有对应的单元测试方法，没有的就添加对应的测试方法

## 快速开始

### 引入Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>com.github.zheng93775</groupId>
        <artifactId>test-code-generator</artifactId>
        <version>1.0.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>junit</groupId>
        <artifactId>junit</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

版本号根据自己情况添加即可

### 在 src/test/java 下新建一个启动类

```java
import com.github.zheng93775.generator.TestCodeGenerator;

public class TestCodeGenerateRunner {
    public static void main(String[] args) {
        TestCodeGenerator testCodeGenerator = new TestCodeGenerator();
        testCodeGenerator.generate();
    }
}
```

运行启动类即可
