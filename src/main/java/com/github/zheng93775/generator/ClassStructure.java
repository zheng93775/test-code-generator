package com.github.zheng93775.generator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

class ClassStructure {
    protected Class clazz;
    protected List<MethodStructure> methods;

    public Class getClazz() {
        return clazz;
    }

    public void setClazz(Class clazz) {
        this.clazz = clazz;
    }

    public List<MethodStructure> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodStructure> methods) {
        this.methods = methods;
    }

    protected Set<String> buildOverloadMethodNameSet() {
        Set<String> methodNameSet = new HashSet<>();
        Set<String> overloadMethodNameSet = new HashSet<>();
        for (MethodStructure methodStructure : methods) {
            if (methodNameSet.contains(methodStructure.getMethod().getName())) {
                overloadMethodNameSet.add(methodStructure.getMethod().getName());
            } else {
                methodNameSet.add(methodStructure.getMethod().getName());
            }
        }
        return overloadMethodNameSet;
    }
}
