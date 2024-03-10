package com.doubleangels.nextdnsmanagement.geckoruntime;

import org.mozilla.geckoview.GeckoRuntime;

public class GeckoRuntimeSingleton {
    private static GeckoRuntime runtime;

    public static GeckoRuntime getInstance() {
        return runtime;
    }

    public static void setInstance(GeckoRuntime instance) {
        runtime = instance;
    }
}

