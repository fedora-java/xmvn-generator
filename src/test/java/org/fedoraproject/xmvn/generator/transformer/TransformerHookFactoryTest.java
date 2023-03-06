package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

public class TransformerHookFactoryTest {
    @Test
    public void testFactory() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/tmp/build/root");
        EasyMock.expect(bc.eval("%{_javadir}")).andReturn("/tmp/javadir");
        EasyMock.expect(bc.eval("%{_jnidir}")).andReturn("/tmp/jnidir");
        EasyMock.replay(bc);
        HookFactory factory = new TransformerHookFactory();
        Hook hook = factory.createHook(bc);
        assertInstanceOf(TransformerHook.class, hook);
        EasyMock.verify(bc);
    }
}
