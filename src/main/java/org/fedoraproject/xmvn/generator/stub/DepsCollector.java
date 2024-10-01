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
package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.logging.Logger;

public class DepsCollector implements Collector {
    private final Map<Path, Set<String>> provides = new LinkedHashMap<>();
    private final Map<Path, Set<String>> requires = new LinkedHashMap<>();
    private final Path buildRoot;
    private Path lastPath;

    private void found(String kind, Path filePath, String dep) {
        if (!filePath.equals(lastPath)) {
            Path shortPath = Path.of("/").resolve(buildRoot.relativize(filePath));
            Logger.debug("=> " + shortPath);
            lastPath = filePath;
        }
        Logger.debug("  -> found " + kind + ": " + dep);
    }

    public DepsCollector(Path buildRoot) {
        this.buildRoot = buildRoot;
    }

    @Override
    public void addProvides(Path filePath, String dep) {
        provides.computeIfAbsent(filePath, x -> new TreeSet<>()).add(dep);
        found("Provides", filePath, dep);
    }

    @Override
    public void addRequires(Path filePath, String dep) {
        requires.computeIfAbsent(filePath, x -> new TreeSet<>()).add(dep);
        found("Requires", filePath, dep);
    }

    public Set<String> getDeps(Path filePath, String kind) {
        return switch (kind) {
        case "provides" -> provides.computeIfAbsent(filePath, x -> new TreeSet<>());
        case "requires" -> requires.computeIfAbsent(filePath, x -> new TreeSet<>());
        default -> Collections.emptySet();
        };
    }
}
