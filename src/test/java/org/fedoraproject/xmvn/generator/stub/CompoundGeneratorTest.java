package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.file.Path;
import java.util.List;

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
        return (List<Path> filePaths, Collector collector) -> {
            for (Path filePath : filePaths) {
                collector.addProvides(filePath, filePath.getFileName().toString());
                collector.addProvides(filePath, "prov2 = 1.2.3");
                collector.addRequires(filePath, "somereq");
                collector.addRequires(filePath, "anotherdep >= 42");
            }
        };
    }
}

abstract class AbstractTestGeneratorFactory implements GeneratorFactory {
    private final String id;

    public AbstractTestGeneratorFactory(String id) {
        this.id = id;
    }

    @Override
    public Generator createGenerator(BuildContext context) {
        return (List<Path> filePaths, Collector collector) -> {
            for (Path filePath : filePaths) {
                collector.addProvides(filePath, "Prov" + id + "1");
                collector.addProvides(filePath, "Prov" + id + "2");
                collector.addRequires(filePath, "Req" + id + "1");
                collector.addRequires(filePath, "Req" + id + "2");
            }
        };
    }
}

class TGFA extends AbstractTestGeneratorFactory {
    public TGFA() {
        super("A");
    }
}

class TGFB extends AbstractTestGeneratorFactory {
    public TGFB() {
        super("B");
    }
}

class TGFC extends AbstractTestGeneratorFactory {
    public TGFC() {
        super("C");
    }
}

class TestGeneratorFactory3 implements GeneratorFactory {
    @Override
    public Generator createGenerator(BuildContext context) {
        return (List<Path> filePaths, Collector collector) -> {
            for (Path filePath : filePaths) {
                collector.addProvides(filePath, "prov" + filePath.getFileName().toString().length());
                if (filePath.toString().endsWith("3")) {
                    collector.addRequires(filePath, "req");
                }
            }
        };
    }
}

public class CompoundGeneratorTest {
    @Test
    public void testCompoundGenerator() {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        String generators = "\n " + TestGeneratorFactory1.class.getName() + " \n\t   "
                + TestGeneratorFactory2.class.getName() + " ";
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}")).andReturn(generators);
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}")).andReturn(generators);
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%#")).andReturn("1").times(2);
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one").times(2);
        Generator gen1 = EasyMock.createStrictMock(Generator.class);
        TestGeneratorFactory1.gen = gen1;
        gen1.generate(EasyMock.eq(List.of(Path.of("/build/root/some/file/one"))), EasyMock.isA(Collector.class));
        EasyMock.expectLastCall();
        EasyMock.replay(bc, gen1);
        CompoundGenerator cg = new CompoundGenerator(bc);
        String prov = cg.runGenerator("provides");
        assertEquals("one\nprov2 = 1.2.3\n", prov);
        String req = cg.runGenerator("requires");
        assertEquals("anotherdep >= 42\nsomereq\n", req);
        EasyMock.verify(bc, gen1);
    }

    @Test
    public void testClassNotFound() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}")).andReturn("com.foo.Bar");
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}")).andReturn("com.foo.Bar");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.replay(bc);
        try {
            new CompoundGenerator(bc).runGenerator("provides");
            fail("ClassNotFoundException expected");
        } catch (RuntimeException e) {
            Throwable c = e.getCause();
            assertInstanceOf(ClassNotFoundException.class, c);
            assertEquals("com.foo.Bar", c.getMessage());
        }
        EasyMock.verify(bc);
    }

    @Test
    public void testClassIsNotFactory() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}")).andReturn(CompoundGeneratorTest.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}")).andReturn(CompoundGeneratorTest.class.getName());
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
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}")).andReturn("");
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}")).andReturn("");
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%#")).andReturn("1");
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one");
        EasyMock.expect(bc.eval("%{warn:xmvn-generator: no generators were specified}")).andReturn("");
        EasyMock.replay(bc);
        String prov = new CompoundGenerator(bc).runGenerator("provides");
        assertEquals("", prov);
        EasyMock.verify(bc);
    }

    @Test
    public void testFiltering() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}"))
                .andReturn(TGFA.class.getName() + " " + TGFC.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}"))
                .andReturn(TGFB.class.getName() + " " + TGFC.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%#")).andReturn("1").times(2);
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/some/file/one").times(2);
        EasyMock.replay(bc);
        CompoundGenerator cg = new CompoundGenerator(bc);
        String prov = cg.runGenerator("provides");
        assertEquals("ProvA1\nProvA2\nProvC1\nProvC2\n", prov);
        String req = cg.runGenerator("requires");
        assertEquals("ReqB1\nReqB2\nReqC1\nReqC2\n", req);
        EasyMock.verify(bc);
    }

    @Test
    public void testMultifile() throws Exception {
        BuildContext bc = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{?__xmvngen_provides_generators}")).andReturn(TestGeneratorFactory3.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_requires_generators}")).andReturn(TestGeneratorFactory3.class.getName());
        EasyMock.expect(bc.eval("%{?__xmvngen_debug}")).andReturn("").anyTimes();
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/build/root").anyTimes();
        EasyMock.expect(bc.eval("%#")).andReturn("5").times(2);
        EasyMock.expect(bc.eval("%1")).andReturn("/build/root/f").times(2);
        EasyMock.expect(bc.eval("%2")).andReturn("/build/root/f2").times(2);
        EasyMock.expect(bc.eval("%3")).andReturn("/build/root/ff3").times(2);
        EasyMock.expect(bc.eval("%4")).andReturn("/build/root/file").times(2);
        EasyMock.expect(bc.eval("%5")).andReturn("/build/root/file5").times(2);
        EasyMock.replay(bc);
        CompoundGenerator cg = new CompoundGenerator(bc);
        String prov = cg.runGenerator("provides");
        assertEquals(
                ";/build/root/f\nprov1\n;/build/root/f2\nprov2\n;/build/root/ff3\nprov3\n;/build/root/file\nprov4\n;/build/root/file5\nprov5\n",
                prov);
        String req = cg.runGenerator("requires");
        assertEquals(";/build/root/ff3\nreq\n", req);
        EasyMock.verify(bc);
    }
}
