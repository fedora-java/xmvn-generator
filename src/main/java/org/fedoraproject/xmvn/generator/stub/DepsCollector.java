package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
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

    public DepsCollector(List<Path> filePaths, Path buildRoot) {
        this.buildRoot = buildRoot;
        for (Path filePath : filePaths) {
            provides.put(filePath, new TreeSet<>());
            requires.put(filePath, new TreeSet<>());
        }
    }

    @Override
    public void addProvides(Path filePath, String dep) {
        provides.get(filePath).add(dep);
        found("Provides", filePath, dep);
    }

    @Override
    public void addRequires(Path filePath, String dep) {
        requires.get(filePath).add(dep);
        found("Requires", filePath, dep);
    }

    public Set<String> getDeps(Path filePath, String kind) {
        return switch (kind) {
        case "provides" -> provides.get(filePath);
        case "requires" -> requires.get(filePath);
        default -> Collections.emptySet();
        };
    }
}
