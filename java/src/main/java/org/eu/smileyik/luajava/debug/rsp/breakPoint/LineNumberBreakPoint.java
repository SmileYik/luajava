package org.eu.smileyik.luajava.debug.rsp.breakPoint;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;

import java.util.Objects;

public class LineNumberBreakPoint extends AbstractBreakPoint {
    private final int lineNumber;

    public LineNumberBreakPoint(int lineNumber) {
        this.lineNumber = lineNumber;
    }

    public long getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean isInBreakPoint(LuaStateFacade facade, LuaDebug ar) {
        return ar.getCurrentLine() == lineNumber;
    }

    @Override
    public String toString() {
        return "LineNumberBreakPoint{" +
                "lineNumber=" + lineNumber +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LineNumberBreakPoint that = (LineNumberBreakPoint) o;
        return lineNumber == that.lineNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lineNumber);
    }
}
