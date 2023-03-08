package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;

class TestGeneratorFactory1 implements GeneratorFactory {
    static Generator gen;

    @Override
    public Generator createGenerator(BuildContext context) {
        return gen;
    }
}

class TestGeneratorFactory2 implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return (Path filePath, Collector collector) -> {
            collector.addProvides(filePath.getFileName().toString());
            collector.addProvides("prov2 = 1.2.3");
            collector.addRequires("somereq");
            collector.addRequires("anotherdep >= 42");
        };
    }
}

public class CompoundGeneratorTest {
    @Test
    public void testCompoundGenerator() {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_generators}"))
                .andReturn("\n " + TestGeneratorFactory1.class.getName().toString() + " \n\t   "
                        + TestGeneratorFactory2.class.getName().toString() + " ");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one").times(2);
        Generator gen1 = EasyMock.createStrictMock(Generator.class);
        TestGeneratorFactory1.gen = gen1;
        gen1.generate(EasyMock.eq(Paths.get("/build/root/some/file/one")), EasyMock.isA(Collector.class));
        EasyMock.expectLastCall();
        EasyMock.replay(bc, gen1);
        CompoundGenerator cg = new CompoundGenerator(bc);
        String prov = cg.runGenerator("provides");
        assertEquals("one\nprov2 = 1.2.3", prov);
        String req = cg.runGenerator("requires");
        assertEquals("anotherdep >= 42\nsomereq", req);
        EasyMock.verify(bc, gen1);
    }

    @Test
    public void testClassNotFound() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_generators}")).andReturn("com.foo.Bar");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.replay(bc);
        try {
            new CompoundGenerator(bc).runGenerator("provides");
            fail("ClassNotFoundException expected");
        } catch (Throwable t) {
            assertInstanceOf(RuntimeException.class, t);
            Throwable e = t.getCause();
            assertInstanceOf(ClassNotFoundException.class, e);
            assertEquals("com.foo.Bar", e.getMessage());
        }
        EasyMock.verify(bc);
    }

    @Test
    public void testClassIsNotFactory() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_generators}")).andReturn(CompoundGeneratorTest.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.replay(bc);
        try {
            new CompoundGenerator(bc).runGenerator("provides");
            fail("ClassCastException expected");
        } catch (ClassCastException e) {
            assertTrue(e.getMessage().contains("GeneratorFactory"));
        }
        EasyMock.verify(bc);
    }

    @Test
    public void testNoFactories() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_generators}")).andReturn("");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one").anyTimes();
        EasyMock.replay(bc);
        String prov = new CompoundGenerator(bc).runGenerator("provides");
        assertEquals("", prov);
        EasyMock.verify(bc);
    }
}
