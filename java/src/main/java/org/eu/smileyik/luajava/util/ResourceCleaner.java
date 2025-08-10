/*
 * ResourceCleaner.java, SmileYik, 2025-8-10
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

package org.eu.smileyik.luajava.util;

import java.io.Closeable;
import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.LinkedList;

public class ResourceCleaner implements Runnable, Closeable {
    private static final ResourceCleaner INSTANCE = new ResourceCleaner();

    public static ResourceCleaner getInstance() {
        return INSTANCE;
    }

    private final ReferenceQueue<Object> referenceQueue = new ReferenceQueue<>();
    private final LinkedList<Ref> refs = new LinkedList<>();

    private final Thread cleanerThread;

    public ResourceCleaner() {
        cleanerThread = new Thread(this);
        cleanerThread.setDaemon(true);
        cleanerThread.setName("ResourceCleaner");
        cleanerThread.start();
    }

    @Override
    public void run() {
        monitorTask();
    }

    public synchronized PhantomReference<Object> register(Object o, Runnable finalizer) {
        Ref ref = new Ref(o, referenceQueue, finalizer);
        refs.add(ref);
        return ref;
    }

    @Override
    public synchronized void close() {
        cleanerThread.interrupt();
    }

    private void monitorTask() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Ref removed = (Ref) referenceQueue.remove();
                removed.clean();
                refs.remove(removed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static final class Ref extends PhantomReference<Object> {

        private final Runnable finalizer;

        /**
         * Creates a new phantom reference that refers to the given object and
         * is registered with the given queue.
         *
         * <p> It is possible to create a phantom reference with a <tt>null</tt>
         * queue, but such a reference is completely useless: Its <tt>get</tt>
         * method will always return {@code null} and, since it does not have a queue,
         * it will never be enqueued.
         *
         * @param referent the object the new phantom reference will refer to
         * @param q        the queue with which the reference is to be registered,
         *                 or <tt>null</tt> if registration is not required
         */
        public Ref(Object referent, ReferenceQueue<? super Object> q, Runnable finalizer) {
            super(referent, q);
            this.finalizer = finalizer;
        }

        public void clean() {
            finalizer.run();
        }
    }
}
