package org.fedoraproject.xmvn.generator.jpms;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.Collector;

public class JPMSGeneratorTest {
    private Collector collector;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
    }

    private void expectProvides(String prov) {
        collector.addProvides(prov);
        EasyMock.expectLastCall();
    }

    private void performTest(String jarName) {
        Path jarPath = Paths.get("src/test/resources").resolve(jarName);
        EasyMock.replay(collector);
        new JPMSGenerator().generate(jarPath, collector);
        EasyMock.verify(collector);
    }

    @Test
    public void testSimpleJar() {
        expectProvides("jpms(foo)");
        performTest("simple.jar");
    }

    @Test
    public void testMultiReleaseJar() {
        expectProvides("jpms(foo)");
        performTest("mr.jar");
    }
}
