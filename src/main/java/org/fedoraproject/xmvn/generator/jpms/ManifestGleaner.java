package org.fedoraproject.xmvn.generator.jpms;

import java.util.jar.Manifest;

import org.fedoraproject.xmvn.generator.Collector;

class ManifestGleaner {
    private final Collector collector;

    public ManifestGleaner(Collector collector) {
        this.collector = collector;
    }

    public void glean(Manifest mf) {
        String autoName = mf.getMainAttributes().getValue("Automatic-Module-Name");
        if (autoName != null) {
            collector.addProvides("jpms(" + autoName + ")");
        }
    }
}
