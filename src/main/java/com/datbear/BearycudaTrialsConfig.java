package com.datbear;

import net.runelite.client.config.*;

import java.awt.*;

@ConfigGroup("bearycudaTrials")
public interface BearycudaTrialsConfig extends Config {

    @ConfigSection(name = "Outlines/Colors", description = "All options relating to colors & outlines", position = 1, closedByDefault = false)
    String outlines = "outlines";

    @ConfigSection(name = "Menu Swaps", description = "All options relating to menu entry swaps", position = 2, closedByDefault = true)
    String menuSwaps = "menuSwaps";

    @ConfigSection(name = "Boat True Tile/Heading", description = "Options for showing the boat's true tile and heading arrows", position = 3, closedByDefault = false)
    String boatTrueTileHeading = "boatTrueTileHeading";

    @ConfigSection(name = "Debug", description = "Debugging options (menu items, extra overlays)", position = 4, closedByDefault = true)
    String debug = "debug";

    //outlines
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

    @ConfigItem(keyName = "showPortalRouteArrows", name = "Show portal route arrows", description = "Toggle showing route direction arrows when near portals", section = outlines, position = 15)
    default boolean showPortalRouteArrows() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "portalRouteArrowColor", name = "Portal route arrow color", description = "Color used for portal route direction arrows", section = outlines, position = 16)
    default Color portalRouteArrowColor() {
        return new Color(0, 255, 255, 200);
    }

    @ConfigItem(keyName = "showPortalBoatArrows", name = "Show portal boat arrows", description = "Toggle showing boat direction arrows when near portals", section = outlines, position = 17)
    default boolean showPortalBoatArrows() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "portalBoatArrowColor", name = "Portal boat arrow color", description = "Color used for portal boat direction arrows", section = outlines, position = 18)
    default Color portalBoatArrowColor() {
        return new Color(0, 255, 0, 200);
    }

    @ConfigItem(keyName = "showObstacleOutlines", name = "Show obstacle outlines", description = "Toggle outlining of obstacle tiles during trials", section = outlines, position = 19)
    default boolean showObstacleOutlines() {
        return false;
    }

    @Alpha
    @ConfigItem(keyName = "obstacleOutlineColor", name = "Obstacle outline color", description = "Color used to outline obstacle tiles", section = outlines, position = 20)
    default Color obstacleOutlineColor() {
        return new Color(255, 0, 0, 255);
    }

    @ConfigItem(keyName = "hideDecorations", name = "Hide decorations", description = "Hide decorative objects during trials", section = outlines, position = 21)
    default boolean hideDecorations() {
        return true;
    }

    @ConfigItem(keyName = "showSpeedBoostRemaining", name = "Show speed boost remaining", description = "Show a radial cooldown and ticks remaining on the wind mote button", section = outlines, position = 22)
    default boolean showSpeedBoostRemaining() {
        return true;
    }

    @ConfigItem(keyName = "showTrialBoatHighlights", name = "Show trial boat highlights", description = "Toggle outlining of the active trial boats", section = outlines, position = 23)
    default boolean showTrialBoatHighlights() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "trialBoatHighlightColor", name = "Trial boat highlight color", description = "Outline color used when highlighting trial boats", section = outlines, position = 24)
    default Color trialBoatHighlightColor() {
        return new Color(0, 255, 255, 200);
    }

    // true boat tile / heading
    @ConfigItem(keyName = "showBoatTrueTile", name = "Show true boat tile", description = "Highlight the boat's true tile", section = boatTrueTileHeading, position = 1)
    default boolean showBoatTrueTile() {
        return true;
    }

    @Alpha
    @ConfigItem(keyName = "boatTrueTileFillColor", name = "True boat tile fill color", description = "Fill color for the boat's true tile", section = boatTrueTileHeading, position = 2)
    default Color boatTrueTileFillColor() {
        return new Color(255, 255, 255, 45);
    }

    @ConfigItem(keyName = "boatTrueTileBorderColor", name = "True boat tile border color", description = "Border color for the boat's true tile", section = boatTrueTileHeading, position = 3)
    default Color boatTrueTileBorderColor() {
        return Color.BLACK;
    }

    @ConfigItem(keyName = "showCurrentHeading", name = "Show current heading", description = "Show the current heading arrow", section = boatTrueTileHeading, position = 4)
    default boolean showCurrentHeading() {
        return false;
    }

    @Alpha
    @ConfigItem(keyName = "currentHeadingColor", name = "Current heading color", description = "Color for the current heading arrow", section = boatTrueTileHeading, position = 5)
    default Color currentHeadingColor() {
        return Color.GREEN;
    }

    @ConfigItem(keyName = "showRequestedHeading", name = "Show requested heading", description = "Show the requested heading arrow", section = boatTrueTileHeading, position = 6)
    default boolean showRequestedHeading() {
        return false;
    }

    @Alpha
    @ConfigItem(keyName = "requestedHeadingColor", name = "Requested heading color", description = "Color for the requested heading arrow", section = boatTrueTileHeading, position = 7)
    default Color requestedHeadingColor() {
        return Color.YELLOW;
    }

    @ConfigItem(keyName = "showHoveredHeading", name = "Show hovered heading", description = "Show the hovered heading arrow", section = boatTrueTileHeading, position = 8)
    default boolean showHoveredHeading() {
        return false;
    }

    @Alpha
    @ConfigItem(keyName = "hoveredHeadingColor", name = "Hovered heading color", description = "Color for the hovered heading arrow", section = boatTrueTileHeading, position = 9)
    default Color hoveredHeadingColor() {
        return Color.WHITE;
    }

    //menu swaps
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

    @ConfigItem(keyName = "disableUnsetSail", name = "Disable 'Unset sail' left-click during trials", description = "When enabled, this will remove the left-click 'Unset' action on the sail while in a trial", section = menuSwaps, position = 4)
    default boolean disableUnsetSail() {
        return true;
    }

    //debug
    @ConfigItem(keyName = "showDebugOverlay", name = "Show debug overlay", description = "Show debugging info (player/instance coords & next waypoint indices)", section = debug, position = 1)
    default boolean showDebugOverlay() {
        return false;
    }

    @ConfigItem(keyName = "showDebugMenuCopyTileOptions", name = "Show debug tile copy menu options", description = "Adds 'Copy worldpoint' and 'Copy tile worldpoint' menu items for debugging", section = debug, position = 2)
    default boolean showDebugMenuCopyTileOptions() {
        return false;
    }

    @ConfigItem(keyName = "showDebugRouteModificationOptions", name = "Show debug route modification menu options", description = "Adds '[BT]' menu items for route modifications", section = debug, position = 3)
    default boolean showDebugRouteModificationOptions() {
        return false;
    }

    @ConfigItem(keyName = "enableCratePickupDebug", name = "Track crate pickup distance", description = "Log and display crate pickup distances for debugging", section = debug, position = 4)
    default boolean enableCratePickupDebug() {
        return false;
    }

    @ConfigItem(keyName = "enableBoatPathDebug", name = "Show boat path overlay", description = "Draw boat path tiles for the last several ticks", section = debug, position = 5)
    default boolean enableBoatPathDebug() {
        return false;
    }

}
