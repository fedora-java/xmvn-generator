package org.fedoraproject.xmvn.generator.jpms;

import java.nio.file.Path;
import java.util.List;

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

    private void expectProvides(Path filePath, String prov) {
        collector.addProvides(filePath, prov);
        EasyMock.expectLastCall();
    }

    private void performTest(Path jarPath) {
        EasyMock.replay(collector);
        new JPMSGenerator().generate(List.of(jarPath), collector);
        EasyMock.verify(collector);
    }

    @Test
    public void testSimpleJar() {
        Path jarPath = Path.of("src/test/resources/simple.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(jarPath);
    }

    @Test
    public void testMultiReleaseJar() {
        Path jarPath = Path.of("src/test/resources/mr.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(jarPath);
    }
}
