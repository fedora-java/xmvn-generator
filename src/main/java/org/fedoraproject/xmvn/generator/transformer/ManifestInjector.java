package org.fedoraproject.xmvn.generator.transformer;

import java.util.jar.Manifest;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.logging.Logger;

class ManifestInjector implements ManifestTransformer {
    private final BuildContext buildContext;

    public ManifestInjector(BuildContext context) {
        this.buildContext = context;
    }

    private void inject(Manifest mf, String key, String rpmExpr) {
        String value = buildContext.eval(rpmExpr);
        mf.getMainAttributes().putValue(key, value);
        Logger.debug("Injected manifest attribute " + key + ": " + value);
    }

    @Override
    public void transform(Manifest mf) {
        inject(mf, "Rpm-Name", "%{name}");
        inject(mf, "Rpm-Epoch", "%{?epoch}");
        inject(mf, "Rpm-Version", "%{version}");
        inject(mf, "Rpm-Release", "%{release}");
        inject(mf, "Rpm-License", "%{license}");
        inject(mf, "Rpm-Source-Name", "%{NAME}");
        inject(mf, "Rpm-Source-Epoch", "%{?EPOCH}");
        inject(mf, "Rpm-Source-Version", "%{VERSION}");
        inject(mf, "Rpm-Source-Release", "%{RELEASE}");
    }
}
