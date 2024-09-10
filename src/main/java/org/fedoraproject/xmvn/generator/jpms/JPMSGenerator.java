package org.fedoraproject.xmvn.generator.jpms;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPMSGenerator implements Generator {
    private final BuildContext context;

    public JPMSGenerator(BuildContext context) {
        this.context = context;
    }

    @Override
    public void generate(Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        List<Path> prefixes = new ArrayList<>();
        prefixes.add(buildRoot.resolve("usr/lib/java"));
        prefixes.add(buildRoot.resolve("usr/share/java"));
        for (Path prefix : prefixes) {
            if (Files.isDirectory(prefix)) {
                try (Stream<Path> filePaths = Files.find(prefix, Integer.MAX_VALUE,
                        (path, attr) -> attr.isRegularFile() && path.getFileName().toString().endsWith(".jar"))) {
                    for (Path filePath : filePaths.toList()) {
                        ManifestGleaner manifestGleaner = new ManifestGleaner(filePath, collector);
                        ModuleInfoGleaner moduleInfoGleaner = new ModuleInfoGleaner(filePath, collector);
                        try (JarFile jarFile = new JarFile(filePath.toFile(), false, JarFile.OPEN_READ,
                                JarFile.runtimeVersion())) {
                            manifestGleaner.glean(jarFile.getManifest());
                            Iterator<JarEntry> it = jarFile.versionedStream().iterator();
                            while (it.hasNext()) {
                                JarEntry jarEntry = it.next();
                                if ("module-info.class".equals(jarEntry.getName())) {
                                    try (InputStream is = jarFile.getInputStream(jarEntry)) {
                                        moduleInfoGleaner.glean(is);
                                    }
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }
        }
    }

    @Override
    public String toString() {
        return "JPMS generator";
    }
}
