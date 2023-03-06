package org.fedoraproject.xmvn.generator.transformer;

import java.util.jar.Manifest;

import org.fedoraproject.xmvn.generator.BuildContext;

class ManifestInjector implements ManifestTransformer {
    private final BuildContext buildContext;

    public ManifestInjector(BuildContext context) {
        this.buildContext = context;
    }

    private void inject(Manifest mf, String key, String rpmExpr) {
        String value = buildContext.eval(rpmExpr);
        mf.getMainAttributes().putValue(key, value);
    }

    @Override
    public void transform(Manifest mf) {
        inject(mf, "Rpm-Name", "%{NAME}");
        inject(mf, "Rpm-Epoch", "%{?EPOCH}");
        inject(mf, "Rpm-Version", "%{VERSION}");
        inject(mf, "Rpm-Release", "%{RELEASE}");
    }
}
