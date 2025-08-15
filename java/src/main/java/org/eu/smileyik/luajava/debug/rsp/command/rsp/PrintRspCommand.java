package org.eu.smileyik.luajava.debug.rsp.command.rsp;

import java.util.Map;

public abstract class PrintRspCommand implements RspCommand {
    private static final String[] PREFIXES = new String[] {
            "p",
            "print"
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

    protected String buildVariableMessage(Map<String, String> variables, String type) {
        StringBuilder message = new StringBuilder();
        message.append("Currently has ")
                .append(variables.size())
                .append(" ")
                .append(type)
                .append(" variables.\n");
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            message.append("  ")
                    .append(entry.getKey())
                    .append(" = ")
                    .append(entry.getValue())
                    .append("\n");
        }
        return message.toString();
    }

    protected String buildVariableMessage(Map<String, String> variables, String type, String name) {
        return String.format("  %s variable: %s = %s\n",
                type, name, variables.get(name));
    }
}
