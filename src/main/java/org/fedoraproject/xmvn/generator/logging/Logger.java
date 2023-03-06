package org.fedoraproject.xmvn.generator.logging;

public class Logger {
    private static final int BOX_WIDTH = 80;
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

    private static String rep(char c, int n) {
        return new String(new char[n]).replace('\0', c);
    }

    private static void log(String msg) {
        if (debugEnabled) {
            System.err.println(msg);
        }
    }

    public static void debug(String msg) {
        int n = msg.length();
        if (n <= BOX_WIDTH - 4) {
            msg += rep(' ', BOX_WIDTH - 4 - n) + " " + BOX_BORDER_VERTICAL;
        }
        log(BOX_BORDER_VERTICAL + " " + msg);
    }

    public static void beg() {
        Logger.log(BOX_TOP_LEFT + rep(BOX_BORDER_HORIZONTAL, BOX_WIDTH - 2) + BOX_TOP_RIGHT);
        debug("XMvn Generator");
    }

    public static void cut() {
        Logger.log(BOX_DIVIDER_LEFT + rep(BOX_DIVIDER, BOX_WIDTH - 2) + BOX_DIVIDER_RIGHT);
    }

    public static void end() {
        Logger.log(BOX_BOTTOM_LEFT + rep(BOX_BORDER_HORIZONTAL, BOX_WIDTH - 2) + BOX_BOTTOM_RIGHT);
    }
}
