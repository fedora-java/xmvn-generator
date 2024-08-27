package org.fedoraproject.xmvn.generator;

import java.nio.file.Path;

public interface Collector {
    void addProvides(Path filePath, String name);
    void addRequires(Path filePath, String name);
}
