package org.fedoraproject.xmvn.generator.filesystem;

import java.nio.file.Path;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;

public class FilesystemGeneratorTest {
    private Collector collector;
    private BuildContext context;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
        context = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(context.eval("%{buildroot}")).andReturn("/some/buildroot").anyTimes();
    }

    private void expectRequires(Path filePath, String req) {
        collector.addRequires(filePath, req);
        EasyMock.expectLastCall();
    }

    private void performTest(Path jarPath) {
        EasyMock.replay(collector, context);
        new FilesystemGenerator(context).generate(List.of(jarPath), collector);
        EasyMock.verify(collector, context);
    }

    @Test
    public void testJar() {
        Path jarPath = Path.of("/some/buildroot/usr/share/java/foo.jar");
        expectRequires(jarPath, "javapackages-filesystem");
        performTest(jarPath);
    }

    @Test
    public void testJavadoc() {
        Path jarPath = Path.of("/some/buildroot/usr/share/javadoc/foo/index.html");
        expectRequires(jarPath, "javapackages-filesystem");
        performTest(jarPath);
    }

    @Test
    public void testNonJava() {
        Path jarPath = Path.of("/some/buildroot/usr/bin/foo");
        performTest(jarPath);
    }

    @Test
    public void testDirectoryItself() {
        Path jarPath = Path.of("/some/buildroot/usr/share/java");
        performTest(jarPath);
    }
}
