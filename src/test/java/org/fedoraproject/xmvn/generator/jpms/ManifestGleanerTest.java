package org.fedoraproject.xmvn.generator.jpms;

import java.util.jar.Manifest;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.Collector;

public class ManifestGleanerTest {
    private Collector collector;
    private Manifest manifest;
    private ManifestGleaner gleaner;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createStrictMock(Collector.class);
        manifest = new Manifest();
        gleaner = new ManifestGleaner(collector);
    }

    private void updateManifest(String key, String value) {
        manifest.getMainAttributes().putValue(key, value);
    }

    private void expectProvides(String prov) {
        collector.addProvides(prov);
        EasyMock.expectLastCall();
    }

    private void performTest() {
        EasyMock.replay(collector);
        gleaner.glean(manifest);
        EasyMock.verify(collector);
    }

    @Test
    public void testEmptyManifest() {
        performTest();
    }

    @Test
    public void testNullManifest() {
        manifest = null;
        performTest();
    }

    @Test
    public void testSimple() {
        updateManifest("Automatic-Module-Name", "foo.bar");
        expectProvides("jpms(foo.bar)");
        performTest();
    }
}
