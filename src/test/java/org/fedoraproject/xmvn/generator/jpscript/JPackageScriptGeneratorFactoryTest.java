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
package org.fedoraproject.xmvn.generator.jpscript;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.easymock.EasyMock;
import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Generator;
import org.fedoraproject.xmvn.generator.GeneratorFactory;
import org.junit.jupiter.api.Test;

public class JPackageScriptGeneratorFactoryTest {
    @Test
    public void testFactory() {
        BuildContext bc = EasyMock.createStrictMock(BuildContext.class);
        EasyMock.replay(bc);
        GeneratorFactory factory = new JPackageScriptGeneratorFactory();
        Generator gen = factory.createGenerator(bc);
        assertInstanceOf(JPackageScriptGenerator.class, gen);
        EasyMock.verify(bc);
    }
}
