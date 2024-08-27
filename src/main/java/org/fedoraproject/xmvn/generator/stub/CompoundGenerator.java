package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.util.ArrayList;
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
    private DepsCollector collector;

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
        int n = Integer.parseInt(buildContext.eval("%#"));
        boolean multifile = n != 1;
        List<Path> filePaths = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            filePaths.add(Path.of(buildContext.eval("%" + i)));
        }
        if (!multifile) {
            collector = cache.get(filePaths.getFirst());
        }
        if (collector == null) {
            Path buildRoot = Path.of(buildContext.eval("%{buildroot}"));
            collector = new DepsCollector(filePaths, buildRoot);
            if (!multifile) {
                cache.put(filePaths.getFirst(), collector);
            }
            Logger.startLogging();
            for (Generator generator : generators) {
                Logger.startNewSection();
                Logger.debug("Running " + generator + " (" + generator.getClass().getCanonicalName() + ")");
                generator.generate(filePaths, collector);
            }
            Logger.finishLogging();
        }
        StringBuilder sb = new StringBuilder();
        for (Path filePath : filePaths) {
            Set<String> deps = collector.getDeps(filePath, kind);
            if (multifile && !deps.isEmpty()) {
                sb.append(';').append(filePath).append('\n');
            }
            for (String dep : deps) {
                sb.append(dep).append('\n');
            }
        }
        return sb.toString();
    }
}
