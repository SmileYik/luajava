package org.eu.smileyik.luajava.debug.rsp.command.rsp;

public abstract class BreakPointRspCommand implements RspCommand {
    private static final String[] PREFIXES = new String[] {
            "b",
            "break",
            "breakpoint"
    };

    protected abstract String[] innerCommandKeys();

    @Override
    public String[] commandKeys() {
        String[] keys = innerCommandKeys();
        String[] newKeys = new String[keys.length * 3];
        for (int i = 0; i < keys.length; i++) {
            int j = 0;
            for (String prefix : PREFIXES) {
                newKeys[i + j++] = prefix + "." + keys[i];
            }
        }
        return newKeys;
    }
}
