package com.github.zheng93775.generator;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

class ProjectScanner {

    public static ProjectStructure scan(Class mainClass, String controllerPackage) {
        Class baseMapperClass = takeClass("tk.mybatis.mapper.common.BaseMapper");
        Class aspectClass = takeClass("org.aspectj.lang.annotation.Aspect");

        List<Class> controllerClasses = new LinkedList<>();
        List<Class> componentClasses = new LinkedList<>();
        String javaDir = null;
        try {
            PathMatchingResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resourcePatternResolver.getResources("classpath*:**/*.class");
            for (Resource resource : resources) {
                if (resource.getURL().toString().startsWith("jar:")) {
                    continue;
                }
                String canonicalPath = resource.getFile().getCanonicalPath().replaceAll("\\\\", "/");
                if (javaDir == null) {
                    int index = canonicalPath.indexOf("target");
                    javaDir = canonicalPath.substring(0, index) + "src/test/java/";
                    if (!Files.exists(Paths.get(javaDir))) {
                        javaDir = null;
                    }
                }
                int index = canonicalPath.indexOf("classes");
                String fullClassName = canonicalPath.substring(index + "classes".length() + 1, canonicalPath.length() - ".class".length()).replaceAll("[\\\\\\/]", ".");
                Class clazz = Class.forName(fullClassName);
                if (clazz.isMemberClass()) {
                    continue;
                }
                if (AnnotationUtils.findAnnotation(clazz, Controller.class) != null) {
                    if (controllerPackage == null || "".equals(controllerPackage) || fullClassName.startsWith(controllerPackage)) {
                        controllerClasses.add(clazz);
                    }
                    continue;
                }
                if (mainClass == null && AnnotationUtils.findAnnotation(clazz, SpringBootApplication.class) != null) {
                    mainClass = clazz;
                }
                if (AnnotationUtils.findAnnotation(clazz, Configuration.class) != null
                        || (aspectClass != null && AnnotationUtils.findAnnotation(clazz, aspectClass) != null)
                        || AnnotationUtils.findAnnotation(clazz, ControllerAdvice.class) != null) {
                    continue;
                }
                if (AnnotationUtils.findAnnotation(clazz, Component.class) != null) {
                    componentClasses.add(clazz);
                } else if (baseMapperClass != null && baseMapperClass.isAssignableFrom(clazz)) {
                    componentClasses.add(clazz);
                } else if (clazz.isInterface() && (clazz.getSimpleName().endsWith("Dao") || clazz.getSimpleName().endsWith("Mapper"))) {
                    componentClasses.add(clazz);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("扫描项目结构出错", e);
        }
        if (mainClass == null) {
            throw new RuntimeException("找不到 Main Class");
        }
        if (javaDir == null) {
            throw new RuntimeException("找不到 src/main/java");
        }
        ProjectStructure projectStructure = new ProjectStructure();
        projectStructure.setJavaDir(javaDir);
        projectStructure.setMainClass(mainClass);
        projectStructure.setControllerClasses(controllerClasses);
        projectStructure.setComponentClasses(componentClasses);
        return projectStructure;
    }

    private static Class takeClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }
}
