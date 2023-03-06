package org.fedoraproject.xmvn.generator.stub;

import org.fedoraproject.xmvn.generator.jpms.JPMSGeneratorFactory;

public class GeneratorStub {
    private static final CompoundGenerator INSTANCE = new CompoundGenerator(new RpmBuildContext(),
            new JPMSGeneratorFactory());

    public static String trampoline(String kind) {
        return INSTANCE.runGenerator(kind);
    }
}
