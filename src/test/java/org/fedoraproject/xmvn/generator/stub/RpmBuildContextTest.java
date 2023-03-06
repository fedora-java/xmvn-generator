package org.fedoraproject.xmvn.generator.stub;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ResourceScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.fedoraproject.xmvn.generator.BuildContext;

public class RpmBuildContextTest {
    private static final MethodHandle dlopen = CLinker.getInstance().downcallHandle(
            CLinker.systemLookup().lookup("dlopen").get(),
            MethodType.methodType(MemoryAddress.class, MemoryAddress.class, int.class),
            FunctionDescriptor.of(CLinker.C_POINTER, CLinker.C_POINTER, CLinker.C_INT));

    @BeforeAll
    public static void setUpClass() {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemoryAddress handle = (MemoryAddress) dlopen.invokeExact(CLinker.toCString("librpmio.so", scope).address(),
                    0x101);
            assumeTrue(handle != MemoryAddress.NULL);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testEval() {
        BuildContext bc = new RpmBuildContext();
        assertEquals("12", bc.eval("%[7+5]"));
        assertEquals("12", bc.eval("%(expr 7 + 5)"));
    }
}
