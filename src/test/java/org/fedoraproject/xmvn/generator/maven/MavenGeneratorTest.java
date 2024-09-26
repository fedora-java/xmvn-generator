package org.fedoraproject.xmvn.generator.maven;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;

import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.fedoraproject.xmvn.artifact.DefaultArtifact;
import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.locator.ServiceLocatorFactory;
import org.fedoraproject.xmvn.metadata.MetadataResolver;
import org.fedoraproject.xmvn.resolver.ResolutionRequest;
import org.fedoraproject.xmvn.resolver.ResolutionResult;
import org.fedoraproject.xmvn.resolver.Resolver;

public class MavenGeneratorTest {
    private Collector collector;
    private BuildContext context;
    private MetadataResolver metadataResolver;
    private Resolver resolver;
    @TempDir
    private Path br;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
        context = EasyMock.createMock(BuildContext.class);
        resolver = EasyMock.createMock(Resolver.class);
        metadataResolver = new ServiceLocatorFactory().createServiceLocator().getService(MetadataResolver.class);
    }

    private void addBrFile(String loc, String content) throws Exception {
        Path path = br.resolve(loc);
        Files.createDirectories(path.getParent());
        Files.writeString(path, content);
    }

    private void addMd(String pkg, String md) throws Exception {
        addBrFile("usr/share/maven-metadata/" + pkg + ".xml", md);
    }

    private void addMd(String md) throws Exception {
        addMd("main", md);
    }

    private IExpectationSetters<Object> expectProv(String prov) {
        return expectProv("main", prov);
    }

    private IExpectationSetters<Object> expectReq(String req) {
        return expectReq("main", req);
    }

    private IExpectationSetters<Object> expectProv(String pkg, String prov) {
        collector.addProvides(br.resolve("usr/share/maven-metadata").resolve(pkg + ".xml"), prov);
        return EasyMock.expectLastCall();
    }

    private IExpectationSetters<Object> expectReq(String pkg, String req) {
        collector.addRequires(br.resolve("usr/share/maven-metadata").resolve(pkg + ".xml"), req);
        return EasyMock.expectLastCall();
    }

    private void mockResolver(String art, String path, String ns, String cver) {
        ResolutionResult res = EasyMock.createMock(ResolutionResult.class);
        if (path != null) {
            EasyMock.expect(res.getArtifactPath()).andReturn(Path.of(path));
            EasyMock.expect(res.getCompatVersion()).andReturn(cver);
            EasyMock.expect(res.getNamespace()).andReturn(ns);
        } else {
            EasyMock.expect(res.getArtifactPath()).andReturn(null);
        }
        EasyMock.replay(res);
        ResolutionRequest req = new ResolutionRequest(new DefaultArtifact(art));
        EasyMock.expect(resolver.resolve(req)).andReturn(res);
    }

    private void performTest() throws Exception {
        EasyMock.expect(context.eval("%{buildroot}")).andReturn(br.toString()).atLeastOnce();
        EasyMock.replay(collector, context, resolver);
        new MavenGenerator(context, metadataResolver, resolver).generate(collector);
        EasyMock.verify(collector, context, resolver);
    }

    @Test
    public void testNoMetadata() throws Exception {
        performTest();
    }

    @Test
    public void testNoArtifacts() throws Exception {
        addMd("""
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <uuid>0ebc9caa-eb88-4c53-b371-72514294b535</uuid>
                  <skippedArtifacts>
                    <skippedArtifact>
                      <groupId>org.eclipse.gef</groupId>
                      <artifactId>org.eclipse.gef.releng</artifactId>
                      <extension>pom</extension>
                    </skippedArtifact>
                  </skippedArtifacts>
                </metadata>""");
        performTest();
    }

    @Test
    public void testSimple() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/ant-factory-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        performTest();
    }

    @Disabled("XMvn always ignores invalid metadata files")
    @Test
    public void testEmptyMetadataFile() throws Exception {
        addMd("");
        try {
            performTest();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Premature end of file"));
        }
    }

    @Disabled("XMvn always ignores invalid metadata files")
    @Test
    public void testInvalidMetadata() throws Exception {
        addMd("""
                <?xml version="1.0"?>
                <evil/>""");
        try {
            performTest();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Expected root element 'metadata' but found 'evil'"));
        }
    }

    @Disabled("XMvn always ignores invalid metadata files")
    @Test
    public void testMalformedXmlMetadata() throws Exception {
        addMd("<trololololo");
        try {
            performTest();
            fail("Expected exception to be thrown");
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("XML document structures must start and end within the same entity"));
        }
    }

    @Test
    public void testCompressedMetadata() throws Exception {
        String metadata = """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/ant-factory-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""";
        Path gzPath = br.resolve("usr/share/maven-metadata/main.xml");
        Files.createDirectories(gzPath.getParent());
        try (OutputStream os = Files.newOutputStream(gzPath); OutputStream zos = new GZIPOutputStream(os)) {
            zos.write(metadata.getBytes(StandardCharsets.UTF_8));
        }
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        performTest();
    }

    @Test
    public void testSingleNamespace() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/JPP.plexus-ant-factory-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/jsp-2.1-glassfish-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/JPP-jsp-2.1-glassfish-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:1.0) = 9.1.1.b60.25.p2");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:1.0) = 9.1.1.b60.25.p2");
        performTest();
    }

    @Test
    public void testMultiNamespace() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/JPP.plexus-ant-factory-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns2</namespace>
                            <path>/usr/share/java/jsp-2.1-glassfish-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns2</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/JPP-jsp-2.1-glassfish-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        expectProv("ns2-mvn(org.mortbay.jetty:jsp-2.1-glassfish:1.0) = 9.1.1.b60.25.p2");
        expectProv("ns2-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:1.0) = 9.1.1.b60.25.p2");
        performTest();
    }

    @Test
    public void testMultipleMetadata() throws Exception {
        addMd("md1", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory-1.0.jar</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/ant-factory-1.0.pom</path>
                            <compatVersions>
                                <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("md1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("md1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        addMd("md2", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/jsp-2.1-glassfish-1.0.jar</path>
                            <compatVersions>
                                <version>6.0.18</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.b60.25.p2</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/jsp-2.1-glassfish-1.0.pom</path>
                            <compatVersions>
                                <version>6.0.18</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("md2", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.b60.25.p2");
        expectProv("md2", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.b60.25.p2");
        performTest();
    }

    @Test
    public void testSystemVersion() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>4320148235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/ant-factory.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/JPP.plexus-ant-factory.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>
                """);
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory) = 1.0");
        expectProv("ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:) = 1.0");
        performTest();
    }

    @Test
    public void testExtensionWar() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <extension>war</extension>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                                <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/apache-maven.war</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:war:6.0.18) = 9.1.1.B60.25.p2");
        performTest();
    }

    @Test
    public void testExtensionJar() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                                <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/apache-maven.jar</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        performTest();
    }

    @Test
    public void testExtensionPom() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>6.0.18</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/jsp-2.1-glassfish.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:) = 6.0.18");
        performTest();
    }

    // Test for https://bugzilla.redhat.com/show_bug.cgi?id=1017271
    @Test
    public void testNamespaceRhbz1017271() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.apache.maven</groupId>
                            <artifactId>apache-maven</artifactId>
                            <version>3.1.1</version>
                            <extension>pom</extension>
                            <namespace>maven31</namespace>
                            <path>/usr/share/maven-poms/JPP-apache-maven.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("maven31-mvn(org.apache.maven:apache-maven:pom:) = 3.1.1");
        performTest();
    }

    @Test
    public void testCompatVersion() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.apache.maven</groupId>
                            <artifactId>apache-maven</artifactId>
                            <version>3.1.1</version>
                            <extension>pom</extension>
                            <compatVersions>
                                <version>3.1.1</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/JPP-apache-maven-3.1.1.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(org.apache.maven:apache-maven:pom:3.1.1) = 3.1.1");
        performTest();
    }

    @Test
    public void testAlias() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>jakarta-regexp</groupId>
                            <artifactId>jakarta-regexp</artifactId>
                            <version>1.0</version>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/jakarta-regexp.pom</path>
                            <aliases>
                                <alias>
                                    <groupId>regexp</groupId>
                                    <artifactId>regexp</artifactId>
                                    <extension>pom</extension>
                                </alias>
                            </aliases>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(jakarta-regexp:jakarta-regexp:pom:) = 1.0");
        expectProv("mvn(regexp:regexp:pom:) = 1.0");
        performTest();
    }

    @Test
    public void testAlias2() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>jakarta-regexp</groupId>
                            <artifactId>jakarta-regexp</artifactId>
                            <version>1.0</version>
                            <path>/usr/share/java/plexus/jakarta-regexp.jar</path>
                            <aliases>
                                <alias>
                                    <groupId>regexp</groupId>
                                    <artifactId>regexp</artifactId>
                                </alias>
                            </aliases>
                        </artifact>
                        <artifact>
                            <groupId>jakarta-regexp</groupId>
                            <artifactId>jakarta-regexp</artifactId>
                            <version>1.0</version>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/jakarta-regexp.pom</path>
                            <aliases>
                                <alias>
                                    <groupId>regexp</groupId>
                                    <artifactId>regexp</artifactId>
                                    <extension>pom</extension>
                                </alias>
                            </aliases>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(jakarta-regexp:jakarta-regexp:pom:) = 1.0");
        expectProv("mvn(jakarta-regexp:jakarta-regexp) = 1.0");
        expectProv("mvn(regexp:regexp:pom:) = 1.0");
        expectProv("mvn(regexp:regexp) = 1.0");
        performTest();
    }

    @Test
    public void testCompatAlias() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>jakarta-regexp</groupId>
                            <artifactId>jakarta-regexp</artifactId>
                            <version>1.0</version>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/jakarta-regexp.pom</path>
                            <compatVersions>
                                <version>1.1</version>
                                <version>1.1.1</version>
                            </compatVersions>
                            <aliases>
                                <alias>
                                    <groupId>regexp</groupId>
                                    <artifactId>regexp</artifactId>
                                    <extension>pom</extension>
                                </alias>
                            </aliases>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(jakarta-regexp:jakarta-regexp:pom:1.1) = 1.0");
        expectProv("mvn(jakarta-regexp:jakarta-regexp:pom:1.1.1) = 1.0");
        expectProv("mvn(regexp:regexp:pom:1.1) = 1.0");
        expectProv("mvn(regexp:regexp:pom:1.1.1) = 1.0");
        performTest();
    }

    @Test
    public void testExtensionJarExplicit() throws Exception {
        addMd("""
                <?xml version="1.0" ?>
                <ns1:metadata xmlns:ns1="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                   <ns1:artifacts>
                      <ns1:artifact>
                         <ns1:groupId>args4j</ns1:groupId>
                         <ns1:artifactId>args4j</ns1:artifactId>
                         <ns1:extension>pom</ns1:extension>
                         <ns1:version>2.0.16</ns1:version>
                         <ns1:path>/home/msrb/projects/javapackages/test/data/mvn_artifact/args4j.pom</ns1:path>
                         <ns1:properties>
                            <xmvn.resolver.disableEffectivePom>true</xmvn.resolver.disableEffectivePom>
                         </ns1:properties>
                      </ns1:artifact>
                      <ns1:artifact>
                         <ns1:groupId>args4j</ns1:groupId>
                         <ns1:artifactId>args4j</ns1:artifactId>
                         <ns1:extension>jar</ns1:extension>
                         <ns1:version>2.0.16</ns1:version>
                         <ns1:path>/home/msrb/projects/javapackages/java-utils/args4j.jar</ns1:path>
                         <ns1:properties>
                            <xmvn.resolver.disableEffectivePom>true</xmvn.resolver.disableEffectivePom>
                         </ns1:properties>
                      </ns1:artifact>
                   </ns1:artifacts>
                </ns1:metadata>""");
        expectProv("mvn(args4j:args4j:pom:) = 2.0.16");
        expectProv("mvn(args4j:args4j) = 2.0.16");
        performTest();
    }

    @Test
    public void testDashesInVersion() throws Exception {
        addMd("F", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>1-alpha-2</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                      <dependencies>
                        <dependency>
                          <groupId>org.sonatype.sisu</groupId>
                          <artifactId>sisu-guice</artifactId>
                          <requestedVersion>3.1.6</requestedVersion>
                          <classifier>no_aop</classifier>
                          <exclusions>
                            <exclusion>
                              <groupId>aopalliance</groupId>
                              <artifactId>aopalliance</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                      <properties>
                        <osgi.version>1.5.1-SNAPSHOT</osgi.version>
                        <osgi.id>osgi2</osgi.id>
                      </properties>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api) = 1.alpha.2");
        expectReq("F", "mvn(org.sonatype.sisu:sisu-guice::no_aop:)");
        addMd("R", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>1-alpha-2</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <dependencies>
                        <dependency>
                          <!-- this is a dependency on subpackage -->
                          <!-- mvn(org.apache.maven:maven-plugin-api) = 1.alpha.2 -->
                          <groupId>org.apache.maven</groupId>
                          <artifactId>maven-plugin-api</artifactId>
                          <requestedVersion>1-alpha-2</requestedVersion>
                        </dependency>
                      </dependencies>
                      <properties>
                        <osgi.version>1.5.101-SNAPSHOT</osgi.version>
                        <osgi.id>osgi1</osgi.id>
                        <osgi.requires>osgi2</osgi.requires>
                      </properties>
                    </artifact>
                  </artifacts>
                  <skippedArtifacts>
                  </skippedArtifacts>
                </metadata>""");
        expectProv("R", "mvn(org.apache.maven:maven-model) = 1.alpha.2");
        expectReq("R", "mvn(org.apache.maven:maven-plugin-api) = 1.alpha.2");
        performTest();
    }

    @Test
    public void testRequireSimple() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.apache.maven</groupId>
                                  <artifactId>maven-project</artifactId>
                                  <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/plexus-plexus-ant-factory.pom</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.apache.maven</groupId>
                                  <artifactId>maven-project</artifactId>
                                  <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        expectReq("mvn(org.apache.maven:maven-project)");
        performTest();
    }

    @Test
    public void testRequireParent() throws Exception {
        addMd("R1", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.codehaus.plexus</groupId>
                                    <artifactId>plexus-ant-factory</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/jsp-2.1-glassfish.pom</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.codehaus.plexus</groupId>
                                    <artifactId>plexus-ant-factory</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R1", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("R1", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectReq("R1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory)");
        addMd("R2", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R2", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        performTest();
    }

    @Test
    public void testRequireMulti() throws Exception {
        addMd("R0", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.codehaus.plexus</groupId>
                                    <artifactId>plexus-ant-factory</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                    <groupId>codehaus</groupId>
                                    <artifactId>plexus-utils</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.maven.wagon</groupId>
                                    <artifactId>wagon-provider-api</artifactId>
                                    <requestedVersion>1.1</requestedVersion>
                                    <classifier>test-jar</classifier>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.codehaus.plexus</groupId>
                                    <artifactId>plexus-ant-factory</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                    <groupId>codehaus</groupId>
                                    <artifactId>plexus-utils</artifactId>
                                    <namespace>ns</namespace>
                                    <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                    <groupId>org.apache.maven.wagon</groupId>
                                    <artifactId>wagon-provider-api</artifactId>
                                    <requestedVersion>1.1</requestedVersion>
                                    <classifier>test-jar</classifier>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R0", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("R0", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectReq("R0", "ns-mvn(org.codehaus.plexus:plexus-ant-factory)");
        expectReq("R0", "ns-mvn(codehaus:plexus-utils) = 1.2");
        expectReq("R0", "mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:)");
        addMd("R1", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/plexus-plexus-ant-factory.pom</path>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("R1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        addMd("R2", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <version>1.4</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                                <version>1.5</version>
                            </compatVersions>
                            <path>/usr/share/java/maven-idea-plugin.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <extension>pom</extension>
                            <version>1.4</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                                <version>1.5</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/maven-idea-plugin.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R2", "ns-mvn(org.apache.maven.plugins:maven-idea-plugin:1.5) = 1.4");
        expectProv("R2", "ns-mvn(org.apache.maven.plugins:maven-idea-plugin:pom:1.5) = 1.4");
        addMd("R3", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/plexus-utils.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <extension>pom</extension>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/maven-poms/plexus/plexus-utils.pom</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectReq("R3", "mvn(org.apache.maven:maven-project)");
        expectProv("R3", "ns-mvn(codehaus:plexus-utils) = 1.2");
        expectProv("R3", "ns-mvn(codehaus:plexus-utils:pom:) = 1.2");
        performTest();
    }

    @Test
    public void testRequireMultiNamespace() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns2</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns2</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns2</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("ns2-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("ns2-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectReq("ns2-mvn(codehaus:plexus-cipher)").times(3);
        expectReq("ns-mvn(codehaus:plexus-utils)").times(3);
        expectReq("mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:)").times(3);
        performTest();
    }

    @Test
    public void testRequireMultiVersioned() throws Exception {
        addMd("F1", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/plexus-plexus-ant-factory.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("F1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        addMd("F2", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <version>1.4</version>
                            <compatVersions>
                                <version>1.5</version>
                            </compatVersions>
                            <path>/usr/share/java/maven-idea-plugin.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <extension>pom</extension>
                            <version>1.4</version>
                            <compatVersions>
                                <version>1.5</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/maven-idea-plugin.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F2", "mvn(org.apache.maven.plugins:maven-idea-plugin:1.5) = 1.4");
        expectProv("F2", "mvn(org.apache.maven.plugins:maven-idea-plugin:pom:1.5) = 1.4");
        addMd("F3", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/plexus-utils.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <extension>pom</extension>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/maven-poms/plexus/plexus-utils.pom</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F3", "ns-mvn(codehaus:plexus-utils) = 1.2");
        expectProv("F3", "ns-mvn(codehaus:plexus-utils:pom:) = 1.2");
        expectReq("F3", "mvn(org.apache.maven:maven-project)");
        addMd("F4", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-cipher</artifactId>
                            <version>1.1</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/plexus-cipher-1.1.jar</path>
                            <compatVersions>
                              <version>1.0</version>
                              <version>1.1</version>
                            </compatVersions>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-cipher</artifactId>
                            <extension>pom</extension>
                            <version>1.1</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/maven-poms/plexus/plexus-cipher-1.1.pom</path>
                            <compatVersions>
                              <version>1.0</version>
                              <version>1.1</version>
                            </compatVersions>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F4", "ns-mvn(codehaus:plexus-cipher:1.0) = 1.1");
        expectProv("F4", "ns-mvn(codehaus:plexus-cipher:1.1) = 1.1");
        expectProv("F4", "ns-mvn(codehaus:plexus-cipher:pom:1.0) = 1.1");
        expectProv("F4", "ns-mvn(codehaus:plexus-cipher:pom:1.1) = 1.1");
        expectReq("F4", "mvn(org.apache.maven:maven-project)");
        addMd("RR", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <!-- ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0 -->
                                  <groupId>org.codehaus.plexus</groupId>
                                  <artifactId>plexus-ant-factory</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                  <resolvedVersion>1.0</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- ns-mvn(codehaus:plexus-utils:1.2) -->
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                  <resolvedVersion>1.2</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- ns-mvn(codehaus:plexus-cipher:1.0) = 1.1 -->
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                  <resolvedVersion>1.0</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:) -->
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <!-- mvn(org.apache.maven.plugins:maven-idea-plugin:1.5) = 1.4 -->
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-idea-plugin</artifactId>
                                  <requestedVersion>1.5</requestedVersion>
                                  <resolvedVersion>1.5</resolvedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish.jar</path>
                            <dependencies>
                                <dependency>
                                  <!-- ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0 -->
                                  <groupId>org.codehaus.plexus</groupId>
                                  <artifactId>plexus-ant-factory</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                  <resolvedVersion>1.0</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- ns-mvn(codehaus:plexus-utils:1.2) -->
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                  <resolvedVersion>1.2</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- ns-mvn(codehaus:plexus-cipher:1.0) = 1.1 -->
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-cipher</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                  <resolvedVersion>1.0</resolvedVersion>
                                </dependency>
                                <dependency>
                                  <!-- mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:) -->
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <!-- mvn(org.apache.maven.plugins:maven-idea-plugin:1.5) = 1.4 -->
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>maven-idea-plugin</artifactId>
                                  <requestedVersion>1.5</requestedVersion>
                                  <resolvedVersion>1.5</resolvedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("RR", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("RR", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectReq("RR", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectReq("RR", "ns-mvn(codehaus:plexus-utils:1.2)");
        expectReq("RR", "ns-mvn(codehaus:plexus-cipher:1.0) = 1.1");
        expectReq("RR", "mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:)");
        expectReq("RR", "mvn(org.apache.maven.plugins:maven-idea-plugin:1.5) = 1.4");
        performTest();
    }

    @Test
    public void testMixed() throws Exception {
        addMd("F1", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <namespace>ns</namespace>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/plexus-plexus-ant-factory.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("F1", "ns-mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        addMd("F2", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <version>1.4</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/maven-idea-plugin.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.apache.maven.plugins</groupId>
                            <artifactId>maven-idea-plugin</artifactId>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <version>1.4</version>
                            <path>/usr/share/maven-poms/maven-idea-plugin.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F2", "ns-mvn(org.apache.maven.plugins:maven-idea-plugin) = 1.4");
        expectProv("F2", "ns-mvn(org.apache.maven.plugins:maven-idea-plugin:pom:) = 1.4");
        addMd("F3", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/plexus/plexus-utils.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>codehaus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <extension>pom</extension>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/maven-poms/plexus/plexus-utils.pom</path>
                            <dependencies>
                                <dependency>
                                    <groupId>org.apache.maven</groupId>
                                    <artifactId>maven-project</artifactId>
                                    <requestedVersion>2.2.1</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("F3", "ns-mvn(codehaus:plexus-utils) = 1.2");
        expectProv("F3", "ns-mvn(codehaus:plexus-utils:pom:) = 1.2");
        expectReq("F3", "mvn(org.apache.maven:maven-project)");
        addMd("RR", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/java/jetty/jsp-2.1-glassfish-6.0.18.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.codehaus.plexus</groupId>
                                  <artifactId>plexus-ant-factory</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>some-maven-plugin</artifactId>
                                  <requestedVersion>1.5</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.mortbay.jetty</groupId>
                            <artifactId>jsp-2.1-glassfish</artifactId>
                            <version>9.1.1.B60.25.p2</version>
                            <extension>pom</extension>
                            <namespace>ns</namespace>
                            <compatVersions>
                              <version>6.0.18</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/jetty/jsp-2.1-glassfish-6.0.18.pom</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.codehaus.plexus</groupId>
                                  <artifactId>plexus-ant-factory</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>codehaus</groupId>
                                  <artifactId>plexus-utils</artifactId>
                                  <namespace>ns</namespace>
                                  <requestedVersion>1.2</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.wagon</groupId>
                                  <artifactId>wagon-provider-api</artifactId>
                                  <classifier>test-jar</classifier>
                                  <requestedVersion>1.0</requestedVersion>
                                </dependency>
                                <dependency>
                                  <groupId>org.apache.maven.plugins</groupId>
                                  <artifactId>some-maven-plugin</artifactId>
                                  <requestedVersion>1.5</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("RR", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:6.0.18) = 9.1.1.B60.25.p2");
        expectProv("RR", "ns-mvn(org.mortbay.jetty:jsp-2.1-glassfish:pom:6.0.18) = 9.1.1.B60.25.p2");
        expectReq("RR", "ns-mvn(org.codehaus.plexus:plexus-ant-factory)");
        expectReq("RR", "ns-mvn(codehaus:plexus-utils) = 1.2");
        expectReq("RR", "mvn(org.apache.maven.wagon:wagon-provider-api::test-jar:)");
        expectReq("RR", "mvn(org.apache.maven.plugins:some-maven-plugin)");
        performTest();
    }

    @Test
    public void testSimpleSubpackage() throws Exception {
        addMd("F", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                      <dependencies>
                        <dependency>
                          <groupId>org.sonatype.sisu</groupId>
                          <artifactId>sisu-guice</artifactId>
                          <requestedVersion>3.1.6</requestedVersion>
                          <exclusions>
                            <exclusion>
                              <groupId>aopalliance</groupId>
                              <artifactId>aopalliance</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api) = 3.2.1");
        expectReq("F", "mvn(org.sonatype.sisu:sisu-guice)");
        addMd("R", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <dependencies>
                        <dependency>
                            <!-- this is a dependency on subpackage -->
                            <!-- mvn(org.apache.maven:maven-plugin-api) = 3.2.1 -->
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-plugin-api</artifactId>
                            <requestedVersion>3.2.1</requestedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                  <skippedArtifacts>
                  </skippedArtifacts>
                </metadata>""");
        expectProv("R", "mvn(org.apache.maven:maven-model) = 3.2.1");
        expectReq("R", "mvn(org.apache.maven:maven-plugin-api) = 3.2.1");
        performTest();
    }

    @Test
    public void testSimpleSubpackage2() throws Exception {
        addMd("F", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                      <dependencies>
                        <dependency>
                          <groupId>org.sonatype.sisu</groupId>
                          <artifactId>sisu-guice</artifactId>
                          <requestedVersion>3.1.6</requestedVersion>
                          <exclusions>
                            <exclusion>
                              <groupId>aopalliance</groupId>
                              <artifactId>aopalliance</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api) = 3.2.1");
        expectReq("F", "mvn(org.sonatype.sisu:sisu-guice)");
        addMd("R", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <dependencies>
                        <dependency>
                            <!-- this is a dependency on subpackage -->
                            <!-- mvn(org.apache.maven:maven-plugin-api) = 3.2.1 -->
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-plugin-api</artifactId>
                            <requestedVersion>3.2.1</requestedVersion>
                        </dependency>
                        <dependency>
                            <!-- this is a regular dependency -->
                            <!-- mvn(org.codehaus.plexus:plexus-utils) -->
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-utils</artifactId>
                            <requestedVersion>3.0.16</requestedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                  <skippedArtifacts>
                  </skippedArtifacts>
                </metadata>""");
        expectProv("R", "mvn(org.apache.maven:maven-model) = 3.2.1");
        expectReq("R", "mvn(org.apache.maven:maven-plugin-api) = 3.2.1");
        expectReq("R", "mvn(org.codehaus.plexus:plexus-utils)");
        performTest();
    }

    @Test
    public void testSimpleSubpackage3() throws Exception {
        addMd("F", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                      <compatVersions>
                          <version>3.2.1</version>
                          <version>3.2.0</version>
                      </compatVersions>
                      <dependencies>
                        <dependency>
                          <groupId>org.sonatype.sisu</groupId>
                          <artifactId>sisu-guice</artifactId>
                          <requestedVersion>3.1.6</requestedVersion>
                          <exclusions>
                            <exclusion>
                              <groupId>aopalliance</groupId>
                              <artifactId>aopalliance</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api:3.2.0) = 3.2.1");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api:3.2.1) = 3.2.1");
        expectReq("F", "mvn(org.sonatype.sisu:sisu-guice)");
        addMd("R", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <compatVersions>
                          <version>3.2.1</version>
                          <version>3.2.0</version>
                      </compatVersions>
                      <dependencies>
                        <dependency>
                            <!-- this is a dependency on (compat) subpackage -->
                            <!-- mvn(org.apache.maven:maven-plugin-api:3.2.0) = 3.2.1 -->
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-plugin-api</artifactId>
                            <requestedVersion>3.2.1</requestedVersion>
                            <resolvedVersion>3.2.0</resolvedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                  <skippedArtifacts>
                  </skippedArtifacts>
                </metadata>""");
        expectProv("R", "mvn(org.apache.maven:maven-model:3.2.0) = 3.2.1");
        expectProv("R", "mvn(org.apache.maven:maven-model:3.2.1) = 3.2.1");
        expectReq("R", "mvn(org.apache.maven:maven-plugin-api:3.2.0) = 3.2.1");
        performTest();
    }

    @Test
    public void testSimpleSubpackage4() throws Exception {
        addMd("F", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                      <compatVersions>
                          <version>3.2.1</version>
                          <version>3.2.0</version>
                      </compatVersions>
                      <dependencies>
                        <dependency>
                          <groupId>org.sonatype.sisu</groupId>
                          <artifactId>sisu-guice</artifactId>
                          <requestedVersion>3.1.6</requestedVersion>
                          <exclusions>
                            <exclusion>
                              <groupId>aopalliance</groupId>
                              <artifactId>aopalliance</artifactId>
                            </exclusion>
                          </exclusions>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api:3.2.0) = 3.2.1");
        expectProv("F", "mvn(org.apache.maven:maven-plugin-api:3.2.1) = 3.2.1");
        expectReq("F", "mvn(org.sonatype.sisu:sisu-guice)");
        addMd("R", """
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <compatVersions>
                          <version>3.2.1</version>
                          <version>3.2.0</version>
                      </compatVersions>
                      <dependencies>
                        <dependency>
                            <!-- this is a dependency on artifact with same gId:aId as subpackage has,
                                 but subpackage is a compat version and this dependency isn't -->
                            <!-- mvn(org.apache.maven:maven-plugin-api) -->
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-plugin-api</artifactId>
                            <requestedVersion>2.0</requestedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                  <skippedArtifacts>
                  </skippedArtifacts>
                </metadata>""");
        expectProv("R", "mvn(org.apache.maven:maven-model:3.2.0) = 3.2.1");
        expectProv("R", "mvn(org.apache.maven:maven-model:3.2.1) = 3.2.1");
        expectReq("R", "mvn(org.apache.maven:maven-plugin-api)");
        performTest();
    }

    @Test
    public void testSelfArtifact() throws Exception {
        addMd("""
                <?xml version="1.0" encoding="UTF-8"?>
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                  <artifacts>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-model</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-model.jar</path>
                      <dependencies>
                        <dependency>
                            <!-- this is a dependency on artifact from same package -->
                            <!-- no requires should be generated -->
                            <groupId>org.apache.maven</groupId>
                            <artifactId>maven-plugin-api</artifactId>
                            <requestedVersion>3.2.1</requestedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                    <artifact>
                      <groupId>org.apache.maven</groupId>
                      <artifactId>maven-plugin-api</artifactId>
                      <version>3.2.1</version>
                      <path>/usr/share/java/maven/maven-plugin-api.jar</path>
                    </artifact>
                  </artifacts>
                </metadata>""");
        expectProv("mvn(org.apache.maven:maven-model) = 3.2.1");
        expectProv("mvn(org.apache.maven:maven-plugin-api) = 3.2.1");
        performTest();
    }

    // https://bugzilla.redhat.com/show_bug.cgi?id=1012980
    @Test
    public void testRequireSkippedRhbz1012980() throws Exception {
        addMd("R", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>g</groupId>
                            <artifactId>a1</artifactId>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/a1.jar</path>
                            <dependencies>
                                <dependency>
                                    <groupId>g</groupId>
                                    <artifactId>skipped</artifactId>
                                    <requestedVersion>UNKNOWN</requestedVersion>
                                </dependency>
                                <dependency>
                                    <groupId>g</groupId>
                                    <artifactId>a2</artifactId>
                                    <requestedVersion>1.2</requestedVersion>
                                    <namespace>ns</namespace>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("R", "ns-mvn(g:a1) = 1.2");
        expectReq("R", "ns-mvn(g:a2) = 1.2");
        EasyMock.expect(context.eval("%{error:Dependency on skipped artifact: g:skipped:jar:UNKNOWN}")).andReturn(null);
        addMd("S", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235933</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>g</groupId>
                            <artifactId>a2</artifactId>
                            <version>1.2</version>
                            <namespace>ns</namespace>
                            <path>/usr/share/java/a2.jar</path>
                        </artifact>
                    </artifacts>
                    <skippedArtifacts>
                        <skippedArtifact>
                            <groupId>g</groupId>
                            <artifactId>skipped</artifactId>
                        </skippedArtifact>
                    </skippedArtifacts>
                </metadata>""");
        expectProv("S", "ns-mvn(g:a2) = 1.2");
        performTest();
    }

    // https://bugzilla.redhat.com/show_bug.cgi?id=1017701#c2
    @Test
    public void testRhbz1017701() throws Exception {
        addMd("API", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.0.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.eclipse.aether</groupId>
                            <artifactId>aether-api</artifactId>
                            <version>0.9.0.M3</version>
                            <namespace>maven31</namespace>
                            <path>/usr/share/java/aether/aether-api.jar</path>
                        </artifact>
                        <artifact>
                            <groupId>org.eclipse.aether</groupId>
                            <artifactId>aether-api</artifactId>
                            <version>0.9.0.M3</version>
                            <namespace>maven31</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/aether-aether-api.pom</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("API", "maven31-mvn(org.eclipse.aether:aether-api) = 0.9.0.M3");
        expectProv("API", "maven31-mvn(org.eclipse.aether:aether-api:pom:) = 0.9.0.M3");
        addMd("SPI", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.0.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.eclipse.aether</groupId>
                            <artifactId>aether-spi</artifactId>
                            <version>0.9.0.M3</version>
                            <namespace>maven31</namespace>
                            <path>/usr/share/java/aether/aether-spi.jar</path>
                            <dependencies>
                                <dependency>
                                    <namespace>maven31</namespace>
                                    <groupId>org.eclipse.aether</groupId>
                                    <artifactId>aether-api</artifactId>
                                    <requestedVersion>0.9.0.M3</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.eclipse.aether</groupId>
                            <artifactId>aether-spi</artifactId>
                            <version>0.9.0.M3</version>
                            <namespace>maven31</namespace>
                            <extension>pom</extension>
                            <path>/usr/share/maven-poms/aether-aether-spi.pom</path>
                            <dependencies>
                                <dependency>
                                    <namespace>maven31</namespace>
                                    <groupId>org.eclipse.aether</groupId>
                                    <artifactId>aether-api</artifactId>
                                    <requestedVersion>0.9.0.M3</requestedVersion>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("SPI", "maven31-mvn(org.eclipse.aether:aether-spi) = 0.9.0.M3");
        expectProv("SPI", "maven31-mvn(org.eclipse.aether:aether-spi:pom:) = 0.9.0.M3");
        expectReq("SPI", "maven31-mvn(org.eclipse.aether:aether-api) = 0.9.0.M3");
        performTest();
    }

    @Test
    public void testUnknownDep() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/java/plexus/plexus-ant-factory.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.apache.maven</groupId>
                                  <artifactId>maven-project</artifactId>
                                  <requestedVersion>2.2.1</requestedVersion>
                                  <resolvedVersion>UNKNOWN</resolvedVersion>
                                  <namespace>UNKNOWN</namespace>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>org.codehaus.plexus</groupId>
                            <artifactId>plexus-ant-factory</artifactId>
                            <version>1.0</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/plexus-plexus-ant-factory.pom</path>
                            <dependencies>
                                <dependency>
                                  <groupId>org.apache.maven</groupId>
                                  <artifactId>maven-project</artifactId>
                                  <requestedVersion>2.2.1</requestedVersion>
                                  <resolvedVersion>UNKNOWN</resolvedVersion>
                                  <namespace>UNKNOWN</namespace>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(org.codehaus.plexus:plexus-ant-factory:1.0) = 1.0");
        expectProv("mvn(org.codehaus.plexus:plexus-ant-factory:pom:1.0) = 1.0");
        EasyMock.expect(
                context.eval("%{error:Dependency on unresolved artifact: org.apache.maven:maven-project:jar:2.2.1}"))
                .andReturn(null);
        performTest();
    }

    @Test
    public void testOptionalDep() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>g</groupId>
                            <artifactId>a</artifactId>
                            <version>1</version>
                            <path>/usr/share/java/a.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>dg</groupId>
                                  <artifactId>da</artifactId>
                                  <requestedVersion>2</requestedVersion>
                                  <optional>true</optional>
                                </dependency>
                            </dependencies>
                        </artifact>
                        <artifact>
                            <groupId>g</groupId>
                            <artifactId>b</artifactId>
                            <version>1</version>
                            <path>/usr/share/java/a.jar</path>
                            <dependencies>
                                <dependency>
                                  <groupId>dg</groupId>
                                  <artifactId>db</artifactId>
                                  <requestedVersion>2</requestedVersion>
                                  <optional>false</optional>
                                </dependency>
                            </dependencies>
                        </artifact>
                    </artifacts>
                </metadata>""");
        expectProv("mvn(g:a) = 1");
        expectProv("mvn(g:b) = 1");
        expectReq("mvn(dg:db)");
        performTest();
    }

    @Test
    public void testPomDeps() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>org.kohsuke</groupId>
                            <artifactId>pom</artifactId>
                            <version>8</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/pom.xml</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/pom.xml", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.kohsuke</groupId>
                  <artifactId>pom</artifactId>
                  <version>8</version>
                  <packaging>pom</packaging>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.fedoraproject.xmvn</groupId>
                        <artifactId>xmvn-core</artifactId>
                      </plugin>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                        <configuration>
                          <source>1.5</source>
                          <target>1.5</target>
                         </configuration>
                      </plugin>
                      <plugin>
                        <artifactId>maven-deploy-plugin</artifactId>
                      </plugin>
                    </plugins>
                    <extensions>
                      <extension>
                        <groupId>org.fedoraproject.xmvn</groupId>
                        <artifactId>xmvn-api</artifactId>
                        <version>2.0.0</version>
                      </extension>
                    </extensions>
                  </build>
                </project>""");
        mockResolver("org.fedoraproject.xmvn:xmvn-core", "found", null, null);
        mockResolver("org.apache.maven.plugins:maven-compiler-plugin", "found", null, null);
        mockResolver("org.apache.maven.plugins:maven-deploy-plugin", null, null, null);
        mockResolver("org.fedoraproject.xmvn:xmvn-api:2.0.0", "found", null, null);
        expectProv("mvn(org.kohsuke:pom:pom:1.0) = 8");
        expectReq("mvn(org.fedoraproject.xmvn:xmvn-core)");
        expectReq("mvn(org.apache.maven.plugins:maven-compiler-plugin)");
        expectReq("mvn(org.fedoraproject.xmvn:xmvn-api)");
        performTest();
    }

    @Test
    public void testPomDepParent() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>gid</groupId>
                            <artifactId>aid</artifactId>
                            <version>1.1</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/pom.xml</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/pom.xml", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <artifactId>parent-pom</artifactId>
                  </parent>
                  <groupId>gid</groupId>
                  <artifactId>aid</artifactId>
                  <version>1.1</version>
                  <packaging>pom</packaging>
                </project>""");
        mockResolver("gid:parent-pom:pom:1.1", "/something", "nss", "1.2.3");
        expectProv("mvn(gid:aid:pom:1.0) = 1.1");
        expectReq("nss-mvn(gid:parent-pom:pom:1.2.3)");
        performTest();
    }

    @Test
    public void testPomDepsNonPomPackaging() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>gid</groupId>
                            <artifactId>aid</artifactId>
                            <version>1.1</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/pom.xml</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/pom.xml", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <artifactId>parent-pom</artifactId>
                  </parent>
                  <groupId>gid</groupId>
                  <artifactId>aid</artifactId>
                  <version>1.1</version>
                  <packaging>war</packaging>
                </project>""");
        expectProv("mvn(gid:aid:pom:1.0) = 1.1");
        performTest();
    }

    @Test
    public void testPomDepsWithParent() throws Exception {
        addMd("""
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>gid</groupId>
                            <artifactId>aid</artifactId>
                            <version>1.1</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/pom.xml</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/pom.xml", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>ppom</groupId>
                    <artifactId>parent-pom</artifactId>
                    <version>2</version>
                  </parent>
                  <groupId>gid</groupId>
                  <artifactId>aid</artifactId>
                  <version>1.1</version>
                  <packaging>pom</packaging>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>plugin</groupId>
                        <artifactId>external</artifactId>
                      </plugin>
                    </plugins>
                  </build>
                </project>""");
        mockResolver("ppom:parent-pom:pom:2", "/something", "nss", "1.2.3");
        mockResolver("plugin:external", "/something-else", null, null);
        expectProv("mvn(gid:aid:pom:1.0) = 1.1");
        expectReq("mvn(plugin:external)");
        expectReq("nss-mvn(ppom:parent-pom:pom:1.2.3)");
        performTest();
    }

    @Test
    public void testPomDepsSubpackage() throws Exception {
        addMd("""
                <!-- POM-only metadata -->
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>gid</groupId>
                            <artifactId>aid</artifactId>
                            <version>1.1</version>
                            <extension>pom</extension>
                            <compatVersions>
                              <version>1.0</version>
                            </compatVersions>
                            <path>/usr/share/maven-poms/pom.xml</path>
                        </artifact>
                    </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/pom.xml", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>ppom</groupId>
                    <artifactId>parent-pom</artifactId>
                    <version>2.0</version>
                  </parent>
                  <groupId>gid</groupId>
                  <artifactId>aid</artifactId>
                  <version>1.1</version>
                  <packaging>pom</packaging>
                  <build>
                    <extensions>
                      <extension>
                        <groupId>extension</groupId>
                        <artifactId>from-subpackage</artifactId>
                        <version>1.1</version>
                      </extension>
                    </extensions>
                  </build>
                </project>
                """);
        mockResolver("ppom:parent-pom:pom:2.0", "pom-path", null, null);
        expectProv("mvn(gid:aid:pom:1.0) = 1.1");
        expectReq("mvn(extension:from-subpackage) = 1.1");
        expectReq("mvn(ppom:parent-pom:pom:)");
        addMd("sub", """
                <metadata xmlns="http://fedorahosted.org/xmvn/METADATA/2.3.0">
                    <uuid>432048235932</uuid>
                    <artifacts>
                        <artifact>
                            <groupId>extension</groupId>
                            <artifactId>from-subpackage</artifactId>
                            <version>1.1</version>
                            <extension>jar</extension>
                            <path>/usr/share/java/e.jar</path>
                        </artifact>
                    </artifacts>
                </metadata>
                """);
        expectProv("sub", "mvn(extension:from-subpackage) = 1.1");
        performTest();
    }

    @Test
    public void testPomSelfRequires() throws Exception {
        addMd("""
                <metadata>
                  <artifacts>
                    <artifact>
                      <groupId>org.apache</groupId>
                      <artifactId>apache</artifactId>
                      <extension>pom</extension>
                      <version>33</version>
                      <path>/usr/share/maven-poms/apache-parent/apache.pom</path>
                    </artifact>
                    <artifact>
                      <groupId>org.apache</groupId>
                      <artifactId>docs</artifactId>
                      <extension>pom</extension>
                      <version>33</version>
                      <path>/usr/share/maven-poms/apache-parent/docs.pom</path>
                    </artifact>
                  </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/apache-parent/apache.pom", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>org.apache</groupId>
                  <artifactId>apache</artifactId>
                  <version>33</version>
                  <packaging>pom</packaging>
                </project>""");
        addBrFile("usr/share/maven-poms/apache-parent/docs.pom", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.apache</groupId>
                    <artifactId>apache</artifactId>
                    <version>33</version>
                  </parent>
                  <artifactId>docs</artifactId>
                  <packaging>pom</packaging>
                </project>""");
        expectProv("mvn(org.apache:apache:pom:) = 33");
        expectProv("mvn(org.apache:docs:pom:) = 33");
        performTest();
    }

    @Test
    public void testPomRequiresJar() throws Exception {
        addMd("""
                <metadata>
                  <artifacts>
                    <artifact>
                      <groupId>com.google.inject.extensions</groupId>
                      <artifactId>extensions-parent</artifactId>
                      <extension>pom</extension>
                      <version>5.1.0</version>
                      <path>/usr/share/maven-poms/child.pom</path>
                      <dependencies>
                        <dependency>
                          <groupId>com.google.inject</groupId>
                          <artifactId>guice</artifactId>
                          <requestedVersion>5.1.0</requestedVersion>
                        </dependency>
                      </dependencies>
                    </artifact>
                  </artifacts>
                </metadata>""");
        addBrFile("usr/share/maven-poms/child.pom", """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <packaging>pom</packaging>
                  <groupId>com.google.inject.extensions</groupId>
                  <artifactId>extensions-parent</artifactId>
                  <version>5.1.0</version>
                </project>""");
        expectProv("mvn(com.google.inject.extensions:extensions-parent:pom:) = 5.1.0");
        expectReq("mvn(com.google.inject:guice)");
        performTest();
    }
}
