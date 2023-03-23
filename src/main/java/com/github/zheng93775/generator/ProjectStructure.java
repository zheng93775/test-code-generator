package com.github.zheng93775.generator;

import java.util.LinkedList;
import java.util.List;

class ProjectStructure {
    private String javaDir;
    private Class mainClass;
    private List<Class> controllerClasses = new LinkedList<>();
    private List<Class> componentClasses = new LinkedList<>();

    public String getJavaDir() {
        return javaDir;
    }

    public void setJavaDir(String javaDir) {
        this.javaDir = javaDir;
    }

    public Class getMainClass() {
        return mainClass;
    }

    public void setMainClass(Class mainClass) {
        this.mainClass = mainClass;
    }

    public List<Class> getControllerClasses() {
        return controllerClasses;
    }

    public void setControllerClasses(List<Class> controllerClasses) {
        this.controllerClasses = controllerClasses;
    }

    public List<Class> getComponentClasses() {
        return componentClasses;
    }

    public void setComponentClasses(List<Class> componentClasses) {
        this.componentClasses = componentClasses;
    }
}
