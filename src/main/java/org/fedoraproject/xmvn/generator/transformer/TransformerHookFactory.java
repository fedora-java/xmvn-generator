package org.fedoraproject.xmvn.generator.transformer;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

public class TransformerHookFactory implements HookFactory {
    @Override
    public Hook createHook(BuildContext context) {
        ManifestTransformer transformer = new ManifestInjector(context);
        Path buildRoot = Paths.get(context.eval("%{buildroot}"));
        TransformerHook hook = new TransformerHook(transformer, buildRoot);
        hook.addDirectoryPrefix(Paths.get(context.eval("%{_javadir}")));
        hook.addDirectoryPrefix(Paths.get(context.eval("%{_jnidir}")));
        return hook;
    }
}
