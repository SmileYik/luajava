package org.eu.smileyik.luajava.debug.rsp.breakPoint;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;

public interface BreakPoint {
    public boolean enable();
    public void enable(boolean enable);
    public boolean isInBreakPoint(LuaStateFacade facade, LuaDebug ar);

    int repeatTimes();
    boolean countDownRepeatTimes();
}
