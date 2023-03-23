package com.github.zheng93775.generator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

class DefParameterizedType implements ParameterizedType {
    private ParameterizedType genericType;

    private Map<String, Type> typeMap;

    public static DefParameterizedType wrap(Type ownerGenericType, ParameterizedType genericType) {
        DefParameterizedType defParameterizedType = new DefParameterizedType();
        defParameterizedType.genericType = genericType;
        defParameterizedType.typeMap = new HashMap<>();
        if (ownerGenericType != null && ownerGenericType instanceof ParameterizedType) {
            Type rawType = ((ParameterizedType) ownerGenericType).getRawType();
            if (rawType instanceof Class) {
                TypeVariable<Class>[] typeVariables = ((Class) rawType).getTypeParameters();
                Type[] argTypes = ((ParameterizedType) ownerGenericType).getActualTypeArguments();
                for (int i = 0; i < argTypes.length && i < typeVariables.length; i++) {
                    defParameterizedType.typeMap.put(typeVariables[i].getName(), argTypes[i]);
                }
            }
        }
        return defParameterizedType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        Type[] types = Arrays.copyOf(genericType.getActualTypeArguments(), genericType.getActualTypeArguments().length);
        for (int i = 0; i < types.length; i++) {
            Type type = types[i];
            if (this.typeMap.containsKey(type.getTypeName())) {
                types[i] = this.typeMap.get(type.getTypeName());
            }
        }
        return types;
    }

    @Override
    public Type getRawType() {
        return genericType.getRawType();
    }

    @Override
    public Type getOwnerType() {
        return genericType.getOwnerType();
    }

    @Override
    public String getTypeName() {
        String typeName = genericType.getRawType().getTypeName();
        if (genericType.getActualTypeArguments().length > 0) {
            typeName += "<";
            typeName += Arrays.stream(this.genericType.getActualTypeArguments()).map(type -> {
                if (this.typeMap.containsKey(type.getTypeName())) {
                    return this.typeMap.get(type.getTypeName()).getTypeName();
                } else {
                    return type.getTypeName();
                }
            }).collect(Collectors.joining(", "));
            typeName += ">";
        }
        return typeName;
    }
}
