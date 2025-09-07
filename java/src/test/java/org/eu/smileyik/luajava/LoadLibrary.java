/*
 * LoadLibrary.java, SmileYik, 2025-8-10
 * Copyright (c) 2025 Smile Yik
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.eu.smileyik.luajava;

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
                if (file.getName().endsWith(".dll") || file.getName().endsWith(".so") || file.getName().endsWith(".dylib")) {
                    System.load(file.getAbsolutePath());
                }
            }
        }
    }
}
