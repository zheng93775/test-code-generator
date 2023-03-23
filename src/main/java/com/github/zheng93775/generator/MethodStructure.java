package com.github.zheng93775.generator;

import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.List;

import static com.github.zheng93775.generator.Tool.or;

class MethodStructure {
    private Method method;
    private String name;
    private RequestMethod requestMethod;
    private RequestMapping requestMapping;
    private String multipartFileName;
    private String requestPath;
    private List<ParameterStructure> params = new LinkedList<>();
    private List<ParameterStructure> headers = new LinkedList<>();
    private Type bodyType;
    private Type returnType;

    public MethodStructure(Method method) {
        this.method = method;
        this.name = method.getName();
        int paramCount = method.getParameterCount();
        for (int i = 0; i < paramCount; i++) {
            ParameterStructure parameterStructure = new ParameterStructure();
            parameterStructure.setName(method.getParameters()[i].getName());
            parameterStructure.setType(or(method.getGenericParameterTypes()[i], method.getParameterTypes()[i]));
            this.params.add(parameterStructure);
        }
        this.returnType = or(method.getGenericReturnType(), method.getReturnType());
    }

    public MethodStructure(Method method, RequestMapping requestMapping, String controllerRequestPath) {
        this.method = method;
        this.name = method.getName();
        this.requestMapping = requestMapping;
        this.returnType = or(method.getGenericReturnType(), method.getReturnType());
        if (requestMapping.method().length == 0) {
            this.requestMethod = RequestMethod.POST;
        } else {
            this.requestMethod = requestMapping.method()[0];
        }
        if (requestMapping.value().length == 0) {
            this.requestPath = controllerRequestPath;
        } else {
            this.requestPath = requestMapping.value()[0];
            if (this.requestPath.startsWith("/")) {
                if (controllerRequestPath.endsWith("/")) {
                    this.requestPath = controllerRequestPath + this.requestPath.substring(1);
                } else {
                    this.requestPath = controllerRequestPath + this.requestPath;
                }
            } else {
                if (controllerRequestPath.endsWith("/")) {
                    this.requestPath = controllerRequestPath + this.requestPath;
                } else {
                    this.requestPath = controllerRequestPath + "/" + this.requestPath;
                }
            }
        }
        if (!this.requestPath.startsWith("/")) {
            this.requestPath = "/" + this.requestPath;
        }
        int paramCount = method.getParameterCount();
        Annotation[][] annotationSS = method.getParameterAnnotations();
        for (int i = 0; i < paramCount; i++) {
            for (Annotation paramAnnotation : annotationSS[i]) {
                ParameterStructure parameterStructure = new ParameterStructure();
                if (paramAnnotation.annotationType() == RequestParam.class) {
                    String name = ((RequestParam) paramAnnotation).name();
                    if (StringUtils.isEmpty(name)) {
                        name = ((RequestParam) paramAnnotation).value();
                    }
                    parameterStructure.setName(name);
                    parameterStructure.setType(or(method.getGenericParameterTypes()[i], method.getParameterTypes()[i]));
                    if (parameterStructure.getType().equals(MultipartFile.class)) {
                        this.multipartFileName = name;
                        continue;
                    }
                    this.params.add(parameterStructure);
                } else if (paramAnnotation.annotationType() == RequestBody.class) {
                    this.bodyType = or(method.getGenericParameterTypes()[i], method.getParameterTypes()[i]);
                } else if (paramAnnotation.annotationType() == RequestHeader.class) {
                    String name = ((RequestHeader) paramAnnotation).name();
                    if (StringUtils.isEmpty(name)) {
                        name = ((RequestHeader) paramAnnotation).value();
                    }
                    parameterStructure.setName(name);
                    parameterStructure.setType(or(method.getGenericParameterTypes()[i], method.getParameterTypes()[i]));
                    this.headers.add(parameterStructure);
                }
            }
        }
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public RequestMethod getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(RequestMethod requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public void setRequestPath(String requestPath) {
        this.requestPath = requestPath;
    }

    public List<ParameterStructure> getParams() {
        return params;
    }

    public void setParams(List<ParameterStructure> params) {
        this.params = params;
    }

    public List<ParameterStructure> getHeaders() {
        return headers;
    }

    public void setHeaders(List<ParameterStructure> headers) {
        this.headers = headers;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RequestMapping getRequestMapping() {
        return requestMapping;
    }

    public void setRequestMapping(RequestMapping requestMapping) {
        this.requestMapping = requestMapping;
    }

    public String getMultipartFileName() {
        return multipartFileName;
    }

    public void setMultipartFileName(String multipartFileName) {
        this.multipartFileName = multipartFileName;
    }

    public Type getBodyType() {
        return bodyType;
    }

    public void setBodyType(Type bodyType) {
        this.bodyType = bodyType;
    }

    public Type getReturnType() {
        return returnType;
    }

    public void setReturnType(Type returnType) {
        this.returnType = returnType;
    }
}
