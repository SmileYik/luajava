package org.eu.smileyik.luajava.debug.rsp.breakPoint;

import java.util.Objects;

public abstract class AbstractBreakPoint implements BreakPoint {
    private boolean enable = true;
    private int repeatTimes = -1;

    @Override
    public boolean enable() {
        return enable;
    }

    @Override
    public void enable(boolean enable) {
        this.enable = enable;
    }

    @Override
    public int repeatTimes() {
        return repeatTimes;
    }

    public void setRepeatTimes(int repeatTimes) {
        this.repeatTimes = repeatTimes;
    }

    public boolean countDownRepeatTimes() {
        if (repeatTimes == -1) {
            return false;
        }
        return --repeatTimes <= 0;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AbstractBreakPoint that = (AbstractBreakPoint) o;
        return enable == that.enable && repeatTimes == that.repeatTimes;
    }

    @Override
    public int hashCode() {
        return Objects.hash(enable, repeatTimes);
    }
}
