package org.fedoraproject.xmvn.generator.transformer;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class TransformingPseudoGeneratorFactory implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        ManifestTransformer transformer = new ManifestInjector(context);
        Path buildRoot = Paths.get(context.eval("%{buildroot}"));
        TransformingPseudoGenerator generator = new TransformingPseudoGenerator(transformer, buildRoot);
        generator.addDirectoryPrefix(Paths.get(context.eval("%{_javadir}")));
        generator.addDirectoryPrefix(Paths.get(context.eval("%{_jnidir}")));
        return generator;
    }
}
