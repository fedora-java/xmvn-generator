package org.fedoraproject.xmvn.generator.logging;

public class Logger {
    private static final int BOX_WIDTH = 120;
    private static final boolean UNICODE = true;
    private static final char BOX_TOP_LEFT = UNICODE ? '╔' : '#';
    private static final char BOX_TOP_RIGHT = UNICODE ? '╗' : '#';
    private static final char BOX_BOTTOM_LEFT = UNICODE ? '╚' : '#';
    private static final char BOX_BOTTOM_RIGHT = UNICODE ? '╝' : '#';
    private static final char BOX_DIVIDER_LEFT = UNICODE ? '╟' : '#';
    private static final char BOX_DIVIDER_RIGHT = UNICODE ? '╢' : '#';
    private static final char BOX_DIVIDER = UNICODE ? '─' : '-';
    private static final char BOX_BORDER_HORIZONTAL = UNICODE ? '═' : '#';
    private static final char BOX_BORDER_VERTICAL = UNICODE ? '║' : '#';
    private static boolean debugEnabled;

    public static void enableDebug() {
        debugEnabled = true;
    }

    private static String repeat(char c, int n) {
        return new String(new char[n]).replace('\0', c);
    }

    private static void print(String msg) {
        if (debugEnabled) {
            System.err.println(msg);
        }
    }

    private static void flush() {
        if (debugEnabled) {
            System.err.flush();
        }
    }

    public static void debug(String msg) {
        int n = msg.length();
        if (n <= BOX_WIDTH - 4) {
            msg += repeat(' ', BOX_WIDTH - 4 - n) + " " + BOX_BORDER_VERTICAL;
        }
        print(BOX_BORDER_VERTICAL + " " + msg);
    }

    public static void startLogging() {
        Logger.print(BOX_TOP_LEFT + repeat(BOX_BORDER_HORIZONTAL, BOX_WIDTH - 2) + BOX_TOP_RIGHT);
        debug("XMvn Generator");
    }

    public static void startNewSection() {
        Logger.print(BOX_DIVIDER_LEFT + repeat(BOX_DIVIDER, BOX_WIDTH - 2) + BOX_DIVIDER_RIGHT);
    }

    public static void finishLogging() {
        Logger.print(BOX_BOTTOM_LEFT + repeat(BOX_BORDER_HORIZONTAL, BOX_WIDTH - 2) + BOX_BOTTOM_RIGHT);
        flush();
    }
}
