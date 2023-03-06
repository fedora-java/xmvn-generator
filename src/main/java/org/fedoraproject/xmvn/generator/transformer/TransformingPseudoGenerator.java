package org.fedoraproject.xmvn.generator.transformer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.logging.Logger;

class TransformingPseudoGenerator implements Generator {
    private final ManifestTransformer manifestTransformer;
    private final List<Path> prefixes = new ArrayList<>();
    private final Path buildRoot;

    public TransformingPseudoGenerator(ManifestTransformer transformer, Path buildRoot) {
        this.manifestTransformer = transformer;
        this.buildRoot = buildRoot;
    }

    public void addDirectoryPrefix(Path prefix) {
        prefixes.add(buildRoot.resolve(Paths.get("/").relativize(prefix)));
    }

    @Override
    public void generate(Path filePath, Collector collector) {
        if (filePath.getFileName().toString().endsWith(".jar")) {
            if (prefixes.stream().anyMatch(prefix -> filePath.startsWith(prefix))) {
                JarTransformer jarTransformer = new JarTransformer(manifestTransformer);
                try {
                    jarTransformer.transformJar(filePath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.debug("JAR transformation skipped: " + filePath);
            }
        }
    }
}
