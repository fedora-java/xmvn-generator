package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;
import org.fedoraproject.xmvn.generator.logging.Logger;

class CompoundGenerator {
    private final BuildContext buildContext;
    private final List<FilteredGenerator> generators;
    private final Map<Path, DepsCollector> cache = new LinkedHashMap<>();

    private Generator loadGenerator(String cn) {
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            GeneratorFactory factory = (GeneratorFactory) cl.loadClass(cn).getDeclaredConstructor().newInstance();
            return factory.createGenerator(buildContext);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public CompoundGenerator(BuildContext buildContext) {
        this.buildContext = buildContext;
        if (!buildContext.eval("%{?__xmvngen_debug}").isEmpty()) {
            Logger.enableDebug();
        }
        Set<String> provCns = Set.of(buildContext.eval("%{?__xmvngen_provides_generators}").split("\\s+"));
        Set<String> reqCns = Set.of(buildContext.eval("%{?__xmvngen_requires_generators}").split("\\s+"));
        Set<String> allCns = new LinkedHashSet<>();
        allCns.addAll(provCns);
        allCns.addAll(reqCns);
        generators = allCns.stream().filter(cn -> !cn.isEmpty())
                .map(cn -> new FilteredGenerator(loadGenerator(cn), provCns.contains(cn), reqCns.contains(cn)))
                .collect(Collectors.toUnmodifiableList());
        if (generators.isEmpty()) {
            buildContext.eval("%{warn:xmvn-generator: no generators were specified}");
        }
    }

    public String runGenerator(String kind) {
        Path filePath = Paths.get(buildContext.eval("%1"));
        DepsCollector collector = cache.get(filePath);
        if (collector == null) {
            collector = new DepsCollector();
            cache.put(filePath, collector);
            Logger.startLogging();
            Path buildRoot = Paths.get(buildContext.eval("%{buildroot}"));
            Path shortPath = Paths.get("/").resolve(buildRoot.relativize(filePath));
            Logger.debug(shortPath.toString());
            for (Generator generator : generators) {
                Logger.startNewSection();
                Logger.debug("=> Running generator " + generator);
                generator.generate(filePath, collector);
            }
            Logger.finishLogging();
        }
        return String.join("\n", collector.getDeps(kind));
    }
}
