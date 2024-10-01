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

import java.util.jar.Manifest;
import org.fedoraproject.xmvn.generator.BuildContext;

class ManifestInjector implements ManifestTransformer {
    private final BuildContext buildContext;

    public ManifestInjector(BuildContext context) {
        this.buildContext = context;
    }

    private void inject(Manifest mf, String key, String rpmExpr) {
        String value = buildContext.eval(rpmExpr);
        mf.getMainAttributes().putValue(key, value);
    }

    @Override
    public void transform(Manifest mf) {
        inject(mf, "Rpm-Name", "%{NAME}");
        inject(mf, "Rpm-Epoch", "%{?EPOCH}");
        inject(mf, "Rpm-Version", "%{VERSION}");
        inject(mf, "Rpm-Release", "%{RELEASE}");
    }
}
