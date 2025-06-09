package org.eu.smileyik.luajava.util;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class ResourceCleanerTest {

    @Test
    public void test() throws InterruptedException {
        ResourceCleaner cleaner = new ResourceCleaner();

        for (int i = 0; i < 3; i++) {
            int id = i;
            Object resource = new Object();
            new B(cleaner);

            cleaner.register(resource, () -> {
                System.out.println("资源 " + id + " 已被释放并清理.");
            });

            resource = null;
        }

        System.out.println("请求GC...");
        System.gc();
        Thread.sleep(2000);

        cleaner.close();
    }

    public static class SomeCleaner implements Runnable {


        @Override
        public void run() {

        }
    }

    public static class A {
        private byte[] bytes = new byte[1024];

        protected List<SomeCleaner> cleaners = new ArrayList<>();

        public A (ResourceCleaner cleaner) {
            cleaner.register(this, new ACleaner(cleaners));
        }

        private static class ACleaner extends SomeCleaner {
            List<SomeCleaner> cleaners = new ArrayList<>();

            public ACleaner(List<SomeCleaner> cleaners) {
                this.cleaners = cleaners;
            }

            @Override
            public void run() {
                for (SomeCleaner cleaner : cleaners) {
                    cleaner.run();
                }
                cleaners.clear();
                System.out.println("A clear");
            }
        }
    }

    public static class B extends A {
        private int[] nums = new int[1024];
        public B(ResourceCleaner cleaner) {
            super(cleaner);
            cleaners.add(new BCleaner(nums));
        }

        private static class BCleaner extends SomeCleaner {
            private final WeakReference<int[]> numsRef;

            public BCleaner(int[] nums) {
                numsRef = new WeakReference<>(nums);
            }

            @Override
            public void run() {
                int[] ints = numsRef.get();
                if (ints != null) {
                    Arrays.fill(ints, 0);
                    ints = null;
                }
                numsRef.clear();
                System.out.println("B clear");
            }
        }
    }
}
