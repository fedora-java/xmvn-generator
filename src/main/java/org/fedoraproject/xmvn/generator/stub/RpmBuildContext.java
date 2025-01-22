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
package org.fedoraproject.xmvn.generator.stub;

import io.kojan.lujavrite.Lua;
import org.fedoraproject.xmvn.generator.BuildContext;

class RpmBuildContext implements BuildContext {
    static {
        // FIXME don't hardcode the path
        System.load("/usr/lib64/lua/5.4/lujavrite.so");
    }

    @Override
    public String eval(String macro) {
        Lua.getglobal("rpm"); //       Stack: rpm(-1)
        Lua.getfield(-1, "expand"); // Stack: rpm(-2), expand(-1)
        Lua.pushstring(macro); //      Stack: rpm(-3), expand(-2), macro(-1)
        Lua.pcall(1, 1, 0); //         Stack: rpm(-2), val(-1)
        String val = Lua.tostring(-1);
        Lua.pop(2); //                 Stack: (empty)
        return val;
    }
}
