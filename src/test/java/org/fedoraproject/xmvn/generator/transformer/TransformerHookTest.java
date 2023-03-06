package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarInputStream;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TransformerHookTest {
    @TempDir
    private Path buildRoot;

    @Test
    public void testManifestInjection() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.createDirectories(buildRoot.resolve("another/dir"));
        Files.copy(Paths.get("src/test/resources/example.jar"), jarPath);
        ManifestTransformer transformer = mf -> mf.getMainAttributes().putValue("Injected", "Test");
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/foo"));
        generator.addDirectoryPrefix(Paths.get("/some/prefix"));
        generator.addDirectoryPrefix(Paths.get("/another/dir"));
        generator.run();
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(jarPath))) {
            assertEquals("Test", jis.getManifest().getMainAttributes().getValue("Injected"));
        }
    }

    @Test
    public void testInvalidJar() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.copy(Paths.get("src/test/resources/invalid.jar"), jarPath);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/some/prefix"));
        EasyMock.replay(transformer);
        generator.run();
        EasyMock.verify(transformer);
    }

    @Test
    public void testPrefixMismatch() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.createDirectories(buildRoot.resolve("mismatched/prefix"));
        Files.copy(Paths.get("src/test/resources/example.jar"), jarPath);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get("/mismatched/prefix"));
        EasyMock.replay(transformer);
        generator.run();
        EasyMock.verify(transformer);
    }
}
