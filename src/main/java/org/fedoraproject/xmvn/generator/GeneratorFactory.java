package org.fedoraproject.xmvn.generator;

public interface GeneratorFactory {
    Generator createGenerator(BuildContext context);
}
