package com.datbear;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("bearycudaTrials")
public interface BearycudaTrialsConfig extends Config {

    @ConfigSection(name = "Outlines/Colors", description = "All options relating to colors & outlines", position = 1, closedByDefault = false)
    String outlines = "outlines";

    @ConfigSection(name = "Menu Swaps", description = "All options relating to menu entry swaps", position = 2, closedByDefault = true)
    String menuSwaps = "menuSwaps";

    @ConfigSection(name = "Debug", description = "Debugging options (menu items, extra overlays)", position = 3, closedByDefault = true)
    String debug = "debug";

    @ConfigItem(keyName = "showRouteLines", name = "Show route lines", description = "Toggle drawing of the route polyline", section = outlines, position = 2)
    default boolean showRouteLines() {
        return true;
    }

    @ConfigItem(keyName = "routeLineColor", name = "Route line color", description = "Color used to draw the route polyline", section = outlines, position = 3)
    default Color routeLineColor() {
        return new Color(0, 255, 255, 200);
    }

    @Alpha
    @ConfigItem(keyName = "showRouteDots", name = "Show route dots", description = "Toggle drawing of route waypoint dots", section = outlines, position = 4)
    default boolean showRouteDots() {
        return true;
    }

    @ConfigItem(keyName = "routeDotColor", name = "Route dot color", description = "Color used for route waypoint dots", section = outlines, position = 5)
    default Color routeDotColor() {
        return new Color(255, 255, 255, 200);
    }

    @Alpha
    @ConfigItem(keyName = "showCrateHighlights", name = "Show crate highlights", description = "Toggle outlining of trial crates", section = outlines, position = 6)
    default boolean showCrateHighlights() {
        return true;
    }

    @ConfigItem(keyName = "crateHighlightColor", name = "Crate highlight color", description = "Outline color for trial crates", section = outlines, position = 7)
    default Color crateHighlightColor() {
        return Color.YELLOW;
    }

    @Alpha
    @ConfigItem(keyName = "showBoostHighlights", name = "Show boost highlights", description = "Toggle highlighting of speed-boost tiles/objects", section = outlines, position = 8)
    default boolean showBoostHighlights() {
        return true;
    }

    @ConfigItem(keyName = "boostHighlightColor", name = "Boost highlight color", description = "Highlight color for speed-boost tiles/objects", section = outlines, position = 9)
    default Color boostHighlightColor() {
        return Color.BLUE;
    }

    @Alpha
    @ConfigItem(keyName = "showTrimSailHighlights", name = "Show trim sail highlights", description = "Toggle highlights for trimmable sails", section = outlines, position = 10)
    default boolean showTrimSailHighlights() {
        return true;
    }

    @ConfigItem(keyName = "trimSailHighlightColor", name = "Trim sail highlight color", description = "Highlight color used for trimmable sails", section = outlines, position = 11)
    default Color trimSailHighlightColor() {
        return new Color(0, 255, 0, 150);
    }

    @Alpha
    @ConfigItem(keyName = "showJubblyToadHighlights", name = "Show Jubbly toad flag highlights", description = "Toggle highlighting of Jubbly toad flags", section = outlines, position = 12)
    default boolean showJubblyToadHighlights() {
        return true;
    }

    @ConfigItem(keyName = "jubblyToadInRangeColor", name = "Jubbly toad in-range color", description = "Color used for toad flags when within range", section = outlines, position = 13)
    default Color jubblyToadInRangeColor() {
        return new Color(0, 255, 0, 200);
    }

    @Alpha
    @ConfigItem(keyName = "jubblyToadOutOfRangeColor", name = "Jubbly toad out-of-range color", description = "Color used for toad flags when out of range", section = outlines, position = 14)
    default Color jubblyToadOutOfRangeColor() {
        return new Color(255, 0, 0, 150);
    }

    @ConfigItem(keyName = "enableStartPreviousRankLeftClick", name = "Enable 'Start-previous-rank' left-click", description = "When enabled, this will swap the left-click action on trial npcs to Start-previous-rank", section = menuSwaps, position = 1)
    default boolean enableStartPreviousRankLeftClick() {
        return true;
    }

    @ConfigItem(keyName = "enableQuickResetLeftClick", name = "Enable 'Quick-reset' left-click", description = "When enabled, this will swap the left-click action on the reset button in the HUD to Quick-reset", section = menuSwaps, position = 2)
    default boolean enableQuickResetLeftClick() {
        return true;
    }

    @ConfigItem(keyName = "disableStopNavigating", name = "Disable 'Stop-navigating' left-click during trials", description = "When enabled, this will remove the left-click 'Stop-navigating' action on the helm while in a trial", section = menuSwaps, position = 3)
    default boolean disableStopNavigating() {
        return true;
    }

    @ConfigItem(keyName = "showDebugOverlay", name = "Show debug overlay", description = "Show debugging info (player/instance coords & next waypoint indices)", section = debug, position = 1)
    default boolean showDebugOverlay() {
        return false;
    }

    @ConfigItem(keyName = "showDebugMenuCopyTileOptions", name = "Show debug tile copy menu options", description = "Adds 'Copy worldpoint' and 'Copy tile worldpoint' menu items for debugging", section = debug, position = 2)
    default boolean showDebugMenuCopyTileOptions() {
        return false;
    }

    @ConfigItem(keyName = "showGwenithGlideRoutes", name = "Show Gwenith Glide routes (INCOMPLETE)", description = "Toggle display of Gwenith Glide routes", section = debug, position = 3)
    default boolean showGwenithGlideRoutes() {
        return false;
    }

}
