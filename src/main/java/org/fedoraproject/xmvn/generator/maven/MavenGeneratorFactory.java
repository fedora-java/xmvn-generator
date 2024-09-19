package org.fedoraproject.xmvn.generator.maven;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class MavenGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return new MavenGenerator(context);
    }
}
