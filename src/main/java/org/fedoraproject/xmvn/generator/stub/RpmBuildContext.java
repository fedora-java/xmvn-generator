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
package org.fedoraproject.xmvn.generator.stub;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.fedoraproject.xmvn.generator.BuildContext;

class RpmBuildContext implements BuildContext {
    static {
        try (InputStream is = RpmBuildContext.class.getResourceAsStream("/xmvn-generator-native.so")) {
            Path p = Files.createTempFile("xmvngen-native", ".so");
            try (OutputStream os = Files.newOutputStream(p)) {
                is.transferTo(os);
            }
            System.load(p.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public native String eval(String macro);
}
