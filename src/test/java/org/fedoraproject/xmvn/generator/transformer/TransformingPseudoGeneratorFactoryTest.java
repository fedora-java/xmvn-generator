package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class TransformingPseudoGeneratorFactoryTest {
    @Test
    public void testFactory() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/tmp/build/root");
        EasyMock.expect(bc.eval("%{_javadir}")).andReturn("/tmp/javadir");
        EasyMock.expect(bc.eval("%{_jnidir}")).andReturn("/tmp/jnidir");
        EasyMock.replay(bc);
        GeneratorFactory factory = new TransformingPseudoGeneratorFactory();
        Generator gen = factory.createGenerator(bc);
        assertInstanceOf(TransformingPseudoGenerator.class, gen);
        EasyMock.verify(bc);
    }
}
