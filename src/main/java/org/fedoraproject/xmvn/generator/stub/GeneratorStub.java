package org.fedoraproject.xmvn.generator.stub;

public class GeneratorStub {
    private static final CompoundGenerator INSTANCE = new CompoundGenerator(new RpmBuildContext());

    public static String trampoline(String kind) {
        return INSTANCE.runGenerator(kind);
    }
}
