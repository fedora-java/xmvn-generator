package org.fedoraproject.xmvn.generator.callback;

import org.fedoraproject.xmvn.generator.logging.Logger;

public class PostInstallHook implements Runnable {
    @Override
    public void run() {
        Logger.enableDebug();
        Logger.beg();
        Logger.debug("Post-Install Hook");
        Logger.end();
    }
}
