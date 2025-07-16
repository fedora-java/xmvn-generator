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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.logging.Logger;

class TransformerHook implements Hook {
    private final ManifestTransformer manifestTransformer;
    private final List<Path> prefixes = new ArrayList<>();
    private final Path buildRoot;

    public TransformerHook(ManifestTransformer transformer, Path buildRoot) {
        this.manifestTransformer = transformer;
        this.buildRoot = buildRoot;
    }

    public void addDirectoryPrefix(Path prefix) {
        prefixes.add(buildRoot.resolve(Path.of("/").relativize(prefix)));
    }

    @Override
    public void run() {
        try {
            for (Path prefix : prefixes) {
                if (!Files.isDirectory(prefix, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                List<Path> javaFiles;
                try (Stream<Path> paths =
                        Files.find(
                                prefix,
                                10,
                                (p, bfa) ->
                                        bfa.isRegularFile()
                                                && p.getFileName().toString().endsWith(".jar"))) {
                    javaFiles = paths.toList();
                }
                for (Path filePath : javaFiles) {
                    JarTransformer jarTransformer = new JarTransformer(manifestTransformer);
                    Logger.debug(
                            "injecting manifest into "
                                    + Path.of("/").resolve(buildRoot.relativize(filePath)));
                    try {
                        jarTransformer.transformJar(filePath);
                    } catch (IOException e) {
                        // Continue despite exception
                        Logger.debug(e);
                    }
                }
            }
        } catch (IOException e) {
            // Continue despite exception
            Logger.debug(e);
        }
    }

    @Override
    public String toString() {
        return "JAR transformer hook";
    }
}
