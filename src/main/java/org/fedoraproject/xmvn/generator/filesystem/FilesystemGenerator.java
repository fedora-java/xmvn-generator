/*-
 * Copyright (c) 2024 Red Hat, Inc.
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
package org.fedoraproject.xmvn.generator.filesystem;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class FilesystemGenerator implements Generator {
    private final BuildContext context;

    public FilesystemGenerator(BuildContext context) {
        this.context = context;
    }

    @Override
    public void generate(Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        List<Path> prefixes = new ArrayList<>();
        prefixes.add(buildRoot.resolve("etc/java"));
        prefixes.add(buildRoot.resolve("etc/jvm"));
        prefixes.add(buildRoot.resolve("usr/lib/eclipse"));
        prefixes.add(buildRoot.resolve("usr/lib/java"));
        prefixes.add(buildRoot.resolve("usr/lib/jvm"));
        prefixes.add(buildRoot.resolve("usr/lib/jvm-common"));
        prefixes.add(buildRoot.resolve("usr/lib/jvm-private"));
        prefixes.add(buildRoot.resolve("usr/share/eclipse"));
        prefixes.add(buildRoot.resolve("usr/share/ivy-xmls"));
        prefixes.add(buildRoot.resolve("usr/share/java"));
        prefixes.add(buildRoot.resolve("usr/share/javadoc"));
        prefixes.add(buildRoot.resolve("usr/share/jvm"));
        prefixes.add(buildRoot.resolve("usr/share/jvm-common"));
        prefixes.add(buildRoot.resolve("usr/share/maven-metadata"));
        prefixes.add(buildRoot.resolve("usr/share/maven-poms"));
        for (Path prefix : prefixes) {
            if (Files.isDirectory(prefix)) {
                try (Stream<Path> filePaths = Files.find(prefix, Integer.MAX_VALUE,
                        (path, attr) -> !path.equals(prefix))) {
                    for (Path filePath : filePaths.toList()) {
                        collector.addRequires(filePath, "javapackages-filesystem");
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "javapackages-filesystem generator";
    }
}
