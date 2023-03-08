package org.fedoraproject.xmvn.generator.stub;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;
import org.fedoraproject.xmvn.generator.logging.Logger;

class CompoundGenerator {
    private final BuildContext buildContext;
    private final List<Generator> generators = new ArrayList<>();
    private final Map<Path, DepsCollector> cache = new LinkedHashMap<>();

    public CompoundGenerator(BuildContext buildContext) {
        this.buildContext = buildContext;
        if (!buildContext.eval("%{?__xmvngen_debug}").isEmpty()) {
            Logger.enableDebug();
        }
        try {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            for (String cn : buildContext.eval("%{?__xmvngen_generators}").split("\\s+")) {
                if (!cn.isEmpty()) {
                    GeneratorFactory factory = (GeneratorFactory) cl.loadClass(cn).getDeclaredConstructor()
                            .newInstance();
                    generators.add(factory.createGenerator(buildContext));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
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
                Logger.debug("=> Running generator " + generator.getClass().getSimpleName());
                generator.generate(filePath, collector);
            }
            Logger.finishLogging();
        }
        return String.join("\n", collector.getDeps(kind));
    }
}
