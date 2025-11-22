package com.datbear;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BearracudaTrialsPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(BearracudaTrialsPlugin.class);
        RuneLite.main(args);
    }
}