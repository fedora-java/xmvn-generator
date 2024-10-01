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

import java.nio.file.Path;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class FilteredGenerator implements Generator {
    private final Generator delegate;
    private final boolean includeProvides;
    private final boolean includeRequires;

    class FilteringCollector implements Collector {
        private final Collector delegate;

        public FilteringCollector(Collector collector) {
            this.delegate = collector;
        }

        @Override
        public void addProvides(Path filePath, String name) {
            if (includeProvides) {
                delegate.addProvides(filePath, name);
            }
        }

        @Override
        public void addRequires(Path filePath, String name) {
            if (includeRequires) {
                delegate.addRequires(filePath, name);
            }
        }
    }

    public FilteredGenerator(Generator delegate, boolean includeProvides, boolean includeRequires) {
        this.delegate = delegate;
        this.includeProvides = includeProvides;
        this.includeRequires = includeRequires;
    }

    @Override
    public void generate(Collector collector) {
        delegate.generate(new FilteringCollector(collector));
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}