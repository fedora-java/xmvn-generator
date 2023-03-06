package org.fedoraproject.xmvn.generator;

public interface HookFactory {
    Hook createHook(BuildContext context);
}
