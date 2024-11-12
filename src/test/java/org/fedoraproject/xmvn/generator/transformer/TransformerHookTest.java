/*-
 * Copyright (c) 2023-2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarInputStream;
import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class TransformerHookTest {
    @TempDir private Path buildRoot;

    @Test
    public void testManifestInjection() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.createDirectories(buildRoot.resolve("another/dir"));
        Files.copy(Path.of("src/test/resources/example.jar"), jarPath);
        ManifestTransformer transformer = mf -> mf.getMainAttributes().putValue("Injected", "Test");
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Path.of("/foo"));
        generator.addDirectoryPrefix(Path.of("/some/prefix"));
        generator.addDirectoryPrefix(Path.of("/another/dir"));
        generator.run();
        try (JarInputStream jis = new JarInputStream(Files.newInputStream(jarPath))) {
            assertEquals("Test", jis.getManifest().getMainAttributes().getValue("Injected"));
        }
    }

    @Test
    public void testInvalidJar() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.copy(Path.of("src/test/resources/invalid.jar"), jarPath);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Path.of("/some/prefix"));
        EasyMock.replay(transformer);
        generator.run();
        EasyMock.verify(transformer);
    }

    @Test
    public void testPrefixMismatch() throws Exception {
        Path jarPath = buildRoot.resolve("some/prefix/abc.jar");
        Files.createDirectories(buildRoot.resolve("some/prefix"));
        Files.createDirectories(buildRoot.resolve("mismatched/prefix"));
        Files.copy(Path.of("src/test/resources/example.jar"), jarPath);
        ManifestTransformer transformer = EasyMock.createStrictMock(ManifestTransformer.class);
        TransformerHook generator = new TransformerHook(transformer, buildRoot);
        generator.addDirectoryPrefix(Path.of("/mismatched/prefix"));
        EasyMock.replay(transformer);
        generator.run();
        EasyMock.verify(transformer);
    }
}
