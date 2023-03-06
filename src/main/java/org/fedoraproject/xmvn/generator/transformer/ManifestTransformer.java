package org.fedoraproject.xmvn.generator.transformer;

import java.util.jar.Manifest;

interface ManifestTransformer {
    void transform(Manifest manifest);
}
