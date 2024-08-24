package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

public class RpmBuildContextTest {
    /**
     * RpmBuildContext assumes that it is ran in the context of an rpmbuild process.
     * This implies that RPM libraries such as librpmio.so are loaded in the running
     * process and RPM library symbols can be loaded with dlsym(RTLD_DEFAULT, ...).
     * This assumption is not met when running tests, so relevant RPM native
     * libraries must be loaded explicitly before starting the actual test.
     */
    @BeforeAll
    public static void loadNativeLibraries() {
        Linker linker = Linker.nativeLinker();
        MethodHandle dlopen = linker.downcallHandle(linker.defaultLookup().find("dlopen").get(),
                FunctionDescriptor.ofVoid(
                        ValueLayout.ADDRESS
                                .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE)),
                        ValueLayout.JAVA_INT));
        // Try to dlopen() various libraries and ignore failures
        for (String lib : List.of("librpm.so.10", "librpm.so.9")) {
            try (Arena arena = Arena.ofConfined()) {
                // From dlfcn.h
                int RTLD_LAZY = 0x00001;
                int RTLD_GLOBAL = 0x00100;
                dlopen.invokeExact(arena.allocateFrom(lib), RTLD_LAZY | RTLD_GLOBAL);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    @Test
    public void testEval() {
        BuildContext bc = new RpmBuildContext();
        assertEquals("12", bc.eval("%[7+5]"));
        assertEquals("12", bc.eval("%(expr 7 + 5)"));
    }
}
