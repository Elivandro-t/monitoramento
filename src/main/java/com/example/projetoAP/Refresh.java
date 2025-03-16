package com.example.projetoAP;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class Refresh {

    public <T,P> P getReflection(T input) throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        Class<?> code = input.getClass();
        Class<?> target = Class.forName(code.getName());

        Field[] field = code.getDeclaredFields();
        Field[] fieldTarget = target.getDeclaredFields();

        P ps = (P) target.getDeclaredConstructor().newInstance();
        return ps;
    }
}
