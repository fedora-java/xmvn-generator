package org.fedoraproject.xmvn.generator.jpscript;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPackageScriptGenerator implements Generator {
    private final BuildContext context;

    public JPackageScriptGenerator(BuildContext context) {
        this.context = context;
    }

    @Override
    public void generate(Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        Path binDir = buildRoot.resolve("usr/bin");
        if (Files.isDirectory(binDir)) {
            try (Stream<Path> filePaths = Files.find(binDir, 1, (path, attr) -> attr.isRegularFile())) {
                for (Path filePath : filePaths.toList()) {
                    if (Files.readString(filePath).contains("\n. /usr/share/java-utils/java-functions\n")) {
                        collector.addRequires(filePath, "javapackages-tools");
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    @Override
    public String toString() {
        return "jpackage_script generator";
    }
}
