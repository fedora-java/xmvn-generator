package org.fedoraproject.xmvn.generator;

import java.nio.file.Path;
import java.util.List;

public interface Generator {
    void generate(List<Path> filePaths, Collector collector);
}
