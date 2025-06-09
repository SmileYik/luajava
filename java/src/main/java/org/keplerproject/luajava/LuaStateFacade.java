package org.keplerproject.luajava;

import java.io.Closeable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Function;

public class LuaStateFacade implements AutoCloseable {
    private final Lock lock = new ReentrantLock();
    private final int stateId;
    private final LuaState luaState;


    protected LuaStateFacade(int stateId) {
        this.stateId = stateId;
        this.luaState = new LuaState(stateId);
    }

    protected LuaStateFacade(CPtr cPtr) {
        this.stateId = LuaStateFactory.insertLuaState(this, cPtr);
        this.luaState = new LuaState(cPtr, this.stateId);
    }

    public long getCPtrPeer() {
        return luaState.getCPtrPeer();
    }

    public <T> T lock(Function<LuaState, T> function) {
        lock.lock();
        try {
            return function.apply(luaState);
        } finally {
            lock.unlock();
        }
    }

    public void lock(Consumer<LuaState> consumer) {
        lock.lock();
        try {
            consumer.accept(luaState);
        } finally {
            lock.unlock();
        }
    }

    // STACK MANIPULATION

    public LuaStateFacade newThread() {
        lock.lock();
        try {
            return new LuaStateFacade(luaState.newThread());
        } finally {
            lock.unlock();
        }
    }

    // PUSH FUNCTIONS

    public LuaStateFacade toThread(int idx) {
        lock.lock();
        try {
            return new LuaStateFacade(luaState.toThread(idx));
        } finally {
            lock.unlock();
        }
    }

    // ACCESS FUNCTION

    public void xmove(LuaStateFacade to, int n) {
        lock.lock();
        try {
            luaState.xmove(to.luaState, n);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void close() throws Exception {
        LuaStateFactory.removeLuaState(stateId);
        luaState.close();
    }
}
