package org.fedoraproject.xmvn.generator;

import java.nio.file.Path;

public interface Generator {
    void generate(Path filePath, Collector collector);
}
