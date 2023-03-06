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
        EasyMock.expect(bc.eval("%{name}")).andReturn("bn");
        EasyMock.expect(bc.eval("%{?epoch}")).andReturn("be");
        EasyMock.expect(bc.eval("%{version}")).andReturn("bv");
        EasyMock.expect(bc.eval("%{release}")).andReturn("br");
        EasyMock.expect(bc.eval("%{license}")).andReturn("bl");
        EasyMock.expect(bc.eval("%{NAME}")).andReturn("sn");
        EasyMock.expect(bc.eval("%{?EPOCH}")).andReturn("se");
        EasyMock.expect(bc.eval("%{VERSION}")).andReturn("sv");
        EasyMock.expect(bc.eval("%{RELEASE}")).andReturn("sr");
        EasyMock.replay(bc);
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Foo", "xx");
        ManifestInjector manifestInjector = new ManifestInjector(bc);
        manifestInjector.transform(mf);
        assertEquals("xx", mf.getMainAttributes().getValue("Foo"));
        assertEquals("bn", mf.getMainAttributes().getValue("Rpm-Name"));
        assertEquals("be", mf.getMainAttributes().getValue("Rpm-Epoch"));
        assertEquals("bv", mf.getMainAttributes().getValue("Rpm-Version"));
        assertEquals("br", mf.getMainAttributes().getValue("Rpm-Release"));
        assertEquals("bl", mf.getMainAttributes().getValue("Rpm-License"));
        assertEquals("sn", mf.getMainAttributes().getValue("Rpm-Source-Name"));
        assertEquals("se", mf.getMainAttributes().getValue("Rpm-Source-Epoch"));
        assertEquals("sv", mf.getMainAttributes().getValue("Rpm-Source-Version"));
        assertEquals("sr", mf.getMainAttributes().getValue("Rpm-Source-Release"));
        EasyMock.verify(bc);
    }
}
