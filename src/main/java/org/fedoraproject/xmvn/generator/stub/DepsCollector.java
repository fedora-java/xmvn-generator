package org.fedoraproject.xmvn.generator.stub;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.logging.Logger;

public class DepsCollector implements Collector {
    private final Set<String> provides = new TreeSet<>();
    private final Set<String> requires = new TreeSet<>();

    private void found(String kind, String dep) {
        Logger.debug("  -> found " + kind + ": " + dep);
    }

    @Override
    public void addProvides(String dep) {
        provides.add(dep);
        found("Provides", dep);
    }

    @Override
    public void addRequires(String dep) {
        requires.add(dep);
        found("Requires", dep);
    }

    public Set<String> getDeps(String kind) {
        return switch (kind) {
        case "provides" -> provides;
        case "requires" -> requires;
        default -> Collections.emptySet();
        };
    }
}
