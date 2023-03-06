package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarInputStream;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.fedoraproject.xmvn.generator.Collector;

public class TransformingPseudoGeneratorTest {
    @TempDir
    private Path buildRoot;

    @Test
    public void testManifestInjection() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(jarPath.getParent());
        Files.copy(Paths.get("src/test/resources/example.jar"), jarPath);
        Collector collector = EasyMock.createStrictMock(Collector.class);
        ManifestTransformer transformer = mf -> mf.getMainAttributes().putValue("Injected", "Test");
        TransformingPseudoGenerator generator = new TransformingPseudoGenerator(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/foo"));
        generator.addDirectoryPrefix(Paths.get("/some/prefix"));
        generator.addDirectoryPrefix(Paths.get("/another/dir"));
        EasyMock.replay(collector);
        generator.generate(jarPath, collector);
        EasyMock.verify(collector);
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(jarPath))) {
            assertEquals("Test", jis.getManifest().getMainAttributes().getValue("Injected"));
        }
    }

    @Test
    public void testInvalidJar() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(jarPath.getParent());
        Files.copy(Paths.get("src/test/resources/invalid.jar"), jarPath);
        Collector collector = EasyMock.createStrictMock(Collector.class);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformingPseudoGenerator generator = new TransformingPseudoGenerator(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/some/prefix"));
        EasyMock.replay(collector, transformer);
        generator.generate(jarPath, collector);
        EasyMock.verify(collector, transformer);
    }

    @Test
    public void testPrefixMismatch() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(jarPath.getParent());
        Files.copy(Paths.get("src/test/resources/example.jar"), jarPath);
        Collector collector = EasyMock.createStrictMock(Collector.class);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformingPseudoGenerator generator = new TransformingPseudoGenerator(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/mismatched/prefix"));
        EasyMock.replay(collector, transformer);
        generator.generate(jarPath, collector);
        EasyMock.verify(collector, transformer);
    }
}
