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
package org.fedoraproject.xmvn.generator.jpscript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPackageScriptGenerator implements Generator {
    private final BuildContext context;

    public JPackageScriptGenerator(BuildContext context) {
        this.context = context;
    }

    @Override
    public void generate(Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        Path binDir = buildRoot.resolve("usr/bin");
        if (Files.isDirectory(binDir)) {
            try (Stream<Path> filePaths = Files.find(binDir, 1, (path, attr) -> attr.isRegularFile())) {
                for (Path filePath : filePaths.toList()) {
                    String content = Files.readString(filePath);
                    if (content.contains("\n. /usr/share/java-utils/java-functions\n")) {
                        collector.addRequires(filePath, "javapackages-tools");
                    }
                    if (content.contains("\nexport JAVA_HOME=\"${JAVA_HOME:-/usr/lib/jvm/jre-21-openjdk}\"\n")) {
                        collector.addRequires(filePath, "java-21-openjdk-headless");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public String toString() {
        return "jpackage_script generator";
    }
}
