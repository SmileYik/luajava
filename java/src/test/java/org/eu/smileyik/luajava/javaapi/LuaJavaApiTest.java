package org.eu.smileyik.luajava.javaapi;

import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LoadLibrary;
import org.keplerproject.luajava.LuaStateFacade;
import org.keplerproject.luajava.LuaStateFactory;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class LuaJavaApiTest {
    static {
        LoadLibrary.load();
    }

    @Test
    public void arrayIndexTest() throws Exception {
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        String[] strings = new String[] {"hello", ", ", "world", "!"};
        int[] ints = new int[] {1, 2, 3, 4, 5, 6};
        facade.setGlobal("strings", strings).justThrow();
        facade.setGlobal("strs", strings).justThrow();
        facade.setGlobal("ints", ints).justThrow();
        List<String> list = Arrays.asList(strings);
        facade.setGlobal("list", list).justThrow();
        Map<String, String> map = new HashMap<>();
        map.put("hello", "world");
        map.put("world", "!");
        facade.setGlobal("map", map).justThrow();
        facade.setGlobal("obj", new Object()).justThrow();

        facade.evalString("print('#strs = ', #strs)").justThrow();
        facade.evalString("print('#list = ', #list)").justThrow();
        facade.evalString("print('#map = ', #map)").justThrow();
//        facade.evalString("print('#obj = ', #obj)").justThrow();
        facade.evalString("print(strs == strings, strs == strs, strings.__len, getmetatable(strs).__eq(strs, strings))").justThrow();
        facade.evalString("print(getmetatable(strs).__eq, getmetatable(strings).__eq, getmetatable(ints).__eq)").getOrSneakyThrow();
        facade.evalString("print(getmetatable(strs), getmetatable(strings), getmetatable(ints))").getOrSneakyThrow();
        facade.evalString("print(('123'..strings)..'456')").justThrow();
    }

    @Test
    public void reflectTest() throws Exception {
        String lua = "print(getmetatable(abc)); function findMethods(className)\n" +
                "    print(\"----- find: \" .. className)\n" +
                "    local class = luajava.bindClass(className)\n" +
                "    class = luajava.class2Obj(class)\n" +
                // "    setmetatable(class, getmetatable(abc))\n" +
                "    while class ~= nil do\n" +
                "        print(\"find: \" .. class)\n" +
                "        local methods = class:getDeclaredMethods()\n" +
                "        for i=1, #methods do\n" +
                "            local method = methods[i]\n" +
                "            print(method .. \"\")\n" +
                "        end\n" +
                "        class = class:getSuperclass()\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "findMethods(\"org.keplerproject.luajava.LuaJavaAPI\")";
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.setGlobal("abc", new Object());
        facade.openLibs();
        facade.evalString(lua).justThrow();
    }

    public static void main(String[] args) throws Exception {
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        String[] strings = new String[] {"hello", ", ", "world", "!"};
        facade.setGlobal("strings", strings).justThrow();
        facade.setGlobal("strs", strings).justThrow();
        facade.evalString("print(strs == strings, strs, strings)").justThrow();
    }

    @Test
    public void fieldTest() throws Exception {
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        FieldEntity fieldEntity = new FieldEntity();
        FieldEntity.AField newField = new FieldEntity.AField() {
            public String b = "bbbbbb";
            @Override
            public String toString() {
                return (b);
            }
        };
        facade.setGlobal("f", fieldEntity).justThrow();
        facade.setGlobal("cf", FieldEntity.class);
        facade.setGlobal("field", newField);
        facade.evalFile("test/fieldEntityTest.lua").justThrow();
        facade.evalString("f.pf = 1000; f.psf = 2000").justThrow();
        assertEquals(1000d, fieldEntity.pf);
        assertEquals(2000d, FieldEntity.psf);
        facade.evalString("print(cf.f)").justThrow();
        System.out.println(fieldEntity);
        FieldEntity.AField pj = fieldEntity.pj;
        facade.evalString("f.pj=nil").justThrow();
        FieldEntity.AField pj2 = fieldEntity.pj;
        assertNull(pj2);
        facade.evalString("f.pj=field").justThrow();
        assertEquals(newField, fieldEntity.pj);
        facade.evalString("print(f.pj:toString())").justThrow();
        facade.evalString("f.j=field").justThrow();
        facade.evalString("print(f.j:toString())").justThrow();
        facade.close();
    }

    @Test
    public void aTest() throws Exception {
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        facade.setGlobal("a", A.class).justThrow();
        facade.evalFile("test/findMethodTest.lua").justThrow();
        facade.evalString("print(a:a(1))").justThrow();
        facade.close();
    }

    @Test
    public void nestTest() throws Exception {
        List<String> self = new ArrayList<>();
        for (int i = 0; i < 100; ++i) {
            self.add("self()");
        }
        String lua = String.format("print(nest:%s:deep())", String.join(":", self));
        LuaStateFacade facade = LuaStateFactory.newLuaState();
        facade.openLibs();
        facade.setGlobal("nest", new Nest()).justThrow();
        facade.evalString(lua).justThrow();
        facade.evalString("nest:print('123' .. nest:getName() .. '456')").justThrow();
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

class Nest {
    private int i = 0;
    public Nest self() {
        i += 1;
        return this;
    }

    public int deep() {
        return i;
    }

    public String getName() {
        return "Name";
    }

    public void print(String str) {
        System.out.println(str);
    }
}
