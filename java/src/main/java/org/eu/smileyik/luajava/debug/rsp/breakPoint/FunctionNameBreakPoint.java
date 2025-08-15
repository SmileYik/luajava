package org.eu.smileyik.luajava.debug.rsp.breakPoint;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;

import java.util.Objects;

public class FunctionNameBreakPoint extends AbstractBreakPoint {
    private final String name;

    public FunctionNameBreakPoint(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean isInBreakPoint(LuaStateFacade facade, LuaDebug ar) {
        return Objects.equals(ar.getName(), name);
    }

    @Override
    public String toString() {
        return "FunctionNameBreakPoint{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        FunctionNameBreakPoint that = (FunctionNameBreakPoint) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
