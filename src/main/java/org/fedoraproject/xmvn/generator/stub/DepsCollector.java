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
