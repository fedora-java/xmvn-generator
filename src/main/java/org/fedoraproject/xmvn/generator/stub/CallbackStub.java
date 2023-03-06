package org.fedoraproject.xmvn.generator.stub;

import java.io.IOException;

import org.fedoraproject.xmvn.generator.transformer.TransformerHookFactory;

public class CallbackStub {
    private static final CompoundHook INSTANCE = new CompoundHook(new RpmBuildContext(),
            new TransformerHookFactory());

    public static String postInstall() throws IOException {
        return INSTANCE.setUpHook();
    }
}
