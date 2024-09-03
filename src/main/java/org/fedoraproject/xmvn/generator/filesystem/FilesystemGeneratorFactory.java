package org.fedoraproject.xmvn.generator.filesystem;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class FilesystemGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return new FilesystemGenerator(context);
    }
}
