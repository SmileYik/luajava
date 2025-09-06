package org.eu.smileyik.luajava;

import java.nio.ByteBuffer;

public class Utf8Reader {
    public static String readUTF8OrMUTF8(ByteBuffer buf) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (buf.hasRemaining()) {
            int b1 = buf.get() & 0xff;

            if (b1 < 0x80) {
                sb.append((char) b1);
            } else if ((b1 & 0xe0) == 0xc0) {
                int b2 = buf.get() & 0xff;
                sb.append((char) (((b1 & 0x1f) << 6) | (b2 & 0x3f)));
            } else if ((b1 & 0xf0) == 0xe0) {
                int b2 = buf.get() & 0xff;
                int b3 = buf.get() & 0xff;
                int codePoint = (((b1 & 0x0f) << 12) | ((b2 & 0x3f) << 6) | (b3 & 0x3f));
                sb.append((char) codePoint);
            } else if ((b1 & 0xf8) == 0xf0) {
                int b2 = buf.get() & 0xff;
                int b3 = buf.get() & 0xff;
                int b4 = buf.get() & 0xff;
                int codePoint = ((b1 & 0x07) << 18) | ((b2 & 0x3f) << 12) | ((b3 & 0x3f) << 6) | (b4 & 0x3f);
                sb.append(Character.toChars(codePoint));
            } else {
                throw new IllegalArgumentException("Invalid MUTF-8 or UTF-8 byte sequence at index " + (i - 1));
            }
        }
        return sb.toString();
    }
}
