package com.github.zheng93775.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

class ComponentTestCodeWriter extends TestCodeWriter {

    public ComponentTestCodeWriter(ProjectStructure projectStructure, ComponentStructure componentStructure) {
        super(projectStructure, componentStructure);
    }

    private void addBaseImport() {
        this.addImport(Test.class.getName());
        this.addImport(RunWith.class.getName());
        this.addImport(SpringRunner.class.getName());
        this.addImport(Autowired.class.getName());
        this.addImport(SpringBootTest.class.getName());
        this.addImport(Transactional.class.getName());
        this.addImport(Exception.class.getName());
        this.addImport(ObjectMapper.class.getName());
        this.addImport(this.classStructure.getClazz().getName().replaceAll("\\$", "."));
    }

    @Override
    protected String generateCode() {
        this.addBaseImport();
        String methodCodes = this.classStructure.getMethods().stream()
                .map(methodStructure -> generateMethodCode(methodStructure))
                .collect(Collectors.joining("\n"));
        String simpleClassName = this.classStructure.getClazz().getSimpleName();

        StringAppender sa = new StringAppender();
        sa.appendLine(0, "package " + this.classStructure.getClazz().getPackage().getName() + ";").appendLine();
        List<String> importList = new LinkedList<>(this.importSet);
        Collections.sort(importList);
        importList.forEach(i -> {
            sa.appendLine(0, "import " + i + ";");
        });
        sa.appendLine();
        sa.appendLine(0, "@SpringBootTest");
        sa.appendLine(0, "@RunWith(SpringRunner.class)");
        sa.appendLine(0, "public class " + simpleClassName + "Test {").appendLine();
        sa.appendLine(4, "private ObjectMapper objectMapper = new ObjectMapper();").appendLine();
        sa.appendLine(4, "@Autowired");
        sa.appendLine(4, "private " + simpleClassName + " " + prettyVarName(simpleClassName) + ";").appendLine();
        sa.appendLine(0, methodCodes);
        sa.appendLine(0, "}");
        return sa.toString();
    }

    @Override
    protected String generateMethodCode(MethodStructure methodStructure) {
        this.addBaseImport();
        this.varNameSet.clear();
        StringAppender sa = new StringAppender();
        this.addImport(Exception.class.getName());
        sa.appendLine(4, "@Test");
        sa.appendLine(4, "@Transactional");
        sa.appendLine(4, "public void " + methodStructure.getName() + "() throws Exception {");
        String paramStr = methodStructure.getParams().stream().map(parameterStructure -> {
            return this.generateVarCode(parameterStructure.getType(), sa, parameterStructure.getName());
        }).collect(Collectors.joining(", "));

        String methodName = methodStructure.getMethod().getName();
        String componentVarName = prettyVarName(this.classStructure.getClazz().getSimpleName());
        if (methodStructure.getReturnType().equals(void.class)) {
            sa.appendLine(8, componentVarName + "." + methodName + "(" + paramStr + ");");
            sa.appendLine(8, "System.out.println(\"end void\");");
        } else {
            this.addImport(methodStructure.getReturnType().getTypeName());
            String returnTypeName = takeDefName(methodStructure.getReturnType());
            String returnVarName = nextVarName(methodStructure.getReturnType());
            sa.appendLine(8, returnTypeName + " " + returnVarName + " = " + componentVarName + "." + methodName + "(" + paramStr + ");");
            sa.appendLine(8, "System.out.println(\"responseBody:\");");
            sa.appendLine(8, "System.out.println(objectMapper.writeValueAsString(" + returnVarName + "));");
        }
        sa.appendLine(4, "}");
        return sa.toString();
    }
}
