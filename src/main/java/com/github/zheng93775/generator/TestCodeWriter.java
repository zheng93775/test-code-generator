package com.github.zheng93775.generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

abstract class TestCodeWriter {
    protected ProjectStructure projectStructure;
    protected Set<String> importSet;
    protected Set<String> importSimpleSet;
    protected ClassStructure classStructure;
    protected Set<String> varNameSet;

    public TestCodeWriter(ProjectStructure projectStructure, ClassStructure classStructure) {
        this.projectStructure = projectStructure;
        this.classStructure = classStructure;
        this.importSet = new HashSet<>();
        this.importSimpleSet = new HashSet<>();
        this.varNameSet = new HashSet<>();
    }

    public void create() {
        String codeString = this.generateCode();
        String dirFileName = this.projectStructure.getJavaDir() + this.classStructure.getClazz().getPackage().getName().replaceAll("\\.", "/");
        File dirFile = new File(dirFileName);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        String fileName = this.projectStructure.getJavaDir() + this.classStructure.getClazz().getName().replaceAll("\\.", "/") + "Test.java";
        try (FileWriter fw = new FileWriter(fileName)) {
            fw.write(codeString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String generateCode();

    public void append(Class testClass) {
        StringBuilder importCodes = new StringBuilder();
        StringBuilder defineCodes = new StringBuilder();
        StringBuilder contentCodes = new StringBuilder();
        Set<String> oldImportSet = new HashSet<>();
        List<String> oldMethodNames = Arrays.stream(testClass.getDeclaredMethods()).map(m -> m.getName()).collect(Collectors.toList());

        String fileName = this.projectStructure.getJavaDir() + testClass.getName().replaceAll("\\.", "/") + ".java";
        File targetFile = new File(fileName);
        boolean hasChange = false;
        boolean hasObjectMapper = false;
        try (
                FileInputStream fis = new FileInputStream(targetFile);
                InputStreamReader isr = new InputStreamReader(fis);
                BufferedReader br = new BufferedReader(isr)
        ) {
            importCodes.append(br.readLine() + "\n").append(br.readLine() + "\n");
            String line = null;
            int part = 0; // 0:import区域, 1:定义class区域，2:内容区域
            while ((line = br.readLine()) != null) {
                if (part == 0) {
                    if (line.startsWith("import ")) {
                        importCodes.append(line + "\n");
                        oldImportSet.add(line.trim().substring("import ".length(), line.length() - 1).trim());
                    } else if (line.trim().isEmpty()) {
                        importCodes.append(line + "\n");
                    } else {
                        part = 1;
                    }
                }
                if (part == 1) {
                    if (line.startsWith("class ") || line.contains(" class ")) {
                        part = 2;
                    }
                    defineCodes.append(line + "\n");
                    continue;
                }
                if (part == 2) {
                    if (line.trim().equals("private ObjectMapper objectMapper = new ObjectMapper();")) {
                        hasObjectMapper = true;
                    }
                    contentCodes.append(line + "\n");
                }
            }
            String methodCodes = this.classStructure.getMethods().stream()
                    .filter(methodStructure -> {
                        return !oldMethodNames.stream()
                                .filter(old -> old.equals(methodStructure.getName()))
                                .findFirst()
                                .isPresent();
                    })
                    .map(methodStructure -> generateMethodCode(methodStructure))
                    .collect(Collectors.joining("\n"));
            if (!this.importSet.isEmpty()) {
                this.importSet.stream().forEach(i -> {
                    if (!oldImportSet.contains(i)) {
                        importCodes.append("import " + i + ";\n");
                    }
                });
                importCodes.append("\n");
            }
            if (!methodCodes.trim().isEmpty()) {
                int index = contentCodes.lastIndexOf("}");
                contentCodes.delete(index, contentCodes.length());
                contentCodes.append("\n").append(methodCodes).append("\n}\n");
                hasChange = true;
                if (!hasObjectMapper) {
                    defineCodes.append("    private ObjectMapper objectMapper = new ObjectMapper();\n\n");
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (hasChange) {
            try (FileWriter fw = new FileWriter(targetFile)) {
                fw.write(importCodes.toString());
                fw.write(defineCodes.toString());
                fw.write(contentCodes.toString());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract String generateMethodCode(MethodStructure methodStructure);

    protected String toSimpleName(String name) {
        if (name.contains(".")) {
            String[] arr = name.split("\\.");
            return arr[arr.length - 1].replaceAll("\\$", ".");
        } else {
            return name;
        }
    }

    protected String generateVarCode(Type targetType, StringAppender sa) {
        return generateVarCode(targetType, sa, null);
    }

    protected String generateVarCode(Type targetType, StringAppender sa, String varName) {
        varName = nextVarName(targetType, varName);
        if (targetType instanceof Class) {
            Class mainClass = (Class) targetType;
            if (!mainClass.isPrimitive()) {
                this.addImport(targetType.getTypeName());
            }
            if (isSimpleType(targetType)) {
                sa.appendLine(8, takeDefName(mainClass) + " " + varName + " = " + randomValue(targetType) + ";");
            } else if (mainClass.isArray()) {
                varName = generateArrayCode(mainClass, sa, varName);
            } else {
                varName = generateObjectCode(targetType, sa, varName);
            }
        } else if (targetType instanceof ParameterizedType) {
            Class mainClass = (Class) ((ParameterizedType) targetType).getRawType();
            if (List.class.isAssignableFrom(mainClass) || Collection.class.equals(mainClass)) {
                varName = generateListCode((ParameterizedType) targetType, sa, varName);
            } else if (Set.class.isAssignableFrom(mainClass)) {
                varName = generateSetCode((ParameterizedType) targetType, sa, varName);
            } else if (Map.class.isAssignableFrom(mainClass)) {
                varName = generateMapCode((ParameterizedType) targetType, sa, varName);
            } else {
                varName = generateObjectCode(targetType, sa, varName);
            }
        } else {
            throw new RuntimeException("targetType invalid: " + targetType.getTypeName());
        }
        return varName;
    }

    protected Class<?>[] takeConstructorParams(Class clazz) {
        Constructor<?>[] constructors = clazz.getConstructors();
        Class<?>[] parameterTypes = null;
        for (Constructor<?> constructor : constructors) {
            if (parameterTypes == null) {
                parameterTypes = constructor.getParameterTypes();
            } else if (parameterTypes.length > constructor.getParameterCount()) {
                parameterTypes = constructor.getParameterTypes();
            }
        }
        if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
            // 内部类构造函数第一个参数固定是其归属类对象
            if (parameterTypes != null && parameterTypes.length > 0) {
                return Arrays.copyOfRange(parameterTypes, 1, parameterTypes.length);
            }
        }
        return parameterTypes;
    }

    protected boolean hasBuilderMethod(Class clazz) {
        try {
            Method builderMethod = clazz.getDeclaredMethod("builder");
            if (builderMethod != null && Modifier.isStatic(builderMethod.getModifiers())) {
                return true;
            }
        } catch (NoSuchMethodException e) {
        }
        return false;
    }

    protected String generateObjectCode(Type targetType, StringAppender sa, String varName) {
//        System.out.println("generateObjectCode: " + targetType.getTypeName());
        if (StringUtils.isEmpty(varName)) varName = nextVarName(targetType);
        this.addImport(targetType.getTypeName());
        String defName = takeDefName(targetType);
        Class implClass = takeImplClass(targetType);
        Class rawClass = takeRawClass(targetType);
        if (implClass == null) {
            sa.appendLine(8, defName + " " + varName + " = null;");
            return varName;
        }
        String implName = takeImplName(implClass.getTypeName());
        if (defName.endsWith(">")) {
            implName += "<>";
        }
        String newObjectStr = "";
        Class<?>[] constructorParamTypes = this.takeConstructorParams(implClass);
        if (constructorParamTypes == null) {
            if (hasBuilderMethod(implClass)) {
                newObjectStr = implName.substring(4) + ".builder().build();";
            } else {
                System.out.println("找不到构造函数或builder方法：" + implClass.getTypeName());
                newObjectStr = "null;";
            }
        } else if (targetType.equals(MultipartFile.class)) {
            newObjectStr = "new MockMultipartFile(\"file\", new FileInputStream(\"/tmp/abc.txt\"));";
        } else {
            String constructorParamStr = Arrays.stream(constructorParamTypes).map(t -> t.isPrimitive() ? randomValue(t) : "null").collect(Collectors.joining(", "));
            newObjectStr = implName + "(" + constructorParamStr + ");";
        }
        sa.appendLine(8, defName + " " + varName + " = " + newObjectStr);
        Method[] methods = rawClass.getDeclaredMethods();
        for (Method method : methods) {
//            System.out.println(mainClass.getName() + " " + method.getName());
            if (method.getName().startsWith("set") && method.getParameterCount() == 1) {
                Type paramType = method.getParameterTypes()[0];
                if (method.getGenericParameterTypes()[0] != null && method.getGenericParameterTypes()[0] instanceof ParameterizedType) {
                    paramType = DefParameterizedType.wrap(targetType, (ParameterizedType) method.getGenericParameterTypes()[0]);
                }
                String fieldName = lowerFirstChar(method.getName().substring(3));
                String code = isSimpleType(paramType) ? randomValue(paramType) : generateVarCode(paramType, sa, fieldName);
                sa.appendLine(8, varName + "." + method.getName() + "(" + code + ");");
            }
        }
        return varName;
    }

    protected String generateArrayCode(Class targetClass, StringAppender sa, String varName) {
        if (StringUtils.isEmpty(varName)) varName = nextVarName(targetClass);
        Class componentType = targetClass.getComponentType();
        String defName = takeDefName(componentType);
        Class implClass = takeImplClass(componentType);
        String implName = takeImplName(implClass.getTypeName());
        int size = new Random().nextInt(2) + 2;
        sa.appendLine(8, defName + "[] " + varName + " = " + implName + "[" + size + "];");
        for (int i = 0; i < size; i++) {
            if (isSimpleType(componentType)) {
                sa.appendLine(8, varName + "[" + i + "] = " + randomValue(componentType) + ";");
            } else {
                String childVarName = generateVarCode(componentType, sa);
                sa.appendLine(8, varName + "[" + i + "] = " + childVarName + ";");
            }
        }
        return varName;
    }

    protected String generateListCode(ParameterizedType genericType, StringAppender sa, String varName) {
        Type mainType = genericType.getRawType();
        this.addImport(mainType.getTypeName());
        if (StringUtils.isEmpty(varName)) varName = nextVarName(mainType);
        Class clazz = toClass(mainType);
        String defName = takeDefName(genericType);
        String implName = clazz.isInterface() ? takeDefName(LinkedList.class) : takeDefName(mainType);
        Type childType = genericType.getActualTypeArguments()[0];
        if (childType instanceof TypeVariable) {
            childType = String.class;
        }
        this.addImport(childType.getTypeName());
        sa.appendLine(8, defName + " " + varName + " = new " + implName + "<>();");
        int size = new Random().nextInt(2) + 2;
        for (int i = 0; i < size; i++) {
            if (isSimpleType(childType)) {
                sa.appendLine(8, varName + ".add(" + randomValue(childType) + ");");
            } else {
                String childVarName = generateVarCode(childType, sa);
                sa.appendLine(8, varName + ".add(" + childVarName + ");");
            }
        }
        return varName;
    }

    protected String generateSetCode(ParameterizedType genericType, StringAppender sa, String varName) {
        Type mainType = genericType.getRawType();
        this.addImport(mainType.getTypeName());
        if (StringUtils.isEmpty(varName)) varName = nextVarName(mainType);
        Class clazz = toClass(mainType);
        String defName = takeDefName(genericType);
        String implName = clazz.isInterface() ? takeDefName(HashSet.class) : takeDefName(mainType);
        Type childType = genericType.getActualTypeArguments()[0];
        if (childType instanceof TypeVariable) {
            childType = String.class;
        }
        this.addImport(childType.getTypeName());
        sa.appendLine(8, defName + " " + varName + " = new " + implName + "<>();");
        int size = new Random().nextInt(2) + 2;
        for (int i = 0; i < size; i++) {
            if (isSimpleType(childType)) {
                sa.appendLine(8, varName + ".add(" + randomValue(childType) + ");");
            } else {
                String childVarName = generateVarCode(childType, sa);
                sa.appendLine(8, varName + ".add(" + childVarName + ");");
            }
        }
        return varName;
    }

    protected String generateMapCode(ParameterizedType genericType, StringAppender sa, String varName) {
        Type mainType = genericType.getRawType();
        this.addImport(mainType.getTypeName());
        if (StringUtils.isEmpty(varName)) varName = nextVarName(mainType);
        Class clazz = toClass(mainType);
        String defName = takeDefName(genericType);
        String implName = clazz.isInterface() ? takeDefName(HashMap.class) : takeDefName(mainType);
        Type childType1 = genericType.getActualTypeArguments()[0];
        if (childType1 instanceof TypeVariable) {
            childType1 = String.class;
        }
        Type childType2 = genericType.getActualTypeArguments()[1];
        if (childType2 instanceof TypeVariable) {
            childType2 = String.class;
        }
        this.addImport(childType1.getTypeName());
        this.addImport(childType2.getTypeName());
        sa.appendLine(8, defName + " " + varName + " = new " + implName + "<>();");
        int size = new Random().nextInt(3) + 2;
        for (int i = 0; i < size; i++) {
            String key = isSimpleType(childType1) ? randomValue(childType1) : generateVarCode(childType1, sa);
            String value = isSimpleType(childType2) ? randomValue(childType2) : generateVarCode(childType2, sa);
            sa.appendLine(8, varName + ".put(" + key + ", " + value + ");");
        }
        return varName;
    }

    protected boolean isSimpleType(Type type) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            return clazz.isPrimitive() || clazz.equals(String.class) || clazz.equals(Boolean.class) || clazz.equals(Character.class) || Number.class.isAssignableFrom(clazz) || clazz.equals(LocalDateTime.class) || clazz.equals(LocalDate.class) || clazz.equals(LocalTime.class) || clazz.isEnum() || JsonNode.class.isAssignableFrom(clazz) || File.class.equals(clazz);
        }
        return false;
    }

    protected String nextVarName(Type type) {
        return nextVarName(type, null);
    }

    protected String nextVarName(Type type, String varName) {
        if (StringUtils.isEmpty(varName) || varName.matches("^arg\\d+$")) {
            String typeName = type.getTypeName();
            boolean isArray = false;
            if (typeName.endsWith("[]")) {
                isArray = true;
                typeName = typeName.substring(0, typeName.length() - 2);
            }
            if (typeName.contains("<")) {
                int index = typeName.indexOf("<");
                String mainPart = typeName.substring(0, index);
                String genericPart = typeName.substring(index + 1, typeName.length() - 1);
                if (genericPart.contains(",") || genericPart.contains("<")) {
                    varName = prettyVarName(toSimpleName(mainPart)).replace(".", "");
                } else {
                    varName = (prettyVarName(toSimpleName(genericPart)) + toSimpleName(mainPart)).replace(".", "");
                }
            } else {
                varName = prettyVarName(toSimpleName(type.getTypeName())).replace(".", "");
            }
            if (isArray) {
                varName += "Array";
            }
        }
        if (!varNameSet.contains(varName) && !isJavaKeyword(varName)) {
            varNameSet.add(varName);
            return varName;
        }
        for (int i = 1; i < 1000; i++) {
            if (!varNameSet.contains(varName + i)) {
                varNameSet.add(varName + i);
                return varName + i;
            } else {
                continue;
            }
        }
        return varName + "_" + Tool.randomAlphanumeric(6);
    }

    protected String prettyVarName(String varName) {
        int i = 0;
        for (; i < varName.length(); i++) {
            if (!Character.isUpperCase(varName.charAt(i))) {
                break;
            }
        }
        if (i > 1) i--;
        return varName.substring(0, i).toLowerCase() + varName.substring(i);
    }

    private boolean isJavaKeyword(String str) {
        String[] keywords = {"abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void", "volatile", "while"};
        for (String keyword : keywords) {
            if (str.equals(keyword)) {
                return true;
            }
        }
        return false;
    }

    protected String randomValue(Type type) {
        return randomValue(type, true);
    }

    protected String randomValue(Type type, boolean asValue) {
        if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            if (!clazz.isPrimitive()) {
                this.addImport(clazz.getName());
            }
            if (clazz.isPrimitive()) {
                if (clazz.equals(boolean.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextBoolean());
                } else if (clazz.equals(byte.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Byte.MAX_VALUE));
                } else if (clazz.equals(short.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Short.MAX_VALUE));
                } else if (clazz.equals(int.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Short.MAX_VALUE));
                } else if (clazz.equals(long.class)) {
                    return ThreadLocalRandom.current().nextLong(Integer.MAX_VALUE) + (asValue ? "L" : "");
                } else if (clazz.equals(float.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextFloat());
                } else if (clazz.equals(double.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextDouble());
                } else if (clazz.equals(char.class)) {
                    return String.valueOf((char) ThreadLocalRandom.current().nextInt(Character.MIN_VALUE, Character.MAX_VALUE));
                }
            } else if (clazz.equals(String.class)) {
                if (asValue) {
                    return "\"" + Tool.randomAlphanumeric(24) + "\"";
                } else {
                    return Tool.randomAlphanumeric(24);
                }
            } else if (clazz.equals(Boolean.class)) {
                return String.valueOf(ThreadLocalRandom.current().nextBoolean());
            } else if (clazz.equals(Character.class)) {
                return String.valueOf((char) ThreadLocalRandom.current().nextInt(Character.MIN_VALUE, Character.MAX_VALUE));
            } else if (Number.class.isAssignableFrom(clazz)) {
                if (clazz.equals(Byte.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Byte.MAX_VALUE));
                } else if (clazz.equals(Short.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Short.MAX_VALUE));
                } else if (clazz.equals(Integer.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextInt(Short.MAX_VALUE));
                } else if (clazz.equals(Long.class)) {
                    return ThreadLocalRandom.current().nextLong(Integer.MAX_VALUE) + (asValue ? "L" : "");
                } else if (clazz.equals(Float.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextFloat());
                } else if (clazz.equals(Double.class)) {
                    return String.valueOf(ThreadLocalRandom.current().nextDouble());
                } else if (clazz.equals(BigDecimal.class)) {
                    double v = ThreadLocalRandom.current().nextDouble();
                    return asValue ? "new BigDecimal(" + v + ")" : String.valueOf(v);
                }
            } else if (clazz.equals(LocalDate.class)) {
                return asValue ? "LocalDate.now()" : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } else if (clazz.equals(LocalDateTime.class)) {
                return asValue ? "LocalDateTime.now()" : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else if (clazz.equals(LocalTime.class)) {
                return asValue ? "LocalTime.now()" : LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            } else if (clazz.isEnum() && clazz.getEnumConstants().length > 0) {
                int index = ThreadLocalRandom.current().nextInt(clazz.getEnumConstants().length);
                return asValue
                        ? takeDefName(clazz) + "." + clazz.getEnumConstants()[index].toString()
                        : clazz.getEnumConstants()[index].toString();
            } else if (JsonNode.class.isAssignableFrom(clazz)) {
                this.addImport(ObjectMapper.class.getName());
                return "new ObjectMapper().createObjectNode()";
            } else if (File.class.isAssignableFrom(clazz)) {
                this.addImport(File.class.getName());
                return "new File(\"/tmp/abc.txt\")";
            }
        }
        throw new RuntimeException("type is not Class: " + type.getTypeName());
    }

    protected String lowerFirstChar(String str) {
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    protected String upperFirstChar(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    protected Class toClass(Type type) {
        try {
            return Class.forName(type.getTypeName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected boolean addImport(String className) {
        className = className.trim();
        if (className.endsWith("[]")) className = className.substring(0, className.length() - 2);
        if (!className.contains(".")) {
            return false;
        }
        if (className.contains("<")) {
            int index = className.indexOf("<");
            String[] typeNames = className.substring(index + 1, className.length() - 1).split(",");
            for (String typeName : typeNames) {
                addImport(typeName);
            }
            return addImport(className.substring(0, index));
        }
        int childIndex = className.indexOf("$");
        if (childIndex != -1) {
            className = className.substring(0, childIndex);
        }
        String[] arr = className.split("\\.");
        String importSimpleName = arr[arr.length - 1];
        if (this.importSet.contains(className)) {
            return true;
        }
        if (this.importSimpleSet.contains(importSimpleName)) {
//            throw new RuntimeException("import中已存在" + importSimpleName);
            return false;
        }
        this.importSet.add(className);
        this.importSimpleSet.add(importSimpleName);
        return true;
    }

    /**
     * 是否是内部非静态类
     * @param typeName
     * @return
     */
    private boolean isInnerNonStaticClass(String typeName) {
        try {
            Class<?> clazz = Class.forName(typeName);
            if (clazz.isMemberClass() && !Modifier.isStatic(clazz.getModifiers())) {
                return true;
            }
        } catch (ClassNotFoundException e) {
        }
        return false;
    }

    protected String takeImplName(String typeName) {
        String defName = takeDefName(typeName);
        if (defName.contains(".")) {
            // 处理非静态内部类
            int prefixLength = typeName.length() - defName.length();
            int endIndex = typeName.length();
            List<String> nameList = new LinkedList<>();
            while (true) {
                if (isInnerNonStaticClass(typeName.substring(0, endIndex))) {
                    int startIndex = typeName.lastIndexOf("$", endIndex - 1);
                    nameList.add(".new " + typeName.substring(startIndex + 1, endIndex) + "()");
                    endIndex = startIndex;
                } else {
                    nameList.add("new " + typeName.substring(prefixLength, endIndex).replaceAll("\\$", ".") + "()");
                    break;
                }
            }
            Collections.reverse(nameList);
            String implName = String.join("", nameList);
            if (implName.endsWith("()")) {
                return implName.substring(0, implName.length() - 2);
            } else {
                return implName;
            }
        } else {
            return "new " + defName;
        }
    }

    protected String takeDefName(Type type) {
        return takeDefName(type.getTypeName());
    }

    protected String takeDefName(String typeName) {
        typeName = typeName.trim();
        if (typeName.contains("<")) {
            int index = typeName.indexOf("<");
            String defName = takeDefName(typeName.substring(0, index));
            defName += "<";
            String[] genericParts = typeName.substring(index + 1, typeName.length() - 1).split(",");
            defName += Arrays.stream(genericParts).map(part -> takeDefName(part)).collect(Collectors.joining(", "));
            defName += ">";
            return defName;
        }
        boolean addSuccess = this.addImport(typeName);
        if (addSuccess) {
            String[] arr = typeName.split("\\.");
            return arr[arr.length - 1].replaceAll("\\$", ".");
        } else {
            return typeName.replaceAll("\\$", ".");
        }
    }

    protected Class takeRawClass(Type targetType) {
        try {
            String typeName = targetType.getTypeName();
            if (typeName.contains("<")) {
                int index = typeName.indexOf("<");
                return Class.forName(typeName.substring(0, index));
            } else {
                return Class.forName(typeName);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    protected Class takeImplClass(Type targetType) {
        Class<?> clazz = null;
        if (targetType instanceof Class) {
            clazz = (Class<?>) targetType;
        } else if (targetType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) targetType).getRawType();
            if (rawType instanceof Class) {
                clazz = (Class<?>) rawType;
            }
        }
        if (clazz == null) {
            System.out.println("targetType can not found valid impl class: " + targetType.getTypeName());
            return null;
        }
        try {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers())) {
                if (clazz.getTypeName().equals("com.fasterxml.jackson.databind.JsonNode")) {
                    this.addImport("com.fasterxml.jackson.databind.node.ObjectNode");
                    return Class.forName("com.fasterxml.jackson.databind.node.ObjectNode");
                } else if (clazz.equals(MultipartFile.class)) {
                    this.addImport(MockMultipartFile.class.getTypeName());
                    return MockMultipartFile.class;
                } else if (clazz.equals(HttpServletResponse.class)) {
                    this.addImport(MockHttpServletResponse.class.getTypeName());
                    return MockHttpServletResponse.class;
                } else if (clazz.getTypeName().equals("com.baomidou.mybatisplus.core.metadata.IPage")) {
                    this.addImport("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
                    return Class.forName("com.baomidou.mybatisplus.extension.plugins.pagination.Page");
                } 
//                else if (clazz.isInterface()) {
//                    for (Class<?> implClass : clazz.getClasses()) {
//                        if (implClass.isAssignableFrom(clazz)) {
//                            return implClass;
//                        }
//                    }
//                }
                System.out.println("接口或抽象类找不到实现类: " + clazz.getTypeName());
                return null;
            }
            return clazz;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
