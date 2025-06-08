package org.keplerproject.luajava;

import java.io.File;
import java.nio.file.Paths;
import java.util.Objects;

public class LoadLibrary {
    public static void load() {
        File sharedDir = Paths.get("..", "build", "outputs", "shared")
                .toFile();
        try {
            load(sharedDir);
        } catch (Exception e) {
            sharedDir = Paths.get("build", "outputs", "shared")
                    .toFile();
            load(sharedDir);
        }
    }

    private static void load(File sharedDir) {
        if (!sharedDir.exists()) {
            throw new RuntimeException("Could not find shared directory: " + sharedDir.getAbsolutePath());
        }
        for (File file : Objects.requireNonNull(sharedDir.listFiles())) {
            System.load(file.getAbsolutePath());
        }
    }
}
