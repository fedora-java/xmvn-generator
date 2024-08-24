package org.fedoraproject.xmvn.generator.stub;

import static java.lang.foreign.MemorySegment.NULL;
import static java.lang.foreign.ValueLayout.ADDRESS;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Optional;

import org.fedoraproject.xmvn.generator.BuildContext;

class RpmBuildContext implements BuildContext {
    private static final ValueLayout STRING = ADDRESS
            .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));
    private final MethodHandle rpmExpandMH;

    public RpmBuildContext() {
        Linker linker = Linker.nativeLinker();
        Optional<MemorySegment> dlsymAddr = linker.defaultLookup().find("dlsym");
        if (dlsymAddr.isEmpty()) {
            throw new RuntimeException("dlsym is not available");
        }
        FunctionDescriptor dlsymFD = FunctionDescriptor.of(ADDRESS, ADDRESS, STRING);
        MethodHandle dlsymMH = linker.downcallHandle(dlsymAddr.get(), dlsymFD);
        MemorySegment rpmExpandAddr;
        try (Arena arena = Arena.ofConfined()) {
            rpmExpandAddr = (MemorySegment) dlsymMH.invokeExact(NULL, arena.allocateFrom("rpmExpand"));
        } catch (Throwable t) {
            throw new RuntimeException("Unable to lookup rpmExpand symbol with dlsym()", t);
        }
        if (NULL.equals(rpmExpandAddr)) {
            throw new RuntimeException(
                    "Symbol rpmExpand was not found - dlsym(RTLD_DEFAULT, \"rpmExpand\") returned NULL");
        }
        FunctionDescriptor rpmExpandFD = FunctionDescriptor.of(STRING, STRING, ADDRESS);
        rpmExpandMH = linker.downcallHandle(rpmExpandAddr, rpmExpandFD);
    }

    @Override
    public String eval(String macro) {
        try (Arena arena = Arena.ofConfined()) {
            return ((MemorySegment) rpmExpandMH.invokeExact(arena.allocateFrom(macro), NULL)).getString(0);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to invoke librpmio function rpmExpand()", t);
        }
    }
}
