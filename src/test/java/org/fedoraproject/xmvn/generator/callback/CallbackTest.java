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
package org.fedoraproject.xmvn.generator.callback;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class CallbackTest {
    @Test
    public void testCallback() throws Exception {
        Semaphore sema = new Semaphore(0);
        Runnable x = sema::release;
        Callback cb = Callback.setUp(x);
        Process p = new ProcessBuilder(cb.getCommand()).inheritIO().start();
        boolean acquired = sema.tryAcquire(5, TimeUnit.SECONDS);
        assertTrue(acquired);
        boolean joined = p.waitFor(5, TimeUnit.SECONDS);
        assertTrue(joined);
    }
}
