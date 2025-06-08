package org.eu.smileyik.luajava.util;

import org.junit.jupiter.api.Test;

class ResourceCleanerTest {

    @Test
    public void test() throws InterruptedException {
        ResourceCleaner cleaner = new ResourceCleaner();

        for (int i = 0; i < 3; i++) {
            int id = i;
            Object resource = new Object();

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
}