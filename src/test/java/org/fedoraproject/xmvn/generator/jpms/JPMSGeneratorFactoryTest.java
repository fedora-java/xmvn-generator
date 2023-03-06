package org.fedoraproject.xmvn.generator.jpms;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class JPMSGeneratorFactoryTest {
    @Test
    public void testFactory() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.replay(bc);
        GeneratorFactory factory = new JPMSGeneratorFactory();
        Generator gen = factory.createGenerator(bc);
        assertInstanceOf(JPMSGenerator.class, gen);
        EasyMock.verify(bc);
    }
}
