package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Paths;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

public class CompoundGeneratorTest {
    @Test
    public void testCompoundGenerator() {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one").times(2);
        Generator gen1 = EasyMock.createStrictMock(Generator.class);
        GeneratorFactory fac1 = EasyMock.createStrictMock(GeneratorFactory.class);
        EasyMock.expect(fac1.createGenerator(bc)).andReturn(gen1);
        gen1.generate(EasyMock.eq(Paths.get("/build/root/some/file/one")), EasyMock.isA(Collector.class));
        EasyMock.expectLastCall();
        Generator gen2 = (filePath, collector) -> {
            collector.addProvides(filePath.getFileName().toString());
            collector.addProvides("prov2 = 1.2.3");
            collector.addRequires("somereq");
            collector.addRequires("anotherdep >= 42");
        };
        GeneratorFactory fac2 = x -> gen2;
        EasyMock.replay(bc, gen1, fac1);
        CompoundGenerator cg = new CompoundGenerator(bc, fac1, fac2);
        String prov = cg.runGenerator("provides");
        assertEquals("one\nprov2 = 1.2.3", prov);
        String req = cg.runGenerator("requires");
        assertEquals("anotherdep >= 42\nsomereq", req);
        EasyMock.verify(bc, gen1, fac1);
    }
}
