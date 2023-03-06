package org.fedoraproject.xmvn.generator.callback;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

public class CallbackTest {
    @Test
    public void testCallback() throws Exception {
        Semaphore sema = new Semaphore(0);
        Runnable x = () -> {
            sema.release();
        };
        Callback cb = Callback.setUp(x);
        Process p = new ProcessBuilder(cb.getCommand()).inheritIO().start();
        boolean acquired = sema.tryAcquire(5, TimeUnit.SECONDS);
        assertTrue(acquired);
        boolean joined = p.waitFor(5, TimeUnit.SECONDS);
        assertTrue(joined);
    }
}
