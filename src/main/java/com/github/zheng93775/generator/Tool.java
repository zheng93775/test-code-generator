package com.github.zheng93775.generator;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

class Tool {
    private final static String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    public static <T> T or(T a, T b) {
        return a == null ? b : a;
    }

    public static boolean hasObjectParameter(Method method) {
        if (method.getReturnType().equals(Object.class)) {
            return true;
        }
        Class[] types = method.getParameterTypes();
        for (Class childType : types) {
            if (Object.class.equals(childType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasGenericParameter(Method method) {
        if (isGenericParameter(method.getGenericReturnType())) {
            return true;
        }
        Type[] types = method.getGenericParameterTypes();
        for (Type childType : types) {
            if (isGenericParameter(childType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isGenericParameter(Type type) {
        if (type == null) {
            return false;
        }
        if (type instanceof Class && ((Class) type).isPrimitive()) {
            return false;
        }
        if (!type.getTypeName().contains(".")) {
            return true;
        }
        if (type != null && type instanceof ParameterizedType) {
            Type[] types = ((ParameterizedType) type).getActualTypeArguments();
            for (Type childType : types) {
                if (isGenericParameter(childType)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static String randomAlphanumeric(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            int index = (int) (Math.random() * CHARS.length());
            sb.append(CHARS.charAt(index));
        }
        return sb.toString();
    }
}
