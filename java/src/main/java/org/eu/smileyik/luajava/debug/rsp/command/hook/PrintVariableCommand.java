package org.eu.smileyik.luajava.debug.rsp.command.hook;

import org.eu.smileyik.luajava.LuaStateFacade;
import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.RspDebugServer;
import org.eu.smileyik.luajava.debug.util.AnsiMessageBuilder;
import org.eu.smileyik.luajava.debug.util.DebugUtils;

import java.util.Map;

public class PrintVariableCommand implements Command {

    public static final String SCOPE_GLOBAL = "global";
    public static final String SCOPE_LOCAL = "local";

    private final String scope;
    private final String variableName;

    public PrintVariableCommand(String scope) {
        this(scope, null);
    }

    public PrintVariableCommand(String scope, String variableName) {
        this.scope = scope;
        this.variableName = variableName;
    }

    @Override
    public boolean handle(RspDebugServer debugServer, LuaStateFacade facade, LuaDebug ar) {
        try {
            if (ar == null) {
                debugServer.fillMessageQueue(
                        AnsiMessageBuilder.builder().red("Lua Debug info is not found!").toMessage());
            } else {
                Map<String, Object> variables = null;
                switch (scope) {
                    case SCOPE_GLOBAL:
                        variables = DebugUtils.getGlobalVariable(facade, ar);
                        break;
                    case SCOPE_LOCAL:
                        variables = DebugUtils.getLocalVariable(facade, ar);
                        break;
                    default:
                        break;
                }
                if (variables == null) {
                    debugServer.fillMessageQueue(
                            AnsiMessageBuilder.builder().red("Not a valid variable scope!").toMessage());
                } else if (variableName == null) {
                    debugServer.fillMessageQueue(buildVariableMessage(variables, scope));
                } else {
                    debugServer.fillMessageQueue(buildVariableMessage(variables, scope, variableName));
                }
            }
        } finally {
            debugServer.finishedFillMessage();
        }
        return false;
    }

    protected String buildVariableMessage(Map<String, Object> variables, String type) {
        AnsiMessageBuilder message = new AnsiMessageBuilder();
        message.cyan("Currently has ")
                .green().bold().append(variables.size()).resetColor()
                .cyan(" ")
                .purple(type)
                .cyan(" variables.")
                .newLine()
                .resetColor();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            message.append("  ")
                    .purple(entry.getKey())
                    .resetColor(" = ")
                    .green(colorVariableValue(entry.getValue()))
                    .newLine();
        }
        return message.resetColor().toMessage();
    }

    protected String buildVariableMessage(Map<String, Object> variables, String type, String name) {
        return AnsiMessageBuilder.builder()
                .append("  ")
                .cyan(type)
                .resetColor(" ")
                .purple(name)
                .resetColor(" = ")
                .green(colorVariableValue(variables.get(name)))
                .resetColor()
                .newLine()
                .toMessage();
    }

    private static final String[] DEEP_COLOR = new String[] {
            AnsiMessageBuilder.ANSI_RED,
            AnsiMessageBuilder.ANSI_GREEN,
            AnsiMessageBuilder.ANSI_YELLOW,
            AnsiMessageBuilder.ANSI_BLUE,
            AnsiMessageBuilder.ANSI_PURPLE,
            AnsiMessageBuilder.ANSI_CYAN,
            AnsiMessageBuilder.ANSI_WHITE,
    };

    private static String getDeepColor(int idx) {
        return DEEP_COLOR[idx % DEEP_COLOR.length];
    }

    private String colorVariableValue(Object value) {
        String s = DebugUtils.variableToString(value).replace("\n", "\\n");
        if (!s.startsWith("{") || !s.endsWith("}")) {
            return s;
        }

        AnsiMessageBuilder message = new AnsiMessageBuilder();
        int len = s.length();
        int deep = 0;
        boolean inString = false;
        boolean equalsRight = false;
        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            String afterAppend = null;
            if (c == '{') {
                // table start
                message.append(getDeepColor(deep));
                deep += 1;
                afterAppend = AnsiMessageBuilder.ANSI_RESET;
                if (deep > 1) {
                    // if deep > 1 then '{' must after '=', so, can not detect key start,
                    // need add same color as key start by manual
                    afterAppend += new AnsiMessageBuilder()
                            .append("\n  ")
                            .repeat(' ', deep << 1)
                            .purple()
                            .toMessage();
                }
            } else if (c == '}') {
                if (!inString) {
                    // close table
                    deep -= 1;
                    if (i > 0 && s.charAt(i - 1) == '{') {
                        // empty table
                        StringBuilder stringBuilder = message.getStringBuilder();
                        int length = stringBuilder.length() - 1;
                        while (stringBuilder.charAt(length) != '{') {
                            stringBuilder.deleteCharAt(length--);
                        }
                    } else {
                        message.append("\n  ")
                                .repeat(' ', deep << 1)
                                .append(getDeepColor(deep));
                    }
                }
            } else if (c == '"') {
                // check string.
                if (inString) {
                    int j = i - 1;
                    while (j >= 0 && s.charAt(j) == '\\') {
                        j -= 1;
                    }
                    if (((j - i) & 1 ) == 1) {
                        inString = false;
                    }
                } else {
                    inString = true;
                }
            } else if (c == '=') {
                if (!inString) {
                    // value start
                    equalsRight = true;
                    message.resetColor();
                    afterAppend = AnsiMessageBuilder.ANSI_GREEN;
                }
            } else if (c == ',') {
                if (!inString) {
                    // end of key=value, and need start new one.
                    equalsRight = false;
                    message.resetColor();
                }
            } else if (c == '[') {
                if (!inString && !equalsRight) {
                    // key start
                    message.append("\n  ")
                            .repeat(' ', deep << 1)
                            .purple();
                }
            }

            message.append(c);
            if (afterAppend != null) {
                message.append(afterAppend);
            }
        }
        return message.resetColor().toMessage();
    }
}
