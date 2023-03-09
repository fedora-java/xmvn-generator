package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

class TestHookFactory1 implements HookFactory {
    static Hook hook;

    @Override
    public Hook createHook(BuildContext context) {
        return hook;
    }
}

class TestHookFactory2 implements HookFactory {
    static volatile boolean hookRan;

    @Override
    public Hook createHook(BuildContext context) {
        return () -> {
            hookRan = true;
        };
    }
}

public class CompoundHookTest {
    @Test
    public void testCompoundHook() {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_post_install_hooks}")).andReturn(
                "\n " + TestHookFactory1.class.getName() + " \n\t   " + TestHookFactory2.class.getName() + " ");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        Hook hook1 = EasyMock.createStrictMock(Hook.class);
        TestHookFactory1.hook = hook1;
        hook1.run();
        EasyMock.expectLastCall();
        EasyMock.replay(bc, hook1);
        CompoundHook ch = new CompoundHook(bc);
        ch.runHook();
        assertTrue(TestHookFactory2.hookRan);
        EasyMock.verify(bc, hook1);
    }

    @Test
    public void testClassNotFound() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_post_install_hooks}")).andReturn("com.foo.Bar");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.replay(bc);
        try {
            new CompoundHook(bc).runHook();
            fail("ClassNotFoundException expected");
        } catch (Throwable t) {
            assertInstanceOf(RuntimeException.class, t);
            Throwable e = t.getCause();
            assertInstanceOf(ClassNotFoundException.class, e);
            assertEquals("com.foo.Bar", e.getMessage());
        }
        EasyMock.verify(bc);
    }

    @Test
    public void testClassIsNotFactory() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_post_install_hooks}")).andReturn(CompoundHookTest.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.replay(bc);
        try {
            new CompoundHook(bc).runHook();
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("HookFactory"));
        }
        EasyMock.verify(bc);
    }

    @Test
    public void testNoFactories() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_post_install_hooks}")).andReturn("");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{warn:xmvn-generator: no post-install hooks were specified}")).andReturn("");
        EasyMock.replay(bc);
        new CompoundHook(bc).runHook();
        EasyMock.verify(bc);
    }
}
