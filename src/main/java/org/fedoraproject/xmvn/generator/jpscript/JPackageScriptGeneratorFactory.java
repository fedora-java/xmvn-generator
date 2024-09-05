package org.fedoraproject.xmvn.generator.jpscript;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class JPackageScriptGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return new JPackageScriptGenerator(context);
    }
}
