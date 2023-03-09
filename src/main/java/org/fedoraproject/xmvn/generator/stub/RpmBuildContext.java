package org.fedoraproject.xmvn.generator.stub;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.MemoryAddress;
import jdk.incubator.foreign.ResourceScope;

import org.fedoraproject.xmvn.generator.BuildContext;

class RpmBuildContext implements BuildContext {
    private static final MethodHandle dlsym = CLinker.getInstance().downcallHandle(
            CLinker.systemLookup().lookup("dlsym").get(),
            MethodType.methodType(MemoryAddress.class, MemoryAddress.class, MemoryAddress.class),
            FunctionDescriptor.of(CLinker.C_POINTER, CLinker.C_POINTER, CLinker.C_POINTER));

    private static MemoryAddress dlsym(String symbolName) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            return (MemoryAddress) dlsym.invokeExact(MemoryAddress.NULL,
                    CLinker.toCString(symbolName, scope).address());
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodHandle rpmExpand = CLinker.getInstance().downcallHandle(dlsym("rpmExpand"),
            MethodType.methodType(MemoryAddress.class, MemoryAddress.class),
            FunctionDescriptor.of(CLinker.C_POINTER, CLinker.C_POINTER));

    @Override
    public String eval(String macro) {
        try (var macroScope = ResourceScope.newConfinedScope()) {
            return CLinker.toJavaString(
                    (MemoryAddress) rpmExpand.invokeExact(CLinker.toCString(macro, macroScope).address()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
