package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicBoolean;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

public class CompoundHookTest {
    @Test
    public void testCompoundHook() {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        Hook hook1 = EasyMock.createStrictMock(Hook.class);
        HookFactory fac1 = EasyMock.createStrictMock(HookFactory.class);
        EasyMock.expect(fac1.createHook(bc)).andReturn(hook1);
        hook1.run();
        EasyMock.expectLastCall();
        AtomicBoolean hookRan = new AtomicBoolean(false);
        Hook hook2 = () -> {
            hookRan.set(true);
        };
        HookFactory fac2 = x -> hook2;
        EasyMock.replay(bc, hook1, fac1);
        CompoundHook ch = new CompoundHook(bc, fac1, fac2);
        ch.runHook();
        assertTrue(hookRan.get());
        EasyMock.verify(bc, hook1, fac1);
    }
}
