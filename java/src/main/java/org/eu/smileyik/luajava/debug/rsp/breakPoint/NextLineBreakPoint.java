package org.eu.smileyik.luajava.debug.rsp.breakPoint;

public class NextLineBreakPoint extends LineNumberBreakPoint {

    public NextLineBreakPoint(int lineNumber) {
        super(lineNumber);
        this.setRepeatTimes(1);
    }

    @Override
    public String toString() {
        return "arrived next line: " + getLineNumber();
    }
}
