/*-
 * Copyright (c) 2023-2024 Red Hat, Inc.
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
package org.fedoraproject.xmvn.generator.jpms;

import java.nio.file.Files;
import java.nio.file.Path;
import org.easymock.EasyMock;
import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class JPMSGeneratorTest {
    private Collector collector;
    private BuildContext context;

    @TempDir private Path br;

    @BeforeEach
    public void setUp() {
        collector = EasyMock.createMock(Collector.class);
        context = EasyMock.createMock(BuildContext.class);
        EasyMock.expect(context.eval("%{buildroot}")).andReturn(br.toString()).anyTimes();
    }

    private void expectProvides(Path filePath, String prov) {
        collector.addProvides(filePath, prov);
        EasyMock.expectLastCall();
    }

    private void performTest(Path srcPath, Path jarPath) throws Exception {
        EasyMock.replay(collector, context);
        Files.createDirectories(jarPath.getParent());
        Files.copy(srcPath, jarPath);
        new JPMSGenerator(context).generate(collector);
        EasyMock.verify(collector, context);
    }

    @Test
    public void testSimpleJar() throws Exception {
        Path srcPath = Path.of("src/test/resources/simple.jar");
        Path jarPath = br.resolve("usr/share/java/simple.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(srcPath, jarPath);
    }

    @Test
    public void testMultiReleaseJar() throws Exception {
        Path srcPath = Path.of("src/test/resources/mr.jar");
        Path jarPath = br.resolve("usr/lib/java/mr.jar");
        expectProvides(jarPath, "jpms(foo)");
        performTest(srcPath, jarPath);
    }
}
