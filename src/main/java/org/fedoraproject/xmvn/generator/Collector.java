package org.fedoraproject.xmvn.generator;

public interface Collector {
    void addProvides(String name);
    void addRequires(String name);
}
