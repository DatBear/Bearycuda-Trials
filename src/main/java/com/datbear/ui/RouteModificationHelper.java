package com.datbear.ui;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.util.ArrayList;
import java.util.Arrays;

import com.datbear.data.TrialRoute;

import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOptionClicked;
// unused import removed

public class RouteModificationHelper {
    private static final String MENU_TOP_LEVEL = "[BT] Route Modification";
    private static final String MENU_ADD_WAYPOINT = "[BT] Add waypoint to route";
    private static final String MENU_INSERT_WAYPOINT = "[BT] Insert waypoint into route";
    private static final String MENU_REMOVE_WAYPOINT = "[BT] Remove this waypoint";
    private static final String MENU_EXPORT_ROUTE_POINTS = "[BT] Export route points to clipboard";

    public static MenuEntry[] addRouteModificationEntries(Client client, MenuEntry[] entries, TrialRoute route) {
        if (client == null || route == null) {
            return entries;
        }

        // Avoid duplicates if already added
        if (entries != null) {
            for (MenuEntry e : entries) {
                if (e != null && MENU_TOP_LEVEL.equals(e.getOption())) {
                    return entries; // already present
                }
            }
        }

        String routeLabel = "";//route.Location + " " + route.Rank;

        // Create a top-level marker entry (non-functional placeholder for grouping)
        var top = client.getMenu().createMenuEntry(-1)
                .setOption(MENU_TOP_LEVEL)
                .setTarget(routeLabel)
                .setType(MenuAction.RUNELITE);

        var subMenu = top.createSubMenu();

        // Create child entries (simulated hierarchy by ordering directly after top-level)
        var export = subMenu.createMenuEntry(-1)
                .setOption(MENU_EXPORT_ROUTE_POINTS)
                .setTarget(routeLabel)
                .setType(MenuAction.RUNELITE);
        var remove = subMenu.createMenuEntry(-1)
                .setOption(MENU_REMOVE_WAYPOINT)
                .setTarget(routeLabel)
                .setType(MenuAction.RUNELITE);
        var insert = subMenu.createMenuEntry(-1)
                .setOption(MENU_INSERT_WAYPOINT)
                .setTarget(routeLabel)
                .setType(MenuAction.RUNELITE);
        var add = subMenu.createMenuEntry(-1)
                .setOption(MENU_ADD_WAYPOINT)
                .setTarget(routeLabel)
                .setType(MenuAction.RUNELITE);

        var entryList = new ArrayList<MenuEntry>();
        entryList.add(top);
        entryList.addAll(Arrays.asList(entries));

        return entryList.toArray(new MenuEntry[0]);
    }

    // Handle clicks; lastVisitedIndex constrains inserts to +/- 5 of recent progress.
    public static boolean handleMenuOptionClicked(MenuOptionClicked event, Client client, TrialRoute route, Point menuOpenedPoint, int lastVisitedIndex) {
        if (event == null || client == null || route == null) {
            return false;
        }
        String option = event.getMenuOption();
        if (option == null) {
            return false;
        }
        boolean isOur = option.equals(MENU_ADD_WAYPOINT) || option.equals(MENU_INSERT_WAYPOINT) || option.equals(MENU_REMOVE_WAYPOINT) || option.equals(MENU_EXPORT_ROUTE_POINTS);
        if (!isOur) {
            return false;
        }

        Point canvasPoint = menuOpenedPoint != null ? menuOpenedPoint : client.getMouseCanvasPosition();
        WorldPoint worldPoint = getWorldPointFromPoint(client, canvasPoint);
        if (worldPoint == null) {
            return true;
        }

        if (option.equals(MENU_ADD_WAYPOINT)) {
            AddWaypoint(worldPoint, route);
        } else if (option.equals(MENU_INSERT_WAYPOINT)) {
            InsertWaypoint(worldPoint, route, lastVisitedIndex);
        } else if (option.equals(MENU_REMOVE_WAYPOINT)) {
            RemoveWaypoint(worldPoint, route, lastVisitedIndex);
        } else if (option.equals(MENU_EXPORT_ROUTE_POINTS)) {
            ExportRoute(route);
        }

        event.consume();
        return true;
    }

    private static void AddWaypoint(WorldPoint worldPoint, TrialRoute route) {
        if (route == null || worldPoint == null) {
            return;
        }
        if (route.Points == null) {
            route.Points = new ArrayList<>();
        }
        route.Points.add(worldPoint);
    }

    private static void InsertWaypoint(WorldPoint worldPoint, TrialRoute route, int lastVisitedIndex) {
        if (route == null || worldPoint == null) {
            return;
        }

        if (route.Points == null || route.Points.isEmpty()) {
            AddWaypoint(worldPoint, route);
            return;
        }
        int nearestIdx = -1;
        int minAllowed = Math.max(0, lastVisitedIndex - 5);
        int maxAllowed = Math.min(route.Points.size() - 1, lastVisitedIndex + 5);
        double nearestDist = Double.MAX_VALUE;
        for (int i = minAllowed; i <= maxAllowed; i++) {
            WorldPoint p = route.Points.get(i);
            if (p == null) {
                continue;
            }
            double d = Math.hypot(p.getX() - worldPoint.getX(), p.getY() - worldPoint.getY());
            if (d < nearestDist) {
                nearestDist = d;
                nearestIdx = i;
            }
        }
        // Insert after nearest to preserve forward ordering feel.
        route.Points.add(nearestIdx + 1, worldPoint);
    }

    private static void RemoveWaypoint(WorldPoint worldPoint, TrialRoute route, int lastVisitedIndex) {
        if (route == null || worldPoint == null) {
            return;
        }

        if (route.Points == null || route.Points.isEmpty()) {
            return;
        }
        int removeIdx = -1;
        double threshold = 5.0; // tiles
        double nearestDist = Double.MAX_VALUE;
        int minAllowed = Math.max(0, lastVisitedIndex - 5);
        int maxAllowed = Math.min(route.Points.size() - 1, lastVisitedIndex + 5);
        for (int i = minAllowed; i <= maxAllowed; i++) {
            WorldPoint p = route.Points.get(i);
            if (p == null) {
                continue;
            }
            double d = Math.hypot(p.getX() - worldPoint.getX(), p.getY() - worldPoint.getY());
            if (d < threshold && d < nearestDist) {
                nearestDist = d;
                removeIdx = i;
            }
        }
        if (removeIdx >= 0) {
            route.Points.remove(removeIdx);
        }
    }

    private static void ExportRoute(TrialRoute route) {
        if (route == null) {
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\t// Trial Route:\n");
        var idx = 0;
        for (var point : route.Points) {
            sb.append(String.format("/*%d*/new WorldPoint(%d, %d, %d)%s\n", idx++, point.getX(), point.getY(), point.getPlane(), idx < route.Points.size() ? "," : ""));
        }
        sb.append("\t// End of trial route\n");

        var stringSelection = new StringSelection(sb.toString());
        var clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(stringSelection, null);
    }

    private static WorldPoint getWorldPointFromPoint(Client client, Point point) {
        var worldView = client.getTopLevelWorldView();
        var scene = worldView.getScene();
        var z = worldView.getPlane();
        var tiles = scene.getTiles();

        if (tiles != null && z >= 0 && z < tiles.length) {
            var plane = tiles[z];
            for (var x = 0; x < plane.length; x++) {
                for (var y = 0; y < plane[x].length; y++) {
                    var tile = plane[x][y];
                    if (tile == null)
                        continue;
                    var lp = tile.getLocalLocation();
                    var poly = Perspective.getCanvasTilePoly(client, lp);
                    if (poly == null || point == null)
                        continue;
                    if (poly.contains(point.getX(), point.getY())) {
                        return WorldPoint.fromLocalInstance(client, lp);
                    }
                }
            }
        }
        return null;
    }

}
