package com.github.zheng93775.generator;

public class TestCodeGenerator {

    private ProjectStructure projectStructure;

    public void generate() {
        generate(null, null, null);
    }

    public void generate(Class mainClass, String controllerPackage, Integer createLimit) {
        this.projectStructure = ProjectScanner.scan(mainClass, controllerPackage);
        int createSize = 0;
        for (Class controllerClass : this.projectStructure.getControllerClasses()) {
            if (createLimit != null && createSize >= createLimit) {
                break;
            }
            createSize += generateController(controllerClass);
        }
        for (Class componentClass : this.projectStructure.getComponentClasses()) {
            if (createLimit != null && createSize >= createLimit) {
                break;
            }
            createSize += generateComponent(componentClass);
        }
    }

    public int generateController(Class controllerClass) {
        if (this.projectStructure == null) {
            this.projectStructure = ProjectScanner.scan(null, null);
        }
        ControllerStructure controllerStructure = new ControllerStructure(controllerClass);
        Class testControllerClass = this.findTestClass(controllerClass);
        TestCodeWriter testCodeWriter = new ControllerTestCodeWriter(projectStructure, controllerStructure);
        if (testControllerClass == null) {
            testCodeWriter.create();
            return 1;
        } else {
            testCodeWriter.append(testControllerClass);
            return 0;
        }
    }

    public int generateComponent(Class componentClass) {
        if (this.projectStructure == null) {
            this.projectStructure = ProjectScanner.scan(null, null);
        }
        ComponentStructure componentStructure = new ComponentStructure(componentClass);
        Class testClass = this.findTestClass(componentClass);
        TestCodeWriter testCodeWriter = new ComponentTestCodeWriter(projectStructure, componentStructure);
        if (testClass == null) {
            testCodeWriter.create();
            return 1;
        } else {
            testCodeWriter.append(testClass);
            return 0;
        }
    }

    private Class findTestClass(Class clazz) {
        try {
            return Class.forName(clazz.getName() + "Test");
        } catch (Exception e) {
            return null;
        }
    }

}
