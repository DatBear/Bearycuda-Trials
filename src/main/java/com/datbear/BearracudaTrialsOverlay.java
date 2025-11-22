package com.datbear;

import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;

import com.datbear.data.TrialRoute;
import com.datbear.overlay.WorldLines;
import com.datbear.overlay.WorldPerspective;

import java.awt.*;

public class BearracudaTrialsOverlay extends Overlay {
    private static final Color GREEN = new Color(0, 255, 0, 150);
    private static final Color RED = new Color(255, 0, 0, 150);

    @Inject
    private ItemManager itemManager;

    @Inject
    private ModelOutlineRenderer modelOutlineRenderer;

    private Client client;
    private BearracudaTrialsPlugin plugin;
    private BearracudaTrialsConfig config;

    @Inject
    public BearracudaTrialsOverlay(Client client, BearracudaTrialsPlugin plugin,
            BearracudaTrialsConfig config) {
        super();
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.UNDER_WIDGETS);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client.getGameState() != GameState.LOGGED_IN)
            return null;

        var player = client.getLocalPlayer();
        if (player == null)
            return null;

        final var playerLoc = WorldPerspective.getInstanceWorldPointFromReal(
                client,
                client.getTopLevelWorldView(),
                player.getWorldLocation());
        if (playerLoc == null)
            return null;

        // Find the active route and render it (polyline + dots + optional
        // boat->first)
        var active = plugin.getActiveTrialRoute();
        if (active == null) {
            if (config.showDebugOverlay()) {
                renderDebugInfo(graphics, active);
            }
            return null;
        }

        // Draw only the next up-to-5 waypoints (linear polyline beginning at the player's instance location).
        var visible = plugin.getVisibleActiveLineForPlayer(playerLoc, 5);
        if (visible.size() >= 2) {
            WorldLines.drawLinesOnWorld(graphics, client, visible, GREEN, playerLoc.getPlane());
        }

        // Render markers/labels for the next unvisited targets
        var nextIndices = plugin.getNextUnvisitedIndicesForActiveRoute(playerLoc, 5);
        for (int idx : nextIndices) {
            if (active.Points == null || idx < 0 || idx >= active.Points.size())
                continue;
            var real = active.Points.get(idx);
            var wp = WorldPerspective
                    .getInstanceWorldPointFromReal(client, client.getTopLevelWorldView(), real);
            if (wp == null)
                continue;
            var pts = WorldPerspective.worldToCanvasWithOffset(client, wp, wp.getPlane());
            if (pts.isEmpty())
                continue;
            var p = pts.get(0);

            renderLineDots(graphics, wp, GREEN, idx, p);
        }

        // Draw a single line from the player's boat location to the first
        // unvisited waypoint (if any) — uses top-level worldview mapping for
        // the target so we connect boat -> first target correctly.
        var boatLoc = BoatLocation.fromLocal(client, player.getLocalLocation());
        if (boatLoc != null) {
            var next = plugin.getNextUnvisitedIndicesForActiveRoute(boatLoc, 1);
            if (!next.isEmpty() && active.Points != null && next.get(0) < active.Points.size()) {
                var real = active.Points.get(next.get(0));
                var instp = WorldPerspective
                        .getInstanceWorldPointFromReal(client, client.getTopLevelWorldView(), real);
                if (instp != null) {
                    java.util.List<WorldPoint> two = java.util.List.of(boatLoc, instp);
                    WorldLines.drawLinesOnWorld(
                            graphics,
                            client,
                            two,
                            Color.CYAN,
                            boatLoc.getPlane());
                }
            }
        }

        if (config.showDebugOverlay()) {
            renderDebugInfo(graphics, active);
        }
        return null;
    }

    // Old route-specific rendering helpers removed — overlay now works against
    // generic TrialRoute data through the plugin's getVisibleLineForRoute and
    // getNextUnvisitedIndicesForRoute helper methods.

    private void renderLineDots(Graphics2D graphics, WorldPoint wp, Color color, int i,
            Point start) {
        final int size = (i == 0 ? 10 : 6);
        final Color fill = (i == 0 ? new Color(0, 255, 255, 200) : new Color(255, 255, 255, 200));
        final Color border = new Color(0, 0, 0, 200);

        graphics.setColor(fill);
        graphics.fillOval(start.getX() - size / 2, start.getY() - size / 2, size, size);

        graphics.setColor(border);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawOval(start.getX() - size / 2, start.getY() - size / 2, size, size);

        // Draw label (index) near the point so it's easy to match route-to-data
        final String label = String.valueOf(i);
        graphics.setColor(Color.BLACK);
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 12f));
        graphics.drawString(label, start.getX() + (size / 2) + 2, start.getY() - (size / 2) - 2);
    }

    private void renderDebugInfo(Graphics2D graphics, TrialRoute active) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        var player = client.getLocalPlayer();
        if (player == null)
            return;

        var boatLoc = BoatLocation.fromLocal(client, player.getLocalLocation());

        int x = 10;
        int y = 200;
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 15f));
        graphics.setColor(Color.WHITE);
        graphics.drawString("boat loc = " + (boatLoc == null ? "null" : boatLoc.toString()), x, y += 15);
        if (active != null) {
            graphics.drawString("active route = " + active.Location + " " + active.Rank, x, y += 15);
        } else {
            graphics.drawString("active route = null", x, y += 15);
        }
        graphics.drawString("last visited idx = " + plugin.getLastVisitedIndex(), x, y += 15);
    }
}
