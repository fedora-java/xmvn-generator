package org.fedoraproject.xmvn.generator.stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.fedoraproject.xmvn.generator.BuildContext;

class RpmBuildContext implements BuildContext {
    static {
        try (InputStream is = RpmBuildContext.class.getResourceAsStream("/xmvn-generator-native.so")) {
            Path p = Files.createTempFile("xmvngen-native", ".so");
            try (OutputStream os = Files.newOutputStream(p)) {
                is.transferTo(os);
            }
            System.load(p.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public native String eval(String macro);
}
