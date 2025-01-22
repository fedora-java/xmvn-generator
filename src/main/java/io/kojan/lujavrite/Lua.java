/*-
 * Copyright (c) 2025 Red Hat, Inc.
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
package io.kojan.lujavrite;

/**
 * Java API for calling Lua through <a href="https://github.com/mizdebsk/lujavrite">LuJavRite</a>.
 *
 * <p>For documentation of individual functions, see the <a
 * href="https://www.lua.org/manual/5.4/manual.html#lua_getglobal">Lua Reference Manual</a>.
 *
 * <p>This trivial Java class is maintained as part of LuJavRite project, but it is expected to be
 * source-bundled by projects depending on the Java->Lua calling functionality of LuJavRite. The
 * canonical copy of this class source can be found at <a
 * href="https://github.com/mizdebsk/lujavrite/blob/master/lujavrite.c">
 * https://github.com/mizdebsk/lujavrite/blob/master/lujavrite.c</a>.
 *
 * @author Mikolaj Izdebski
 */
public class Lua {
    public static native int getglobal(String name);

    public static native int getfield(int index, String name);

    public static native void pushstring(String string);

    public static native int pcall(int nargs, int nresults, int msgh);

    public static native String tostring(int index);

    public static native int pop(int n);

    public static native int remove(int index);
}
