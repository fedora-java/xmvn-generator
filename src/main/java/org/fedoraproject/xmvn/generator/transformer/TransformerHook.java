package org.fedoraproject.xmvn.generator.transformer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.logging.Logger;

class TransformerHook implements Hook {
    private final ManifestTransformer manifestTransformer;
    private final List<Path> prefixes = new ArrayList<>();
    private final Path buildRoot;

    public TransformerHook(ManifestTransformer transformer, Path buildRoot) {
        this.manifestTransformer = transformer;
        this.buildRoot = buildRoot;
    }

    public void addDirectoryPrefix(Path prefix) {
        prefixes.add(buildRoot.resolve(Paths.get("/").relativize(prefix)));
    }

    @Override
    public void run() {
        try {
            for (Path prefix : prefixes) {
                if (!Files.isDirectory(prefix, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                List<Path> javaFiles = Files
                        .find(prefix, 10,
                                (p, bfa) -> bfa.isRegularFile() && p.getFileName().toString().endsWith(".jar"))
                        .collect(Collectors.toList());
                for (Path filePath : javaFiles) {
                    JarTransformer jarTransformer = new JarTransformer(manifestTransformer);
                    Logger.debug("injecting manifest into " + Paths.get("/").resolve(buildRoot.relativize(filePath)));
                    try {
                        jarTransformer.transformJar(filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
