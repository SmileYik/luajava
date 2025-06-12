package org.eu.smileyik.luajava.type;

import org.eu.smileyik.luajava.BaseTest;
import org.junit.jupiter.api.Test;
import org.keplerproject.luajava.LuaException;
import org.keplerproject.luajava.LuaObject;
import org.keplerproject.luajava.LuaState;
import org.keplerproject.luajava.LuaStateFacade;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class LuaArrayTest extends BaseTest {

    @Test
    public void createTest() throws Throwable {
        String lua = "map = {a = 1, b = 2, c = '3', d = function() print(4) end}\n" +
                "array = {1, 'a', 2, 'b', 3, function() print('c') end}";
        LuaStateFacade facade = newLuaState();
        facade.lockThrow(L -> {
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            LuaObject luaObject = facade.getLuaObject("map").getOrThrow(LuaException.class);
            System.out.println(luaObject);
            assert luaObject instanceof LuaTable;
            assert !(luaObject instanceof LuaArray);

            luaObject = facade.getLuaObject("array").getOrThrow(LuaException.class);
            assert luaObject instanceof LuaTable;
            assert luaObject instanceof LuaArray;
            System.out.println(((LuaArray) luaObject).isArray());

        });

        facade.close();
    }

    @Test
    public void forEachTest() throws Throwable {
        String lua = "array = {1, 'a', 2, 'b', 3, function() print('c') end}\n" +
                "strs = {'1', '2', '3', '4', '5', '6', '7', '8', '9'}\n" +
                "nums = {1, 2, 3, 4, 5, 6, 7, 8, 9}\n" +
                "bools = {true, false, true, false}\n" +
                "funcs = {function() end, function() end, function() end, function() end}\n" +
                "tables = {{i = 0}, {}, {}, {}, {}, {}, {}, {}}\n";
        LuaStateFacade facade = newLuaState();
        facade.lockThrow(L -> {
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            LuaObject luaObject = facade.getLuaObject("array").getOrThrow(LuaException.class);
            assert luaObject instanceof LuaArray;
            LuaArray array = (LuaArray) luaObject;
            try {
                array.forEach((idx, obj) -> {
                    System.out.printf("[%d] %s: %s\n", (Integer) idx, obj, obj.getClass());
                });
                array.forEachValue(obj -> {
                    System.out.printf("%s: %s\n", obj, obj.getClass());
                });

                array = (LuaArray) facade.getLuaObject("strs").getOrThrow(LuaException.class);
                array.forEachValue(String.class, System.out::println);

                array = (LuaArray) facade.getLuaObject("nums").getOrThrow(LuaException.class);
                array.forEachValue(Number.class, System.out::println);

                array = (LuaArray) facade.getLuaObject("bools").getOrThrow(LuaException.class);
                array.forEachValue(Boolean.class, System.out::println);

                array = (LuaArray) facade.getLuaObject("funcs").getOrThrow(LuaException.class);
                array.forEachValue(LuaFunction.class, it -> System.out.println(it + ": " + it.getClass()));

                array = (LuaArray) facade.getLuaObject("tables").getOrThrow(LuaException.class);
                array.forEachValue(LuaTable.class, it -> System.out.println(it + ": " + it.getClass()));

                createArray(L, "objs", new Object[] {new Object(), new Object()});
                array = (LuaArray) facade.getLuaObject("objs").getOrThrow(LuaException.class);
                array.forEachValue(System.out::println);

                createAArray(L, "as");
                array = (LuaArray) facade.getLuaObject("as").getOrThrow(LuaException.class);
                array.forEachValue(A.class, System.out::println);

                createAArrayArray(L, "ass");
                array = (LuaArray) facade.getLuaObject("ass").getOrThrow(LuaException.class);
                array.forEachValue(A[].class, System.out::println);
                
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        facade.close();
    }

    @Test
    public void toListTest() throws Throwable {
        String lua = "array = {1, 'a', 2, 'b', 3, function() print('c') end}\n" +
                "strs = {'1', '2', '3', '4', '5', '6', '7', '8', '9'}\n" +
                "nums = {1, 2, 3, 4, 5, 6, 7, 8, 9}\n" +
                "bools = {true, false, true, false}\n" +
                "funcs = {function() end, function() end, function() end, function() end}\n" +
                "tables = {{i = 0}, {}, {}, {}, {}, {}, {}, {}}\n" +
                "chars = {'a', 'b', 'c', 'd'}\n" +
                "d2list = {{1, 2, 3}, {4, 5, 6}, {7}, {}, {8, 9}}\n" +
                "d2list_str = {{'1'}, {'str'}}";
        LuaStateFacade facade = newLuaState();
        facade.lockThrow(L -> {
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            LuaObject luaObject = facade.getLuaObject("array").getOrThrow(LuaException.class);
            assert luaObject instanceof LuaArray;
            LuaArray array = (LuaArray) luaObject;
            try {
                System.out.println(array.asList(Object.class).getOrThrow());

                array = (LuaArray) facade.getLuaObject("strs").getOrThrow(LuaException.class);
                assert array.asList(String.class).getOrSneakyThrow().toString().equals("[1, 2, 3, 4, 5, 6, 7, 8, 9]");

                array = (LuaArray) facade.getLuaObject("nums").getOrThrow(LuaException.class);
                assert array.asList(Number.class).getOrSneakyThrow().toString().equals("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]");
                assert array.asList(Double.class).getOrSneakyThrow().toString().equals("[1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0]");

                array = (LuaArray) facade.getLuaObject("bools").getOrThrow(LuaException.class);
                assert array.asList(Boolean.class).getOrSneakyThrow().toString().equals("[true, false, true, false]");

                array = (LuaArray) facade.getLuaObject("chars").getOrThrow(LuaException.class);
                assert Objects.equals(array.asList(Character.class).getOrSneakyThrow(), Arrays.asList('a', 'b', 'c', 'd')) : "as Character List Failed";

                array = (LuaArray) facade.getLuaObject("d2list").getOrThrow(LuaException.class);
                assert Objects.equals(array.asDeepList(Double.class).getOrSneakyThrow(),
                        Arrays.asList(Arrays.asList(1d, 2d, 3d),
                                Arrays.asList(4d, 5d, 6d),
                                Arrays.asList(7d),
                                Arrays.asList(),
                                Arrays.asList(8d, 9d)));

                array = (LuaArray) facade.getLuaObject("d2list_str").getOrThrow(LuaException.class);
                assert Objects.equals(array.asDeepList(String.class).getOrSneakyThrow(), Arrays.asList(
                        Arrays.asList("1"), Arrays.asList("str")
                ));

                array = (LuaArray) facade.getLuaObject("funcs").getOrThrow(LuaException.class);
                System.out.println(array.asList(LuaFunction.class).getOrSneakyThrow());

                array = (LuaArray) facade.getLuaObject("tables").getOrThrow(LuaException.class);
                System.out.println(array.asList(LuaTable.class).getOrSneakyThrow());

                createArray(L, "objs", new Object[] {new Object(), new Object()});
                array = (LuaArray) facade.getLuaObject("objs").getOrThrow(LuaException.class);
                System.out.println(array.asList().getOrSneakyThrow());

                createAArray(L, "as");
                array = (LuaArray) facade.getLuaObject("as").getOrThrow(LuaException.class);
                System.out.println(array.asList(A.class).getOrSneakyThrow());

                createAArrayArray(L, "ass");
                array = (LuaArray) facade.getLuaObject("ass").getOrThrow(LuaException.class);
                System.out.println(array.asList(A[].class).getOrSneakyThrow());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });
        facade.close();
    }

    @Test
    public void toArrayTest() throws Throwable {
        String lua = "array = {1, 'a', 2, 'b', 3, function() print('c') end}\n" +
                "strs = {'1', '2', '3', '4', '5', '6', '7', '8', '9'}\n" +
                "nums = {1, 2, 3, 4, 5, 6, 7, 8, 9}\n" +
                "bools = {true, false, true, false}\n" +
                "funcs = {function() end, function() end, function() end, function() end}\n" +
                "tables = {{i = 0}, {}, {}, {}, {}, {}, {}, {}}\n" +
                "chars = {'a', 'b', 'c', 'd'}";
        LuaStateFacade facade = newLuaState();
        facade.lockThrow(L -> {
            int exp = L.LdoString(lua);
            if (exp != 0) {
                throw new LuaException(L.toString(-1));
            }
            try {
                LuaObject luaObject = facade.getLuaObject("array").getOrThrow(LuaException.class);
                assert luaObject instanceof LuaArray;
                LuaArray array = (LuaArray) luaObject;
                System.out.println(array.asList(Object.class));

                array = (LuaArray) facade.getLuaObject("strs").getOrThrow(LuaException.class);
                assert Arrays.equals(array.asArray(String.class).getOrSneakyThrow(), new String[] {"1", "2", "3", "4", "5", "6", "7", "8", "9"});

                array = (LuaArray) facade.getLuaObject("nums").getOrThrow(LuaException.class);
                assert Arrays.equals(array.asArray(Number.class).getOrSneakyThrow(), new Number[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0});
                assert Arrays.equals(array.asArray(Double.class).getOrSneakyThrow(), new Number[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0});
                assert Arrays.equals(array.toDoubleArray().getOrSneakyThrow(), new double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0});
                assert Arrays.equals(array.toFloatArray().getOrSneakyThrow(), new float[] {1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f});
                assert Arrays.equals(array.toLongArray().getOrSneakyThrow(), new long[] {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L});
                assert Arrays.equals(array.asPrimitiveArray(double[].class).getOrSneakyThrow(), new double[] {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0});
                System.out.println("primitive array: " + Arrays.toString(array.asPrimitiveArray(double[].class).getOrSneakyThrow()));

                array = (LuaArray) facade.getLuaObject("bools").getOrThrow(LuaException.class);
                assert Arrays.equals(array.asArray(Boolean.class).getOrSneakyThrow(), new Boolean[]{true, false, true, false});
                assert Arrays.equals(array.toBooleanArray().getOrSneakyThrow(), new boolean[]{true, false, true, false});

                array = (LuaArray) facade.getLuaObject("chars").getOrThrow(LuaException.class);
                assert Arrays.equals(array.asArray(Character.class).getOrSneakyThrow(), new Character[]{'a', 'b', 'c', 'd'}) : "as Character Array Failed";
                assert Arrays.equals(array.toCharArray().getOrSneakyThrow(), new char[]{'a', 'b', 'c', 'd'}) : "toCharArray Failed";

                array = (LuaArray) facade.getLuaObject("funcs").getOrThrow(LuaException.class);
                System.out.println(Arrays.toString(array.asArray(LuaFunction.class).getOrSneakyThrow()));

                array = (LuaArray) facade.getLuaObject("tables").getOrThrow(LuaException.class);
                System.out.println(Arrays.toString(array.asArray(LuaTable.class).getOrSneakyThrow()));

                createArray(L, "objs", new Object[] {new Object(), new Object()});
                array = (LuaArray) facade.getLuaObject("objs").getOrThrow(LuaException.class);
                System.out.println(Arrays.toString(array.asArray().getOrSneakyThrow()));

                createAArray(L, "as");
                array = (LuaArray) facade.getLuaObject("as").getOrThrow(LuaException.class);
                System.out.println(Arrays.toString(array.asArray(A.class).getOrSneakyThrow()));

                createAArrayArray(L, "ass");
                array = (LuaArray) facade.getLuaObject("ass").getOrThrow(LuaException.class);
                System.out.println(Arrays.deepToString(array.asArray(A[].class).getOrSneakyThrow()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        });

        facade.close();
    }

    @Test
    public void atInBoundNumberTest() throws Exception {
        atTestPrepare("array", array -> {
            assertEquals(1d, array.at(0).getOrSneakyThrow(), "array[0] need equal 1.0");
            assertEquals(2d, array.at(2).getOrSneakyThrow(), "array[2] need equal 2.0");
            assertEquals(3d, array.at(4).getOrSneakyThrow(), "array[4] need equal 2.0");
        });
    }

    @Test
    public void atInBoundStringTest() throws Exception {
        atTestPrepare("array", array -> {
            assertEquals("a", array.at(1).getOrSneakyThrow(), "array[1] need equal \"a\"");
        });
    }

    @Test
    public void atInBoundFunctionTest() throws Exception {
        atTestPrepare("array", array -> {
            Object func = array.at(5).getOrSneakyThrow();
            assertInstanceOf(LuaFunction.class, func, "array[5] need be LuaFunction");
            ((LuaFunction) func).call().getOrSneakyThrow();
        });
    }

    @Test
    public void atInBoundArrayTest() throws Exception {
        atTestPrepare("d2list", array -> {
            Object obj = array.at(0).getOrSneakyThrow();
            assertInstanceOf(LuaArray.class, obj, "array[5] need be LuaArray");
            LuaArray a = (LuaArray) obj;
            assertEquals(a.asList(Double.class).getOrSneakyThrow(), Arrays.asList(1D, 2D, 3D));
        });
    }

    @Test
    public void atOutBoundLeftTest() throws Exception {
        atTestPrepare("d2list", array -> {
            assertThrows(LuaException.class, () -> {
                array.at(-1).justThrow();
            });
            assertThrows(LuaException.class, () -> {
                array.at(-10).justThrow();
            });
            assertThrows(LuaException.class, () -> {
                array.at(-100).justThrow();
            });
        });
    }

    @Test
    public void atOutBoundRightTest() throws Exception {
        atTestPrepare("d2list", array -> {
            assertThrows(LuaException.class, () -> {
                array.at(Integer.MAX_VALUE).justThrow();
            });
            assertThrows(LuaException.class, () -> {
                array.at(Short.MAX_VALUE).justThrow();
            });
            assertThrows(LuaException.class, () -> {
                array.at(999).justThrow();
            });
        });
    }

    private void atTestPrepare(String arrayName, Consumer<LuaArray> consumer) throws Exception {
        String lua = "array = {1, 'a', 2, 'b', 3, function() print('c') end}\n" +
                "strs = {'1', '2', '3', '4', '5', '6', '7', '8', '9'}\n" +
                "nums = {1, 2, 3, 4, 5, 6, 7, 8, 9}\n" +
                "bools = {true, false, true, false}\n" +
                "funcs = {function() end, function() end, function() end, function() end}\n" +
                "tables = {{i = 0}, {}, {}, {}, {}, {}, {}, {}}\n" +
                "chars = {'a', 'b', 'c', 'd'}\n" +
                "d2list = {{1, 2, 3}, {4, 5, 6}, {7}, {}, {8, 9}}\n" +
                "d2list_str = {{'1'}, {'str'}}";
        try (LuaStateFacade facade = newLuaState()) {
            facade.evalString(lua)
                    .mapResultValue(v -> facade.lockThrow(L -> {
                        createArray(L, "objs", new Object[] {new Object(), new Object()});
                        createAArray(L, "as");
                        createAArrayArray(L, "ass");
                    }))
                    .mapResultValue(v -> facade.getGlobal(arrayName, LuaArray.class)
                            .justCast(LuaArray.class, LuaException.class))
                    .ifSuccessThen(consumer)
                    .justThrow(LuaException.class);
        }
    }

    @Test
    public void arraySetTest() throws Exception {
        String lua = "array = {1, 2, 3, 4, 5, 6, 7, 8, 9} \n" +
                "function print_array() \n" +
                "  for i, v in pairs(array) do \n" +
                "    print(i, v) \n" +
                "  end\n" +
                "  print('-------------')\n" +
                "end";
        try (LuaStateFacade facade = newLuaState()) {
            facade.evalString(lua).justThrow();
            LuaFunction printArray = facade.getGlobal("print_array", LuaFunction.class).getOrThrow();
            LuaArray array = facade.getGlobal("array", LuaArray.class).getOrThrow();
            printArray.call().getOrThrow();
            assertEquals(2.0, array.at(1).getOrSneakyThrow());
            assertEquals(3.0, array.at(2).getOrSneakyThrow());
            array.set(1, 1);
            array.set(2, 2);
            assertEquals(1.0, array.at(1).getOrSneakyThrow());
            assertEquals(2.0, array.at(2).getOrSneakyThrow());
            printArray.call().getOrThrow();
            // out bound
            assertThrows(LuaException.class, () -> {array.set(-1, 1).getOrThrow();});
            printArray.call().getOrThrow();
            // out bound
            array.set(9, 1);
            assertThrows(LuaException.class, () -> {array.set(9, 1).getOrThrow();});
            printArray.call().getOrThrow();
            // set nil
            assertThrows(LuaException.class, () -> {array.set(9, null).getOrThrow();});
            array.set(8, null).getOrThrow();
            printArray.call().getOrThrow();

            // set self
            array.set(8, array).getOrThrow();
            printArray.call().getOrThrow();
            array.set(7, array).getOrThrow();
            printArray.call().getOrThrow();

            System.out.println(array);

            LuaArray innerArray = (LuaArray) array.at(8).getOrThrow();
            System.out.println(innerArray.equals(array));
            System.out.println();
        }
    }

    @Test
    public void arrayAddTest() throws Exception {
        String lua = "array = {1, 2, 3, 4, 5, 6, 7, 8, 9} \n" +
                "function print_array() \n" +
                "  for i, v in pairs(array) do \n" +
                "    print(i, v) \n" +
                "  end\n" +
                "  print('-------------')\n" +
                "end";
        try (LuaStateFacade facade = newLuaState()) {
            facade.evalString(lua).justThrow();
            LuaFunction printArray = facade.getGlobal("print_array", LuaFunction.class).getOrThrow();
            LuaArray array = facade.getGlobal("array", LuaArray.class).getOrThrow();
            printArray.call().getOrThrow();
            assert array.length() == 9;
            array.add(10);
            assert array.length() == 10;
            printArray.call().getOrThrow();
            array.add(11);
            array.add(12);
            array.add(13);
            assert array.length() == 13;
            printArray.call().getOrThrow();
        }
    }

    private void createAArray(LuaState L, String name) {
        A[] as = new A[] {new A(), new A(), new A(), new A(), new A()};
        createArray(L, name, as);
    }

    private void createAArrayArray(LuaState L, String name) {
        A[] as = new A[] {new A(), new A(), new A(), new A(), new A()};
        createArray(L, name, new A[][] {as, as});
    }

    private void createArray(LuaState L, String name, Object[] array) {
        L.newTable();
        for (int i = 0; i < array.length; i++) {
            L.pushJavaObject(array[i]);
            L.rawSetI(-2, i + 1);
        }
        L.setGlobal(name);
    }


    final static class A {
        static AtomicInteger counter = new AtomicInteger(0);
        private final int id;
        A () {
            id = counter.incrementAndGet();
        }
        @Override
        public String toString() {
            return "IAmA: " + id;
        }
    }
}