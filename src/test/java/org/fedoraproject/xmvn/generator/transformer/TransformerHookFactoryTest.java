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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.easymock.EasyMock;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

public class TransformerHookFactoryTest {
    @Test
    public void testFactory() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.expect(bc.eval("%{buildroot}")).andReturn("/tmp/build/root");
        EasyMock.expect(bc.eval("%{_javadir}")).andReturn("/tmp/javadir");
        EasyMock.expect(bc.eval("%{_jnidir}")).andReturn("/tmp/jnidir");
        EasyMock.replay(bc);
        HookFactory factory = new TransformerHookFactory();
        Hook hook = factory.createHook(bc);
        assertInstanceOf(TransformerHook.class, hook);
        EasyMock.verify(bc);
    }
}
