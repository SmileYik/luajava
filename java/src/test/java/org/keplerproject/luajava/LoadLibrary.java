package org.keplerproject.luajava;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

public class LoadLibrary {
    public static void load() {
        load((String) null);
    }

    public static void load(String folder) {
        File sharedDir = Paths.get("..", "build", "outputs", "shared").toFile();
        if (!sharedDir.exists()) {
            sharedDir = Paths.get("build", "outputs", "shared").toFile();
        }
        if (folder != null) {
            sharedDir = new File(sharedDir, folder);
        } else {
            File[] files = sharedDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        sharedDir = file;
                        break;
                    }
                }
            }
        }
        load(sharedDir);
    }

    private static void load(File sharedDir) {
        if (!sharedDir.exists()) {
            throw new RuntimeException("Could not find shared directory: " + sharedDir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(sharedDir.listFiles())) {
            if (file.isFile()) {
                System.load(file.getAbsolutePath());
            }
        }
    }
}
