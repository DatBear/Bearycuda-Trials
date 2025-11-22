package com.datbear;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("bearSailing")
public interface BearracudaTrialsConfig extends Config {
    @ConfigSection(name = "General Notifications", closedByDefault = false, position = 0, description = "Choose when you are notified.")
    String generalNotifications = "generalNotifications";

    @ConfigSection(name = "Menu Swaps", description = "All options relating to menu entry swaps", position = 1, closedByDefault = false)
    String menuSwaps = "menuSwaps";

    @ConfigSection(name = "Outlines", description = "All options relating to colored outlines", position = 2, closedByDefault = true)
    String outlines = "outlines";

    @ConfigSection(name = "Overlays", description = "All options relating to overlays", position = 3, closedByDefault = true)
    String overlays = "overlays";

    enum RouteDisplay {
        Swordfish, Marlin, Both
    }

    @ConfigItem(keyName = "routeToShow", name = "Route to show", description = "Which route to display on the overlay (Swordfish, Marlin, or Both)", section = overlays, position = 0)
    default RouteDisplay routeToShow() {
        return RouteDisplay.Both;
    }

    @ConfigItem(keyName = "showDebugOverlay", name = "Show debug overlay", description = "Show debugging info (player/instance coords & next waypoint indices)", section = overlays, position = 1)
    default boolean showDebugOverlay() {
        return false;
    }

    @ConfigItem(keyName = "muteApprentices", name = "Mute game help messages", description = "Mutes the over head messages of the apprentices giving game advice.")
    default boolean muteApprentices() {
        return true;
    }

}
