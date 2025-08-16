package org.eu.smileyik.luajava.debug.util;

public class AnsiMessageBuilder {
    public static final String ANSI_START = "\u001b";
    public static final String ANSI_BOLD =  "\u001B[1m";
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_PURPLE = "\u001B[35m";
    public static final String ANSI_CYAN = "\u001B[36m";
    public static final String ANSI_WHITE = "\u001B[37m";
    public static final String ANSI_BLACK_BACKGROUND = "\u001B[40m";
    public static final String ANSI_RED_BACKGROUND = "\u001B[41m";
    public static final String ANSI_GREEN_BACKGROUND = "\u001B[42m";
    public static final String ANSI_YELLOW_BACKGROUND = "\u001B[43m";
    public static final String ANSI_BLUE_BACKGROUND = "\u001B[44m";
    public static final String ANSI_PURPLE_BACKGROUND = "\u001B[45m";
    public static final String ANSI_CYAN_BACKGROUND = "\u001B[46m";
    public static final String ANSI_WHITE_BACKGROUND = "\u001B[47m";

    private final StringBuilder message = new StringBuilder();

    public static AnsiMessageBuilder builder() {
        return new AnsiMessageBuilder();
    }

    public StringBuilder getStringBuilder() {
        return message;
    }

    public AnsiMessageBuilder append(char c) {
        message.append(c);
        return this;
    }

    public AnsiMessageBuilder append(String message) {
        this.message.append(message);
        return this;
    }

    public AnsiMessageBuilder append(Object message) {
        this.message.append(message);
        return this;
    }

    public AnsiMessageBuilder append(AnsiMessageBuilder messageBuilder) {
        this.message.append(messageBuilder.message);
        return resetColor();
    }

    public AnsiMessageBuilder repeat(char c, int times) {
        return repeat(c + "", times);
    }

    public AnsiMessageBuilder repeat(String str, int times) {
        if (times <= 0) return this;
        StringBuilder append = new StringBuilder(str);
        StringBuilder result = new StringBuilder();
        while (times > 0) {
            if ((times & 1) == 1) result.append(append);
            append.append(append);
            times >>= 1;
        }
        return append(result.toString());
    }

    public String toMessage() {
        return message.toString();
    }

    public AnsiMessageBuilder resetColor() {
        return append(ANSI_RESET);
    }

    public AnsiMessageBuilder resetColor(String message) {
        return resetColor().append(message);
    }

    public AnsiMessageBuilder bold() {
        return append(ANSI_BOLD);
    }

    public AnsiMessageBuilder newLine() {
        return append("\n");
    }

    public AnsiMessageBuilder red() {
        return append(ANSI_RED);
    }

    public AnsiMessageBuilder red(String message) {
        return red().append(message);
    }

    public AnsiMessageBuilder green() {
        return append(ANSI_GREEN);
    }

    public AnsiMessageBuilder green(String message) {
        return green().append(message);
    }

    public AnsiMessageBuilder yellow() {
        return append(ANSI_YELLOW);
    }

    public AnsiMessageBuilder yellow(String message) {
        return yellow().append(message);
    }

    public AnsiMessageBuilder blue() {
        return append(ANSI_BLUE);
    }

    public AnsiMessageBuilder blue(String message) {
        return blue().append(message);
    }

    public AnsiMessageBuilder purple() {
        return append(ANSI_PURPLE);
    }

    public AnsiMessageBuilder purple(String message) {
        return purple().append(message);
    }

    public AnsiMessageBuilder cyan() {
        return append(ANSI_CYAN);
    }

    public AnsiMessageBuilder cyan(String message) {
        return cyan().append(message);
    }

    public AnsiMessageBuilder white() {
        return append(ANSI_WHITE);
    }

    public AnsiMessageBuilder white(String message) {
        return white().append(message);
    }

    public AnsiMessageBuilder blackBackground() {
        return append(ANSI_BLACK_BACKGROUND);
    }

    public AnsiMessageBuilder blackBackground(String message) {
        return blackBackground().append(message);
    }

    public AnsiMessageBuilder redBackground() {
        return append(ANSI_RED_BACKGROUND);
    }

    public AnsiMessageBuilder redBackground(String message) {
        return redBackground().append(message);
    }

    public AnsiMessageBuilder greenBackground() {
        return append(ANSI_GREEN_BACKGROUND);
    }

    public AnsiMessageBuilder greenBackground(String message) {
        return greenBackground().append(message);
    }

    public AnsiMessageBuilder yellowBackground() {
        return append(ANSI_YELLOW_BACKGROUND);
    }

    public AnsiMessageBuilder yellowBackground(String message) {
        return yellowBackground().append(message);
    }

    public AnsiMessageBuilder blueBackground() {
        return append(ANSI_BLUE_BACKGROUND);
    }

    public AnsiMessageBuilder blueBackground(String message) {
        return blueBackground().append(message);
    }

    public AnsiMessageBuilder purpleBackground() {
        return append(ANSI_PURPLE_BACKGROUND);
    }

    public AnsiMessageBuilder purpleBackground(String message) {
        return purpleBackground().append(message);
    }

    public AnsiMessageBuilder cyanBackground() {
        return append(ANSI_CYAN_BACKGROUND);
    }

    public AnsiMessageBuilder cyanBackground(String message) {
        return cyanBackground().append(message);
    }

    public AnsiMessageBuilder whiteBackground() {
        return append(ANSI_WHITE_BACKGROUND);
    }

    public AnsiMessageBuilder whiteBackground(String message) {
        return whiteBackground().append(message);
    }
}
