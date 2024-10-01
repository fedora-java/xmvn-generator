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

import java.nio.file.Path;
import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;

public class TransformerHookFactory implements HookFactory {
    @Override
    public Hook createHook(BuildContext context) {
        ManifestTransformer transformer = new ManifestInjector(context);
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        TransformerHook hook = new TransformerHook(transformer, buildRoot);
        hook.addDirectoryPrefix(Path.of(context.eval("%{_javadir}")));
        hook.addDirectoryPrefix(Path.of(context.eval("%{_jnidir}")));
        return hook;
    }
}
