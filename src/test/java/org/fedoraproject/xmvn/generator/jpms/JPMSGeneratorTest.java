package org.fedoraproject.xmvn.generator.jpms;

import java.nio.file.Files;
import java.nio.file.Path;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;

public class JPMSGeneratorTest {
    private Collector collector;
    private BuildContext context;
    @TempDir
    private Path br;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
        context = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(context.eval("%{buildroot}")).andReturn(br.toString()).anyTimes();
    }

    private void expectProvides(Path filePath, String prov) {
        collector.addProvides(filePath, prov);
        EasyMock.expectLastCall();
    }

    private void performTest(Path srcPath, Path jarPath) throws Exception {
        EasyMock.replay(collector, context);
        Files.createDirectories(jarPath.getParent());
        Files.copy(srcPath, jarPath);
        new JPMSGenerator(context).generate(collector);
        EasyMock.verify(collector, context);
    }

    @Test
    public void testSimpleJar() throws Exception {
        Path srcPath = Path.of("src/test/resources/simple.jar");
        Path jarPath = br.resolve("usr/share/java/simple.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(srcPath, jarPath);
    }

    @Test
    public void testMultiReleaseJar() throws Exception {
        Path srcPath = Path.of("src/test/resources/mr.jar");
        Path jarPath = br.resolve("usr/lib/java/mr.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(srcPath, jarPath);
    }
}
