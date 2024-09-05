package org.fedoraproject.xmvn.generator.jpscript;

import java.nio.file.Path;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;

public class JPackageScriptGeneratorTest {
    private Collector collector;
    private BuildContext context;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
        context = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(context.eval("%{buildroot}")).andReturn("src/test/resources").anyTimes();
    }

    private void expectRequires(String req) {
        collector.addRequires(EasyMock.anyObject(Path.class), EasyMock.eq(req));
        EasyMock.expectLastCall();
    }

    private void performTest(String script) {
        Path scriptPath = Path.of("src/test/resources/usr/bin").resolve(script);
        EasyMock.replay(collector, context);
        new JPackageScriptGenerator(context).generate(List.of(scriptPath), collector);
        EasyMock.verify(collector, context);
    }

    @Test
    public void testJPackage() {
        expectRequires("javapackages-tools");
        performTest("jflex");
    }

    @Test
    public void testNonJPackage() {
        performTest("xmvn");
    }
}
