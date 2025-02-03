/*-
 * Copyright (c) 2024 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fedoraproject.xmvn.generator.maven;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.maven.model.Extension;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.fedoraproject.xmvn.artifact.Artifact;
import org.fedoraproject.xmvn.artifact.DefaultArtifact;
import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.logging.Logger;
import org.fedoraproject.xmvn.locator.ServiceLocator;
import org.fedoraproject.xmvn.locator.ServiceLocatorFactory;
import org.fedoraproject.xmvn.metadata.ArtifactAlias;
import org.fedoraproject.xmvn.metadata.ArtifactMetadata;
import org.fedoraproject.xmvn.metadata.Dependency;
import org.fedoraproject.xmvn.metadata.MetadataRequest;
import org.fedoraproject.xmvn.metadata.MetadataResolver;
import org.fedoraproject.xmvn.metadata.SkippedArtifactMetadata;
import org.fedoraproject.xmvn.resolver.ResolutionRequest;
import org.fedoraproject.xmvn.resolver.ResolutionResult;
import org.fedoraproject.xmvn.resolver.Resolver;

class Subpackage {
    final Path path;
    boolean pomOnly = true;

    Subpackage(Path path) {
        this.path = path;
    }
}

class UniqueArtifact {
    final Subpackage pkg;
    final ArtifactMetadata amd;
    final String rpmVersion;
    final String namespace;
    final Set<Artifact> artifacts = new LinkedHashSet<>();
    boolean pom = true;

    UniqueArtifact(Subpackage pkg, ArtifactMetadata amd) {
        this.pkg = pkg;
        this.amd = amd;
        this.rpmVersion = amd.getVersion().replace('-', '.');
        this.namespace = amd.getNamespace();
    }
}

class MavenGenerator implements Generator {
    private final BuildContext context;
    private final MetadataResolver metadataResolver;
    private final Resolver resolver;

    MavenGenerator(BuildContext context, MetadataResolver metadataResolver, Resolver resolver) {
        this.context = context;
        this.metadataResolver = metadataResolver;
        this.resolver = resolver;
    }

    public MavenGenerator(BuildContext context) {
        ServiceLocator serviceLocator = new ServiceLocatorFactory().createServiceLocator();
        this.context = context;
        this.metadataResolver = serviceLocator.getService(MetadataResolver.class);
        this.resolver = serviceLocator.getService(Resolver.class);
    }

    private String formatDep(Artifact art, String pkgver, String ns) {
        boolean cusExt = !art.getExtension().equals(Artifact.DEFAULT_EXTENSION);
        boolean cusCla = !art.getClassifier().equals("");
        boolean cusVer = !art.getVersion().equals(Artifact.DEFAULT_VERSION);
        StringBuilder sb = new StringBuilder();
        if (ns != null && !ns.isBlank()) {
            sb.append(ns);
            sb.append("-");
        }
        sb.append("mvn(");
        sb.append(art.getGroupId());
        sb.append(":");
        sb.append(art.getArtifactId());
        if (cusCla || cusExt) {
            sb.append(":");
        }
        if (cusExt) {
            sb.append(art.getExtension());
        }
        if (cusCla) {
            sb.append(":");
            sb.append(art.getClassifier());
        }
        if (cusCla || cusExt || cusVer) {
            sb.append(":");
        }
        if (cusVer) {
            sb.append(art.getVersion());
        }
        sb.append(")");
        if (pkgver != null) {
            sb.append(" = ");
            sb.append(pkgver);
        }
        return sb.toString();
    }

    private void error(String msg) {
        context.eval("%{error:" + msg + "}");
    }

    private String coalesce(String... strings) {
        for (String s : strings) {
            if (s != null && !s.isEmpty()) {
                return s;
            }
        }
        return null;
    }

    private String resolveDep(
            Artifact dep, Map<Artifact, List<UniqueArtifact>> myArtifacts, Subpackage pmd) {
        for (Artifact depa : List.of(dep, dep.setVersion(Artifact.DEFAULT_VERSION))) {
            List<UniqueArtifact> umds = myArtifacts.get(depa);
            if (umds != null) {
                UniqueArtifact umd = umds.getFirst();
                if (umd.pkg.path.equals(pmd.path)) {
                    // Self require
                    return null;
                }
                return formatDep(depa, umd.rpmVersion, umd.namespace);
            }
        }
        ResolutionRequest req = new ResolutionRequest(dep);
        ResolutionResult res = resolver.resolve(req);
        if (res.getArtifactPath() == null) {
            return null;
        }
        String ns = res.getNamespace();
        String cver = res.getCompatVersion();
        Artifact depa = dep.setVersion(cver != null ? cver : Artifact.DEFAULT_VERSION);
        return formatDep(depa, null, ns);
    }

    @Override
    public void generate(Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        Path prefix = buildRoot.resolve("usr/share/maven-metadata");
        Map<Artifact, List<UniqueArtifact>> myArtifacts = new LinkedHashMap<>();
        Set<Artifact> skipped = new LinkedHashSet<>();
        List<UniqueArtifact> umds = new ArrayList<>();
        if (Files.isDirectory(prefix)) {
            MetadataRequest mdReq = new MetadataRequest(List.of(prefix.toString()));
            metadataResolver
                    .resolveMetadata(mdReq)
                    .getPackageMetadataMap()
                    .forEach(
                            (filePath, pmd) -> {
                                Subpackage md = new Subpackage(filePath);
                                for (ArtifactMetadata amd : pmd.getArtifacts()) {
                                    UniqueArtifact umd = new UniqueArtifact(md, amd);
                                    umds.add(umd);
                                    List<Artifact> arts = new ArrayList<>();
                                    arts.add(amd.toArtifact());
                                    for (ArtifactAlias alias : amd.getAliases()) {
                                        arts.add(
                                                new DefaultArtifact(
                                                        alias.getGroupId(),
                                                        alias.getArtifactId(),
                                                        alias.getExtension(),
                                                        alias.getClassifier(),
                                                        Artifact.DEFAULT_VERSION));
                                    }
                                    umd.pom = true;
                                    for (Artifact art : arts) {
                                        if (!"pom".equals(art.getExtension())) {
                                            umd.pom = false;
                                        }
                                        if (amd.getCompatVersions().isEmpty()) {
                                            umd.artifacts.add(
                                                    art.setVersion(Artifact.DEFAULT_VERSION));
                                        } else {
                                            for (String ver : amd.getCompatVersions()) {
                                                umd.artifacts.add(art.setVersion(ver));
                                            }
                                        }
                                    }
                                    md.pomOnly &= umd.pom;
                                    for (Artifact vart : umd.artifacts) {
                                        myArtifacts
                                                .computeIfAbsent(vart, x -> new ArrayList<>())
                                                .add(umd);
                                    }
                                }
                                for (SkippedArtifactMetadata smd : pmd.getSkippedArtifacts()) {
                                    Artifact sart =
                                            new DefaultArtifact(
                                                    smd.getGroupId(),
                                                    smd.getArtifactId(),
                                                    smd.getExtension(),
                                                    smd.getClassifier(),
                                                    Artifact.DEFAULT_VERSION);
                                    skipped.add(sart);
                                }
                            });
        }
        for (UniqueArtifact umd : umds) {
            for (Artifact art : umd.artifacts) {
                collector.addProvides(umd.pkg.path, formatDep(art, umd.rpmVersion, umd.namespace));
            }
            if (umd.pkg.pomOnly) {
                Path pomPath =
                        buildRoot.resolve(Path.of("/").relativize(Path.of(umd.amd.getPath())));
                MavenXpp3Reader pomReader = new MavenXpp3Reader();
                try (Reader reader = Files.newBufferedReader(pomPath)) {
                    Model pom = pomReader.read(reader);
                    if (!"pom".equals(pom.getPackaging())) {
                        continue;
                    }
                    if (pom.getParent() != null) {
                        String pgid = coalesce(pom.getParent().getGroupId(), pom.getGroupId());
                        String paid = pom.getParent().getArtifactId();
                        String pver = coalesce(pom.getParent().getVersion(), pom.getVersion());
                        if (pgid != null && paid != null && pver != null) {
                            String req =
                                    resolveDep(
                                            new DefaultArtifact(pgid, paid, "pom", pver),
                                            myArtifacts,
                                            umd.pkg);
                            if (req != null) {
                                collector.addRequires(umd.pkg.path, req);
                            }
                        }
                    }
                    if (pom.getBuild() != null) {
                        for (Plugin plugin : pom.getBuild().getPlugins()) {
                            // XXX naive approach, plugin management is not supported
                            String pgid = coalesce(plugin.getGroupId(), "org.apache.maven.plugins");
                            String paid = plugin.getArtifactId();
                            String pver = coalesce(plugin.getVersion(), Artifact.DEFAULT_VERSION);
                            if (paid != null) {
                                String req =
                                        resolveDep(
                                                new DefaultArtifact(pgid, paid, pver),
                                                myArtifacts,
                                                umd.pkg);
                                if (req != null) {
                                    collector.addRequires(umd.pkg.path, req);
                                }
                            }
                        }
                        for (Extension ext : pom.getBuild().getExtensions()) {
                            String egid = ext.getGroupId();
                            String eaid = ext.getArtifactId();
                            String ever = coalesce(ext.getVersion(), Artifact.DEFAULT_VERSION);
                            if (egid != null && eaid != null) {
                                String req =
                                        resolveDep(
                                                new DefaultArtifact(egid, eaid, ever),
                                                myArtifacts,
                                                umd.pkg);
                                if (req != null) {
                                    collector.addRequires(umd.pkg.path, req);
                                }
                            }
                        }
                    }
                } catch (IOException | XmlPullParserException e) {
                    try (StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw)) {
                        sw.append("Unable to generate POM dependencies: ");
                        e.printStackTrace(pw);
                        Logger.debug(sw.toString());
                    } catch (IOException e1) {
                        throw new UncheckedIOException(e1);
                    }
                }
            }
            if (umd.pom && !umd.pkg.pomOnly) {
                continue;
            }
            for (Dependency dep : umd.amd.getDependencies()) {
                if (!dep.isOptional()) {
                    Artifact depa = dep.toArtifact();
                    if ("UNKNOWN".equals(dep.getResolvedVersion())) {
                        // XXX improve error message
                        error("Dependency on unresolved artifact: " + depa);
                        continue;
                    }
                    Artifact rdepa = depa.setVersion(dep.getResolvedVersion());
                    String ver = null;
                    List<UniqueArtifact> depmds = myArtifacts.getOrDefault(rdepa, List.of());
                    if (!depmds.isEmpty()) {
                        ver = depmds.getFirst().rpmVersion;
                    } else if (skipped.contains(rdepa)) {
                        // XXX improve error message
                        error("Dependency on skipped artifact: " + depa);
                        continue;
                    }
                    if (depmds.stream().map(x -> x.pkg.path).noneMatch(umd.pkg.path::equals)) {
                        collector.addRequires(
                                umd.pkg.path, formatDep(rdepa, ver, dep.getNamespace()));
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return "Maven generator";
    }
}
