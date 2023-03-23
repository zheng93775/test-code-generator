package com.github.zheng93775.generator;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

class ControllerStructure extends ClassStructure {
    private String requestPath;

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public ControllerStructure(Class controllerClass) {
        this.clazz = controllerClass;
        this.requestPath = this.parseRequestPath();
        this.methods = parseMethods();
        this.processOverloadMethods();
    }

    private String parseRequestPath() {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(clazz, RequestMapping.class);
        return requestMapping != null && requestMapping.path().length > 0 ? requestMapping.path()[0] : "";
    }

    public List<MethodStructure> parseMethods() {
        List<MethodStructure> methodList = new LinkedList<>();
        Method[] methods = this.clazz.getDeclaredMethods();
        Class[] interfaces = this.clazz.getInterfaces();
        for (Method method : methods) {
            RequestMapping requestMapping = AnnotatedElementUtils.getMergedAnnotation(method, RequestMapping.class);
            if (requestMapping == null) {
                for (Class iface : interfaces) {
                    try {
                        Method interfaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
                        requestMapping = AnnotatedElementUtils.getMergedAnnotation(interfaceMethod, RequestMapping.class);
                        if (requestMapping != null) {
                            methodList.add(new MethodStructure(interfaceMethod, requestMapping, this.requestPath));
                            break;
                        }
                    } catch (NoSuchMethodException e) {
                        // do nothing
                    }
                }
            } else {
                methodList.add(new MethodStructure(method, requestMapping, this.requestPath));
            }
        }
        return methodList;
    }

    private void processOverloadMethods() {
        Set<String> overloadMethodNameSet = this.buildOverloadMethodNameSet();
        for (MethodStructure methodStructure : methods) {
            if (overloadMethodNameSet.contains(methodStructure.getName())) {
                String[] paths = methodStructure.getRequestMapping().value();
                if (paths.length == 0) {
                    continue;
                }
                String path = paths.length == 0 ? "" : paths[0];
                path = path.replaceAll("[\\{\\}]", "").replaceAll("[\\/-]", "_");
                if (!path.startsWith("_")) path = "_" + path;
                methodStructure.setName(methodStructure.getName() + path);
            }
        }
    }
}
