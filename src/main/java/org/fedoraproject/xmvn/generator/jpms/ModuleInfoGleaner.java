package org.fedoraproject.xmvn.generator.jpms;

import static org.objectweb.asm.Opcodes.ACC_MANDATED;
import static org.objectweb.asm.Opcodes.ACC_STATIC_PHASE;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACC_TRANSITIVE;
import static org.objectweb.asm.Opcodes.ASM9;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ModuleVisitor;

import org.fedoraproject.xmvn.generator.Collector;
import org.fedoraproject.xmvn.generator.logging.Logger;

class ModuleInfoGleaner {
    private final Collector collector;

    public ModuleInfoGleaner(Collector collector) {
        this.collector = collector;
    }

    public void glean(InputStream inputStream) throws IOException {
        ClassVisitor classVisitor = new ClassVisitor(ASM9) {
            @Override
            public ModuleVisitor visitModule(String modName, int modAccess, String modVersion) {
                StringBuilder prov = new StringBuilder();
                prov.append("jpms(").append(modName).append(")");
                if (modVersion != null) {
                    prov.append(" = ").append(modVersion);
                }
                collector.addProvides(prov.toString());
                return new ModuleVisitor(ASM9) {
                    @Override
                    public void visitRequire(String depName, int depAccess, String depVersion) {
                        String dep = "jpms(" + depName + ")";
                        if ((depAccess & (ACC_STATIC_PHASE | ACC_MANDATED | ACC_SYNTHETIC)) != 0) {
                            Logger.debug("     skipped dependency on " + depName + " (access flags 0x"
                                    + Integer.toHexString(depAccess) + ")");
                        } else if ((depAccess & ACC_TRANSITIVE) != 0) {
                            collector.addRequires(dep);
                        } else {
                            Logger.debug("     skipped dependency on " + depName + " (intransitive)");
                        }
                    }
                };
            }
        };
        new ClassReader(inputStream).accept(classVisitor, 0);
    }
}
