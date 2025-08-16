package org.eu.smileyik.luajava.debug.rsp;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.rsp.breakPoint.BreakPoint;

public interface DebugServer extends AutoCloseable {

    /**
     * close server
     */
    public void close();

    /**
     * stop eval lua script until connect server
     *
     * @return
     * @throws InterruptedException
     */
    DebugServer waitConnection() throws InterruptedException;

    /**
     * check is running step mode or not.
     */
    boolean step();

    /**
     * set step mode.
     * @param flag if flag is true then lua hook method will be invoked every line.
     */
    void step(boolean flag);

    /**
     * add break point
     */
    void addBreakPoint(BreakPoint breakPoint);

    /**
     * remove break point
     */
    void removeBreakPoint(BreakPoint breakPoint);

    LuaStateFacade getLuaStateFacade();
}
