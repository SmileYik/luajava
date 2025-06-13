package org.keplerproject.luajava;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class FindMethodTest {
    static {
        LoadLibrary.load();
    }

    @Test
    public void aTest() throws Exception {
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        facade.setGlobal("a", A.class).justThrow();
        facade.evalFile("test/findMethodTest.lua").justThrow();
        facade.close();
    }
}

class A {
    public static byte aa = 1;
    private static short bb = 2;
    protected static int cc = 3;
    public static double dd = 4;
    public static float ee = 5;
    public static final long ff = 6;
    public static char gg = 'a';
    public String hh = "8";


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

//    public static void a(Double[] array) {
//        System.out.println("A Double[]: " + Arrays.toString(array));
//    }

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

    public static void b(Double a, Double b, Double c, Double d, Double e, Object o) {
        System.out.println("5double + 1object: " + a + " " + b + " " + c + " " + d + " " + e + " " + o);
    }

    public static void b(Double a, Double b, Double c, Double d, Double e, float f) {
        System.out.println("5Double + 1float: " + a + " " + b + " " + c + " " + d + " " + e + " " + f);
    }

    public static void b(double a, double b, double c, double d, double e, double f, double g, double h, double i, Object o) {
        System.out.println("9double + 1object: " + a + " " + b + " " + c + " " + d + " " + e + " " + o);
    }

    public static void b(Double a, Double b, Double c, Double d, Double e, Double f, Double g, Double h, Double i, float j) {
        System.out.println("9Double + 1float: " + a + " " + b + " " + c + " " + d + " " + e + " " + f);
    }
}
