package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.jar.Manifest;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

public class ManifestInjectorTest {
    @Test
    public void testManifestInjector() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{NAME}")).andReturn("nn");
        EasyMock.expect(bc.eval("%{?EPOCH}")).andReturn("ee");
        EasyMock.expect(bc.eval("%{VERSION}")).andReturn("vv");
        EasyMock.expect(bc.eval("%{RELEASE}")).andReturn("rr");
        EasyMock.replay(bc);
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Foo", "xx");
        ManifestInjector manifestInjector = new ManifestInjector(bc);
        manifestInjector.transform(mf);
        assertEquals("xx", mf.getMainAttributes().getValue("Foo"));
        assertEquals("nn", mf.getMainAttributes().getValue("Rpm-Name"));
        assertEquals("ee", mf.getMainAttributes().getValue("Rpm-Epoch"));
        assertEquals("vv", mf.getMainAttributes().getValue("Rpm-Version"));
        assertEquals("rr", mf.getMainAttributes().getValue("Rpm-Release"));
        EasyMock.verify(bc);
    }
}
