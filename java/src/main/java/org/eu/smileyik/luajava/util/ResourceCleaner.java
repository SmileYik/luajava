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
    public synchronized void run() {
        monitorTask();
    }

    public synchronized Ref register(Object o, Runnable finalizer) {
        Ref ref = new Ref(o, referenceQueue, finalizer);
        refs.add(ref);
        return ref;
    }

    @Override
    public synchronized void close() {
        cleanerThread.interrupt();
    }

    private synchronized void monitorTask() {
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

    public static final class Ref extends PhantomReference<Object> {

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
