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
        public void addProvides(String name) {
            if (includeProvides) {
                delegate.addProvides(name);
            }
        }

        @Override
        public void addRequires(String name) {
            if (includeRequires) {
                delegate.addRequires(name);
            }
        }
    }

    public FilteredGenerator(Generator delegate, boolean includeProvides, boolean includeRequires) {
        this.delegate = delegate;
        this.includeProvides = includeProvides;
        this.includeRequires = includeRequires;
    }

    @Override
    public void generate(Path filePath, Collector collector) {
        delegate.generate(filePath, new FilteringCollector(collector));
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}