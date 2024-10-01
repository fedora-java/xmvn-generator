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
package org.fedoraproject.xmvn.generator.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.jar.Manifest;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

public class ManifestInjectorTest {
    @Test
    public void testManifestInjector() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{NAME}")).andReturn("nn");
        EasyMock.expect(bc.eval("%{?EPOCH}")).andReturn("ee");
        EasyMock.expect(bc.eval("%{VERSION}")).andReturn("vv");
        EasyMock.expect(bc.eval("%{RELEASE}")).andReturn("rr");
        EasyMock.replay(bc);
        Manifest mf = new Manifest();
        mf.getMainAttributes().putValue("Foo", "xx");
        ManifestInjector manifestInjector = new ManifestInjector(bc);
        manifestInjector.transform(mf);
        assertEquals("xx", mf.getMainAttributes().getValue("Foo"));
        assertEquals("nn", mf.getMainAttributes().getValue("Rpm-Name"));
        assertEquals("ee", mf.getMainAttributes().getValue("Rpm-Epoch"));
        assertEquals("vv", mf.getMainAttributes().getValue("Rpm-Version"));
        assertEquals("rr", mf.getMainAttributes().getValue("Rpm-Release"));
        EasyMock.verify(bc);
    }
}
