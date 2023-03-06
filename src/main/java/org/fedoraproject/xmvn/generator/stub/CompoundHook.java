package org.fedoraproject.xmvn.generator.stub;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.fedoraproject.xmvn.generator.BuildContext;
import org.fedoraproject.xmvn.generator.Hook;
import org.fedoraproject.xmvn.generator.HookFactory;
import org.fedoraproject.xmvn.generator.callback.Callback;
import org.fedoraproject.xmvn.generator.logging.Logger;

class CompoundHook {
    private final BuildContext buildContext;
    private final List<Hook> hooks;

    public CompoundHook(BuildContext buildContext, HookFactory... factories) {
        this.buildContext = buildContext;
        hooks = Arrays.asList(factories).stream().map(factory -> factory.createHook(buildContext))
                .collect(Collectors.toList());
    }

    void runHook() {
        Logger.beg();
        Logger.debug("Post-install hooks");
        for (Hook hook : hooks) {
            Logger.cut();
            Logger.debug("=> Running hook " + hook.getClass().getSimpleName());
            hook.run();
        }
        Logger.end();
    }

    public String setUpHook() throws IOException {
        if (!buildContext.eval("%{?__xmvngen_debug}").isEmpty()) {
            Logger.enableDebug();
        }
        Callback cb = Callback.setUp(this::runHook);
        return String.join(" ", cb.getCommand());
    }
}
