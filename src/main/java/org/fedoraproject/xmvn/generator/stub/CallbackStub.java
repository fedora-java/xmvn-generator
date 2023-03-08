package org.fedoraproject.xmvn.generator.stub;

import java.io.IOException;

public class CallbackStub {
    private static final CompoundHook INSTANCE = new CompoundHook(new RpmBuildContext());

    public static String postInstall() throws IOException {
        return INSTANCE.setUpHook();
    }
}
