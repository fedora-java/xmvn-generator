package org.fedoraproject.xmvn.generator.jpscript;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.Generator;

class JPackageScriptGenerator implements Generator {
    private final BuildContext context;

    public JPackageScriptGenerator(BuildContext context) {
        this.context = context;
    }

    @Override
    public void generate(List<Path> filePaths, Collector collector) {
        Path buildRoot = Path.of(context.eval("%{buildroot}"));
        Path binDir = buildRoot.resolve("usr/bin");
        for (Path filePath : filePaths) {
            if (filePath.getParent().equals(binDir) && Files.isRegularFile(filePath)) {
                try {
                    if (Files.readString(filePath).contains("\n. /usr/share/java-utils/java-functions\n")) {
                        collector.addRequires(filePath, "javapackages-tools");
                    }
                } catch (IOException e) {
                    // Print the exception trace, ignore it otherwise
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "jpackage_script generator";
    }
}
