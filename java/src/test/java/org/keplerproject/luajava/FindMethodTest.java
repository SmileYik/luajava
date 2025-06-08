package org.keplerproject.luajava;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FindMethodTest {
    static {
        LoadLibrary.load();
    }

    @Test
    public void aTest() {
        LuaState L = LuaStateFactory.newLuaState();
        L.openLibs();


        L.pushJavaObject(A.class);
        L.setGlobal("a");

        int exp = L.LdoFile("test/findMethodTest.lua");
        if (exp != 0) {
            System.out.println(L.toString(-1));
        }




        L.close();
    }
}

class A {
    public static void a(String s) {
        System.out.println("A String: " + s);
    }

    public static void a(int i) {
        System.out.println("A int: " + i);
    }

    public static void a(short s) {
        System.out.println("A short: " + s);
    }

    public static void a(byte b) {
        System.out.println("A byte: " + b);
    }

    public static void a(char c) {
        System.out.println("A char: " + c);
    }

    public static void a(double d) {
        System.out.println("A double: " + d);
    }

    public static void a(Double d) {
        System.out.println("A Double: " + d);
    }

    public static void a(float f) {
        System.out.println("A float: " + f);
    }

    public static void a(long l) {
        System.out.println("A long: " + l);
    }

    public static void a(boolean b) {
        System.out.println("A boolean: " + b);
    }

    public static void a(Object obj) {
        System.out.println("A object: " + obj);
    }

    public static void a(String[] str) {
        System.out.println("A string[]: " + Arrays.toString(str));
    }

    public static void a(int[] array) {
        System.out.println("A int[]: " + Arrays.toString(array));
    }

    public static void a(short[] array) {
        System.out.println("A short[]: " + Arrays.toString(array));
    }

    public static void a(byte[] array) {
        System.out.println("A byte[]: " + Arrays.toString(array));
    }

    public static void a(char[] array) {
        System.out.println("A char[]: " + Arrays.toString(array));
    }

    public static void a(double[] array) {
        System.out.println("A double[]: " + Arrays.toString(array));
    }

    public static void a(float[] array) {
        System.out.println("A float[]: " + Arrays.toString(array));
    }

    public static void a(long[] array) {
        System.out.println("A long[]: " + Arrays.toString(array));
    }

    public static void a(boolean[] array) {
        System.out.println("A boolean[]: " + Arrays.toString(array));
    }

    public static void a(Object[] array) {
        System.out.println("A Object[]: " + Arrays.toString(array));
    }
}
