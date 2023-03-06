package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;
import org.fedoraproject.xmvn.generator.logging.Logger;

class CompoundGenerator {
    private final BuildContext buildContext;
    private final List<Generator> generators;
    private final Map<Path, DepsCollector> cache = new LinkedHashMap<>();

    public CompoundGenerator(BuildContext buildContext, GeneratorFactory... factories) {
        this.buildContext = buildContext;
        generators = Arrays.asList(factories).stream().map(factory -> factory.createGenerator(buildContext))
                .collect(Collectors.toList());
    }

    public String runGenerator(String kind) {
        if (!buildContext.eval("%{?__xmvngen_debug}").isEmpty()) {
            Logger.enableDebug();
        }
        Path filePath = Paths.get(buildContext.eval("%1"));
        DepsCollector collector = cache.get(filePath);
        if (collector == null) {
            collector = new DepsCollector();
            cache.put(filePath, collector);
            Logger.beg();
            Path buildRoot = Paths.get(buildContext.eval("%{buildroot}"));
            Path shortPath = Paths.get("/").resolve(buildRoot.relativize(filePath));
            Logger.debug(shortPath.toString());
            for (Generator generator : generators) {
                Logger.cut();
                Logger.debug("=> Running generator " + generator.getClass().getSimpleName());
                generator.generate(filePath, collector);
            }
            Logger.end();
        }
        return String.join("\n", collector.getDeps(kind));
    }
}
