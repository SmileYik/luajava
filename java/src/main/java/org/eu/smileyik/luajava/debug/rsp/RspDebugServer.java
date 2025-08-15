package org.eu.smileyik.luajava.debug.rsp;

import org.eu.smileyik.luajava.debug.LuaDebug;
import org.eu.smileyik.luajava.debug.rsp.command.hook.Command;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface RspDebugServer extends DebugServer {

    /**
     * get current hooked lua debug info. at the first it is null.
     */
    LuaDebug getCurrentDebugInfo();

    /**
     * add lua hook command. this command will run when lua hook method invoked
     */
    void addCommand(Command command);

    /**
     * blocked way to send message to gdb client
     */
    void sendMessage(String message) throws IOException;

    /**
     * blocked way to send message to gdb client
     */
    void sendMessageAsync(String message);

    /**
     * send response to gdb client
     */
    void sendResponse(String payload) throws IOException;


    // ************ Utilities *****************

    public static <K, V> Map<K, V> ofMap(Object... keyValues) {
        int size = keyValues.length;
        Map<K, V> map = new HashMap<>(size >> 1);
        for (int i = 0; i < size; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }

    public static String fastRepeat(String str, int times) {
        if (times <= 0) return "";
        StringBuilder result = new StringBuilder();
        StringBuilder base = new StringBuilder(str);
        while (times > 0) {
            if ((times & 1) == 1) {
                result.append(base);
            }
            base.append(base);
            times >>= 1;
        }
        return result.toString();
    }

    public static String long2Hex(long l) {
        String str = Long.toHexString(l);
        return reverseBytes(fastRepeat("0", 8 - str.length()) + str);
    }

    public static String reverseBytes(String str) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < str.length(); i += 2) {
            result.insert(0, str.charAt(i));
            result.insert(1, str.charAt(i + 1));
        }
        return result.toString();
    }

    Map<String, Object> getLocalVariables();

    Map<String, Object> getGlobalVariables();

    Map<String, String> getStringGlobalVariables();

    Map<String, String> getStringLocalVariables();

    void setContinueType(Command continueType);

    Command getContinueType();
}
