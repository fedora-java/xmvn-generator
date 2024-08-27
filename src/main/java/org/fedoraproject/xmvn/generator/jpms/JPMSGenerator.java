package org.fedoraproject.xmvn.generator.jpms;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPMSGenerator implements Generator {
    @Override
    public void generate(List<Path> filePaths, Collector collector) {
        for (Path filePath : filePaths) {
            ManifestGleaner manifestGleaner = new ManifestGleaner(filePath, collector);
            ModuleInfoGleaner moduleInfoGleaner = new ModuleInfoGleaner(filePath, collector);
            try (JarFile jarFile = new JarFile(filePath.toFile(), false, JarFile.OPEN_READ, JarFile.runtimeVersion())) {
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
    }

    @Override
    public String toString() {
        return "JPMS generator";
    }
}
