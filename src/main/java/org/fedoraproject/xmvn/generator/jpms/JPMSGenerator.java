package org.fedoraproject.xmvn.generator.jpms;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPMSGenerator implements Generator {
    @Override
    public void generate(Path filePath, Collector collector) {
        ManifestGleaner manifestGleaner = new ManifestGleaner(collector);
        ModuleInfoGleaner moduleInfoGleaner = new ModuleInfoGleaner(collector);
        try (JarInputStream jarInputStream = new JarInputStream(Files.newInputStream(filePath))) {
            manifestGleaner.glean(jarInputStream.getManifest());
            for (JarEntry jarEntry; (jarEntry = jarInputStream.getNextJarEntry()) != null;) {
                if ("module-info.class".equals(jarEntry.getRealName())) {
                    moduleInfoGleaner.glean(jarInputStream);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
