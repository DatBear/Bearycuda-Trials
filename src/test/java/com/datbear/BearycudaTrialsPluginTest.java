package com.datbear;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BearycudaTrialsPluginTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(BearycudaTrialsPlugin.class);
        RuneLite.main(args);
    }
}