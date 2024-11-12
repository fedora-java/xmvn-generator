/*-
 * Copyright (c) 2023-2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
        Logger.print(
                BOX_BOTTOM_LEFT + repeat(BOX_BORDER_HORIZONTAL, BOX_WIDTH - 2) + BOX_BOTTOM_RIGHT);
        flush();
    }
}
