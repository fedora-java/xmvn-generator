package org.fedoraproject.xmvn.generator.jpms;

import java.nio.file.Path;
import java.util.jar.Manifest;

import org.fedoraproject.xmvn.generator.Collector;

class ManifestGleaner {
    private final Collector collector;
    private final Path filePath;

    public ManifestGleaner(Path filePath, Collector collector) {
        this.filePath = filePath;
        this.collector = collector;
    }

    public void glean(Manifest mf) {
        if (mf != null) {
            String autoName = mf.getMainAttributes().getValue("Automatic-Module-Name");
            if (autoName != null) {
                collector.addProvides(filePath, "jpms(" + autoName + ")");
            }
        }
    }
}
