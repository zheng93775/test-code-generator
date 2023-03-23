package com.github.zheng93775.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.FileInputStream;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

class ControllerTestCodeWriter extends TestCodeWriter {
    private ControllerStructure controllerStructure;

    public ControllerTestCodeWriter(ProjectStructure projectStructure, ControllerStructure controllerStructure) {
        super(projectStructure, controllerStructure);
        this.controllerStructure = controllerStructure;
    }

    private void addBaseImport() {
        this.addImport(Test.class.getName());
        this.addImport(RunWith.class.getName());
        this.addImport(Autowired.class.getName());
        this.addImport(AutoConfigureMockMvc.class.getName());
        this.addImport(SpringBootTest.class.getName());
        this.addImport(SpringRunner.class.getName());
        this.addImport(MockMvc.class.getName());
        this.addImport(ObjectMapper.class.getName());
        this.addImport(projectStructure.getMainClass().getName());
        this.addImport(MvcResult.class.getName());
        this.addImport(MockMvcRequestBuilders.class.getName());
        this.addImport(MockMvcResultMatchers.class.getName());
    }

    @Override
    public String generateCode() {
        this.addBaseImport();
        String methodCodes = this.controllerStructure.getMethods().stream()
                .map(methodStructure -> generateMethodCode(methodStructure))
                .collect(Collectors.joining("\n"));

        StringAppender sa = new StringAppender();
        sa.appendLine(0, "package " + this.controllerStructure.getClazz().getPackage().getName() + ";").appendLine();
        List<String> importList = new LinkedList<>(this.importSet);
        Collections.sort(importList);
        importList.forEach(i -> {
            sa.appendLine(0, "import " + i + ";");
        });
        sa.appendLine();
        sa.appendLine(0, "@SpringBootTest(classes = " + projectStructure.getMainClass().getSimpleName() + ".class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)");
        sa.appendLine(0, "@AutoConfigureMockMvc");
        sa.appendLine(0, "@RunWith(SpringRunner.class)");
        sa.appendLine(0, "public class " + this.controllerStructure.getClazz().getSimpleName() + "Test {").appendLine();
        sa.appendLine(4, "private ObjectMapper objectMapper = new ObjectMapper();").appendLine();
        sa.appendLine(4, "@Autowired");
        sa.appendLine(4, "private MockMvc mockMvc;").appendLine();
        sa.appendLine(0, methodCodes);
        sa.appendLine(0, "}");
        return sa.toString();
    }

    @Override
    protected String generateMethodCode(MethodStructure methodStructure) {
        this.addBaseImport();
        this.varNameSet.clear();
        StringAppender sa = new StringAppender();
        sa.appendLine(4, "@Test");
        sa.appendLine(4, "public void " + methodStructure.getName() + "() throws Exception {");
        if (methodStructure.getBodyType() != null) {
            this.generateVarCode(methodStructure.getBodyType(), sa, "req");
        }
        this.addImport(String.class.getName());
        sa.appendLine(8, "MvcResult result = mockMvc.perform(");
        sa.appendLine(16, "MockMvcRequestBuilders");
        String uri = methodStructure.getRequestPath();
        int partCount = uri.split("\\{").length;
        String pathVariables = "";
        for (int i = 1; i < partCount; i++) {
            pathVariables += ", \"" + ThreadLocalRandom.current().nextInt() + "\"";
        }
        String httpMethod = methodStructure.getMultipartFileName() != null ? "fileUpload" : methodStructure.getRequestMethod().name().toLowerCase();
        sa.appendLine(24, "." + httpMethod + "(\"" + uri + "\"" + pathVariables + ")");
        if (methodStructure.getMultipartFileName() != null) {
            this.addImport(MockMultipartFile.class.getTypeName());
            this.addImport(FileInputStream.class.getTypeName());
            sa.appendLine(24, ".file(new MockMultipartFile(\"" + methodStructure.getMultipartFileName()
                    + "\", new FileInputStream(\"/tmp/abc.txt\")))");
        }
        for (ParameterStructure header : methodStructure.getHeaders()) {
            sa.appendLine(24, ".header(\"" + header.getName() + "\", \"" + randomValue(header.getType(), false) + "\")");
        }
        for (ParameterStructure parameterStructure : methodStructure.getParams()) {
            String paramValues = "";
            if (parameterStructure.getType() instanceof ParameterizedType && Collection.class.isAssignableFrom((Class) ((ParameterizedType) parameterStructure.getType()).getRawType())) {
                int limit = ThreadLocalRandom.current().nextInt(2) + 2;
                Type paramGenericType = ((ParameterizedType) parameterStructure.getType()).getActualTypeArguments()[0];
                for (int i = 0; i < limit; i++) {
                    paramValues += ", \"" + randomValue(paramGenericType, false) + "\"";
                }
            } else {
                paramValues += ", \"" + randomValue(parameterStructure.getType(), false) + "\"";
            }
            sa.appendLine(24, ".param(\"" + parameterStructure.getName() + "\"" + paramValues + ")");
        }
        if (methodStructure.getBodyType() != null) {
            this.addImport(MediaType.class.getName());
            sa.appendLine(24, ".contentType(MediaType.APPLICATION_JSON)");
            sa.appendLine(24, ".content(objectMapper.writeValueAsString(req))");
        }
        sa.appendLine(16, ")");
        sa.appendLine(16, ".andExpect(MockMvcResultMatchers.status().isOk())");
        sa.appendLine(16, ".andReturn();");
        sa.appendLine(8, "System.out.println(\"responseBody:\");");
        if (methodStructure.getReturnType().getTypeName().startsWith("reactor.core.publisher.")) {
            sa.appendLine(8, "System.out.println(objectMapper.writeValueAsString(result.getAsyncResult()));");
        } else {
            sa.appendLine(8, "System.out.println(result.getResponse().getContentAsString());");
        }
        sa.appendLine(4, "}");
        return sa.toString();
    }
}
