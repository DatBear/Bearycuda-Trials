package com.datbear;

import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.*;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

import javax.inject.Inject;

import com.datbear.data.Directions;
import com.datbear.data.TrialRoute;
import com.datbear.overlay.WorldLines;
import com.datbear.overlay.WorldPerspective;

import java.awt.*;

public class BearycudaTrialsOverlay extends Overlay {
    // Retain legacy constants (unused after config wiring) in case future fallback logic needed.

    @Inject
    private ItemManager itemManager;

    @Inject
    private SpriteManager spriteManager;

    @Inject
    private ModelOutlineRenderer modelOutlineRenderer;

    private Client client;
    private BearycudaTrialsPlugin plugin;
    private BearycudaTrialsConfig config;

    private final int MOTE_SPRITE_ID = 7075;
    private int nextMoteIndex = -1;

    @Inject
    public BearycudaTrialsOverlay(Client client, BearycudaTrialsPlugin plugin,
            BearycudaTrialsConfig config) {
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

        var playerLoc = BoatLocation.fromLocal(client, player.getLocalLocation());
        if (playerLoc == null)
            return null;

        //renderCameraCenterDirection(graphics, Color.MAGENTA, 2, 40, Directions.SouthEast);

        renderLastMenuCanvasWorldPointOutline(graphics);
        highlightTrimmableSails(graphics);

        var route = plugin.getActiveTrialRoute();
        if (route == null) {
            if (config.showDebugOverlay()) {
                renderDebugInfo(graphics, route);
            }
            return null;
        }

        var boatLoc = BoatLocation.fromLocal(client, player.getLocalLocation());

        renderPortalArrows(graphics, route);
        highlightToadFlags(graphics, boatLoc);
        highlightCrates(graphics);
        highlightBoosts(graphics);
        renderWindMote(graphics);
        highlightTrialBoat(graphics);

        var visible = plugin.getVisibleActiveLineForPlayer(playerLoc, 5);
        if (config.showRouteLines() && visible.size() >= 2) {
            WorldLines.drawLinesOnWorld(graphics, client, visible, config.routeLineColor(), playerLoc.getPlane());
        }

        renderRouteDots(graphics, route);

        if (config.showDebugOverlay()) {
            renderDebugInfo(graphics, route);
        }
        return null;
    }

    private void renderRouteDots(Graphics2D graphics, TrialRoute route) {
        var nextIndices = plugin.getNextUnvisitedIndicesForActiveRoute(5);
        if (config.showRouteDots()) {
            for (int idx : nextIndices) {
                if (route.Points == null || idx < 0 || idx >= route.Points.size())
                    continue;
                var real = route.Points.get(idx);
                var wp = WorldPerspective.getInstanceWorldPointFromReal(client, client.getTopLevelWorldView(), real);
                if (wp == null)
                    continue;
                var pts = WorldPerspective.worldToCanvasWithOffset(client, wp, wp.getPlane());
                if (pts.isEmpty())
                    continue;
                var p = pts.get(0);

                renderLineDots(graphics, wp, config.routeDotColor(), idx, p);
            }
        }
    }

    private void renderLineDots(Graphics2D graphics, WorldPoint wp, Color color, int i, Point start) {
        final int size = (i == 0 ? 10 : 6);
        final Color fill = color;
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

    public void renderCameraCenterDirection(Graphics2D graphics, Color color, int diameter, int arrowLength, Directions direction) {
        if (graphics == null) {
            return;
        }
        if (color == null) {
            color = Color.WHITE;
        }
        if (diameter <= 0) {
            diameter = 8;
        }
        if (arrowLength <= 0) {
            arrowLength = 40;
        }

        int viewportXOffset = client.getViewportXOffset();
        int viewportYOffset = client.getViewportYOffset();
        int viewportWidth = client.getViewportWidth();
        int viewportHeight = client.getViewportHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return;
        }

        int centerX = viewportXOffset + viewportWidth / 2;
        int centerY = viewportYOffset + viewportHeight / 2;

        double dx;
        double dy;
        if (direction != null) {
            final double invSqrt2 = 1.0 / Math.sqrt(2.0);
            double wx; // world X (east +)
            double wy; // world Y (north +)
            switch (direction) {
                case North:
                    wx = 0;
                    wy = 1;
                    break;
                case NorthEast:
                    wx = 1;
                    wy = 1;
                    break;
                case East:
                    wx = 1;
                    wy = 0;
                    break;
                case SouthEast:
                    wx = 1;
                    wy = -1;
                    break;
                case South:
                    wx = 0;
                    wy = -1;
                    break;
                case SouthWest:
                    wx = -1;
                    wy = -1;
                    break;
                case West:
                    wx = -1;
                    wy = 0;
                    break;
                case NorthWest:
                    wx = -1;
                    wy = 1;
                    break;
                default:
                    wx = 0;
                    wy = -1;
                    break; // default south
            }
            if (Math.abs(wx) == 1 && Math.abs(wy) == 1) {
                wx *= invSqrt2;
                wy *= invSqrt2;
            }
            int yaw = client.getCameraYaw() & 2047;
            double yawRad = yaw * (Math.PI / 1024.0);
            double baseAngle = Math.atan2(wx, wy);
            double total = yawRad + baseAngle;
            dx = Math.sin(total);
            dy = -Math.cos(total);
        } else {
            int yaw = client.getCameraYaw() & 2047;
            double yawRad = yaw * (Math.PI / 1024.0);
            dx = Math.sin(yawRad);
            dy = -Math.cos(yawRad);
        }

        int endX = (int) Math.round(centerX + dx * arrowLength);
        int endY = (int) Math.round(centerY + dy * arrowLength);

        // Center dot
        int dotX = centerX - diameter / 2;
        int dotY = centerY - diameter / 2;
        Color borderColor = new Color(0, 0, 0, Math.min(255, color.getAlpha()));
        Stroke previous = graphics.getStroke();
        graphics.setColor(color);
        graphics.fillOval(dotX, dotY, diameter, diameter);
        graphics.setColor(borderColor);
        graphics.setStroke(new BasicStroke(2f));
        graphics.drawOval(dotX, dotY, diameter, diameter);

        // Direction line
        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(3f));
        graphics.drawLine(centerX, centerY, endX, endY);

        // Arrowhead
        double arrowAngle = Math.atan2(dy, dx);
        int headSize = 6;
        double left = arrowAngle + Math.toRadians(150);
        double right = arrowAngle - Math.toRadians(150);
        int leftX = (int) Math.round(endX + Math.cos(left) * headSize);
        int leftY = (int) Math.round(endY + Math.sin(left) * headSize);
        int rightX = (int) Math.round(endX + Math.cos(right) * headSize);
        int rightY = (int) Math.round(endY + Math.sin(right) * headSize);
        graphics.drawLine(endX, endY, leftX, leftY);
        graphics.drawLine(endX, endY, rightX, rightY);

        graphics.setStroke(previous);
    }

    private void renderDebugInfo(Graphics2D graphics, TrialRoute active) {
        if (client.getGameState() != GameState.LOGGED_IN) {
            return;
        }

        var player = client.getLocalPlayer();
        if (player == null)
            return;

        int x = 10;
        int y = 200;
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 15f));
        graphics.setColor(Color.WHITE);

        var boatLoc = BoatLocation.fromLocal(client, player.getLocalLocation());
        graphics.drawString("boat loc = " + (boatLoc == null ? "null" : boatLoc.toString()), x, y += 15);
        if (active != null) {
            graphics.drawString("active route = " + active.Location + " " + active.Rank, x, y += 15);
        } else {
            graphics.drawString("active route = null", x, y += 15);
        }
        graphics.drawString("last visited idx = " + plugin.getLastVisitedIndex(), x, y += 15);
        graphics.drawString("toad flag idx = " + plugin.getHighlightedToadFlagIndex(), x, y += 15);
        graphics.drawString("next mote idx = " + nextMoteIndex, x, y += 15);
    }

    private void renderLastMenuCanvasWorldPointOutline(Graphics2D graphics) {
        var pos = plugin.getLastMenuCanvasWorldPoint();
        if (pos == null) {
            return;
        }

        var localPoints = WorldPerspective.getInstanceLocalPointFromReal(client, pos);
        if (localPoints == null || localPoints.isEmpty()) {
            return;
        }

        for (var lp : localPoints) {
            if (lp == null)
                continue;
            java.awt.Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly == null)
                continue;

            // Draw a translucent fill and a bold border so the tile is obvious
            Color fill = new Color(255, 0, 255, 45);
            Color border = Color.MAGENTA;
            Stroke oldStroke = graphics.getStroke();
            Composite oldComposite = graphics.getComposite();

            graphics.setColor(fill);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            graphics.fill(poly);

            graphics.setComposite(oldComposite);
            graphics.setColor(border);
            graphics.setStroke(new BasicStroke(3f));
            graphics.draw(poly);

            // restore previous graphics state
            graphics.setStroke(oldStroke);
            graphics.setComposite(oldComposite);

            // only draw first matching instance tile
            break;
        }
    }

    private void highlightToadFlags(Graphics2D graphics, WorldPoint player) {
        if (!config.showJubblyToadHighlights()) {
            return;
        }
        var toadGameObjects = plugin.getToadFlagToHighlight();
        if (toadGameObjects == null || toadGameObjects.isEmpty()) {
            return;
        }
        Color inRange = config.jubblyToadInRangeColor();
        Color outRange = config.jubblyToadOutOfRangeColor();
        for (var toadGameObject : toadGameObjects) {
            var color = toadGameObject.getWorldLocation().distanceTo(player) < 15.2 ? inRange : outRange;
            modelOutlineRenderer.drawOutline(toadGameObject, 4, color, 2);
        }
    }

    private void highlightCrates(Graphics2D graphics) {
        if (!config.showCrateHighlights()) {
            return;
        }
        var crates = plugin.getTrialCratesById();
        Color crateColor = config.crateHighlightColor();
        for (var crate : crates.values()) {
            modelOutlineRenderer.drawOutline(crate, 2, crateColor, 2);
        }
    }

    private void highlightBoosts(Graphics2D graphics) {
        if (!config.showBoostHighlights()) {
            return;
        }
        var boosts = plugin.getTrialBoostsById();
        Color boostColor = config.boostHighlightColor();
        for (var boostList : boosts.values()) {
            for (var boost : boostList) {
                var poly = boost.getCanvasTilePoly();
                if (poly != null) {
                    OverlayUtil.renderPolygon(graphics, poly, boostColor);
                }
            }
        }
    }

    private void highlightTrialBoat(Graphics2D graphics) {
        //todo come back to this it doesn't work, the boats are world entities, not sure how to highlight those
        var boats = plugin.getTrialBoatsToHighlight();
        if (boats == null || boats.isEmpty()) {
            return;
        }
        for (var boat : boats) {
            modelOutlineRenderer.drawOutline(boat, 2, Color.CYAN, 2);
        }
    }

    private void renderWindMote(Graphics2D graphics) {
        var route = plugin.getActiveTrialRoute();
        if (route == null || plugin.getLastVisitedIndex() < 0) {
            return;
        }
        var optionalMoteIndex = route.WindMoteIndices.stream().filter(x -> x >= plugin.getLastVisitedIndex()).min(Integer::compareTo);
        nextMoteIndex = optionalMoteIndex.isPresent() ? optionalMoteIndex.get() : -1;
        var moteWorldPoint = nextMoteIndex != -1 && nextMoteIndex - Math.max(0, plugin.getLastVisitedIndex()) < 3 ? route.Points.get(nextMoteIndex) : null;
        if (moteWorldPoint == null) {
            return;
        }

        var localPoint = WorldPerspective.getInstanceLocalPointFromReal(client, moteWorldPoint);
        if (localPoint == null || localPoint.isEmpty()) {
            return;
        }

        var img = spriteManager.getSprite(MOTE_SPRITE_ID, 0);
        OverlayUtil.renderImageLocation(client, graphics, localPoint.get(0), img, 0);
    }

    private void highlightTrimmableSails(Graphics2D graphics) {
        if (!config.showTrimSailHighlights() || !plugin.isNeedsTrim()) {
            return;
        }
        var sail = plugin.getSailGameObject();
        if (sail == null || sail.getWorldView() == null) {
            return;
        }
        var hull = sail.getConvexHull();
        if (hull == null) {
            return;
        }
        OverlayUtil.renderPolygon(graphics, hull, config.trimSailHighlightColor());
    }

    private void renderPortalArrows(Graphics2D graphics, TrialRoute route) {
        if (!config.showGwenithGlideRoutes()) {
            return;
        }
        var portalDirection = plugin.getVisiblePortalDirection(route);
        if (portalDirection == null) {
            return;
        }

        renderCameraCenterDirection(graphics, config.routeLineColor(), 2, 50, portalDirection.FirstMovementDirection);
        renderCameraCenterDirection(graphics, Color.GREEN, 2, 50, portalDirection.BoatDirection);
    }

}
