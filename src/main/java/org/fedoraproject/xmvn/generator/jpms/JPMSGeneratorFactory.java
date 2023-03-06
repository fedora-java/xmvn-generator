package org.fedoraproject.xmvn.generator.jpms;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class JPMSGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return new JPMSGenerator();
    }
}
