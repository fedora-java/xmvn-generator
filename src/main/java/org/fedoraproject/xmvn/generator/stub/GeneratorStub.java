package org.fedoraproject.xmvn.generator.stub;

import org.fedoraproject.xmvn.generator.jpms.JPMSGeneratorFactory;
import org.fedoraproject.xmvn.generator.transformer.TransformingPseudoGeneratorFactory;

public class GeneratorStub {
    private static final CompoundGenerator INSTANCE = new CompoundGenerator(new RpmBuildContext(),
            new TransformingPseudoGeneratorFactory(), new JPMSGeneratorFactory());

    public static String trampoline(String kind) {
        return INSTANCE.runGenerator(kind);
    }
}
