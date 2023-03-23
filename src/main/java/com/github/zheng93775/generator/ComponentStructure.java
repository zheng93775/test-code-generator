package com.github.zheng93775.generator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

class ComponentStructure extends ClassStructure {

    public ComponentStructure(Class clazz) {
        this.clazz = clazz;
        this.methods = parseMethods();
        this.processOverloadMethods();
    }

    public List<MethodStructure> parseMethods() {
        Method[] methods = this.clazz.getDeclaredMethods();
        Map<String, MethodStructure> signatureMethodMap = new HashMap<>();
        for (Method method : methods) {
            if (Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())
                    && !Tool.hasGenericParameter(method)
                    && !Tool.hasObjectParameter(method)
                    && !method.getName().equals("notify")) {
                String signature = method.getName() + "(" + Arrays.stream(method.getParameterTypes()).map(t -> t.getTypeName()).collect(Collectors.joining(", ")) + ")";
                if (signatureMethodMap.containsKey(signature)) {
                    Method existsMethod = signatureMethodMap.get(signature).getMethod();
                    // 签名相同，但是返回值类型不同的方法，需要去重，取类型更具体的那个
                    if (existsMethod.getReturnType().isAssignableFrom(method.getReturnType())) {
                        signatureMethodMap.put(signature, new MethodStructure(method));
                    }
                } else {
                    signatureMethodMap.put(signature, new MethodStructure(method));
                }
            }
        }
        return new LinkedList<>(signatureMethodMap.values());
    }

    private void processOverloadMethods() {
        Set<String> overloadMethodNameSet = this.buildOverloadMethodNameSet();
        for (MethodStructure methodStructure : methods) {
            if (overloadMethodNameSet.contains(methodStructure.getName())) {
                int signatureHashCode = Arrays.stream(methodStructure.getMethod().getParameterTypes()).map(c -> c.getTypeName()).collect(Collectors.joining(",")).hashCode();
                methodStructure.setName(methodStructure.getName() + "_" + Math.abs(signatureHashCode));
            }
        }
    }
}
