package com.datbear;

import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
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

import lombok.extern.slf4j.Slf4j;

import java.awt.*;

@Slf4j
public class BearycudaTrialsOverlay extends Overlay {
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

        var boatLocation = BoatLocation.fromLocal(client, player.getLocalLocation());
        if (boatLocation == null)
            return null;

        var playerLocation = player.getWorldLocation();

        renderTrueInfo(graphics, boatLocation, playerLocation);

        renderLastMenuCanvasWorldPointOutline(graphics);
        highlightTrimmableSails(graphics);

        var route = plugin.getActiveTrialRoute();
        if (route == null) {
            if (config.showDebugOverlay()) {
                renderDebugInfo(graphics, route);
            }
            return null;
        }

        renderPortalArrows(graphics, route, playerLocation);
        highlightToadFlags(graphics, boatLocation);
        highlightCrates(graphics);
        highlightBoosts(graphics);
        renderWindMote(graphics);
        highlightTrialBoat(graphics);

        var visible = plugin.getVisibleActiveLineForPlayer(boatLocation, 5);
        if (config.showRouteLines() && visible.size() >= 2) {
            WorldLines.drawLinesOnWorld(graphics, client, visible, config.routeLineColor(), boatLocation.getPlane());
        }

        renderRouteDots(graphics, route);

        if (config.showDebugOverlay()) {
            renderDebugInfo(graphics, route);
        }
        return null;
    }

    private void renderTrueInfo(Graphics2D graphics, WorldPoint boatLocation, WorldPoint playerLocation) {
        if (config.showTrueBoatTile()) {
            highlightBoatTrueTile(graphics, boatLocation);
        }

        if (config.showRequestedHeading()) {
            renderHeadingTriangle(graphics, playerLocation, plugin.getRequestedHeadingDirection(), config.requestedHeadingColor(), 30, 8);
        }

        if (config.showHoveredHeading()) {
            renderHeadingTriangle(graphics, playerLocation, plugin.getHoveredHeadingDirection(), config.hoveredHeadingColor(), 30, 8);
        }

        if (config.showCurrentHeading()) {
            renderHeadingTriangle(graphics, playerLocation, plugin.getCurrentHeadingDirection(), config.currentHeadingColor(), 30, 8);
        }
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

        // Varbit-derived plugin state
        graphics.drawString("boatSpawnedAngle = " + plugin.getBoatSpawnedAngle(), x, y += 15);
        graphics.drawString("boatSpawnedFineX = " + plugin.getBoatSpawnedFineX(), x, y += 15);
        graphics.drawString("boatSpawnedFineZ = " + plugin.getBoatSpawnedFineZ(), x, y += 15);
        graphics.drawString("boatBaseSpeed = " + plugin.getBoatBaseSpeed(), x, y += 15);
        graphics.drawString("boatSpeedCap = " + plugin.getBoatSpeedCap(), x, y += 15);
        graphics.drawString("boatSpeedBoostDuration = " + plugin.getBoatSpeedBoostDuration(), x, y += 15);
        graphics.drawString("isInTrial = " + plugin.getIsInTrial(), x, y += 15);

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

    private void renderPortalArrows(Graphics2D graphics, TrialRoute route, WorldPoint boatLoc) {
        var portalDirection = plugin.getVisiblePortalDirection(route);
        if (portalDirection == null) {
            return;
        }

        if (config.showPortalRouteArrows() && portalDirection.FirstMovementDirection != portalDirection.BoatDirection) {
            renderHeadingTriangle(graphics, boatLoc, portalDirection.FirstMovementDirection, config.portalRouteArrowColor(), 100, 18);
        }

        if (config.showPortalBoatArrows()) {
            renderHeadingTriangle(graphics, boatLoc, portalDirection.BoatDirection, config.portalBoatArrowColor(), 80, 18);
        }
    }

    private void highlightBoatTrueTile(Graphics2D graphics, WorldPoint worldPoint) {
        var localPoints = WorldPerspective.getInstanceLocalPointFromReal(client, worldPoint);
        if (localPoints == null || localPoints.isEmpty()) {
            return;
        }

        for (var lp : localPoints) {
            if (lp == null)
                continue;
            var poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly == null)
                continue;

            // Draw a translucent fill and a bold border so the tile is obvious
            Color fill = config.trueBoatTileFillColor();
            Color border = config.trueBoatTileBorderColor();
            Stroke oldStroke = graphics.getStroke();
            Composite oldComposite = graphics.getComposite();

            graphics.setColor(fill);
            graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
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

    private void renderHeadingTriangle(Graphics2D graphics, WorldPoint worldPoint, Directions direction, Color color, int offset, int size) {
        if (graphics == null || worldPoint == null || direction == null) {
            return;
        }

        if (color == null) {
            color = Color.WHITE;
        }

        if (offset <= 0) {
            offset = 10;
        }

        var lp = LocalPoint.fromWorld(client, worldPoint);

        if (lp == null) {
            log.info("LP is null for worldPoint {}", worldPoint);
            return;
        }

        var poly = Perspective.getCanvasTilePoly(client, lp);
        if (poly == null) {
            log.info("Poly is null for localPoint {}", lp);
            return;
        }

        Rectangle bounds = poly.getBounds();
        int centerX = bounds.x + bounds.width / 2;
        int centerY = bounds.y + bounds.height / 2;

        // Compute a world-space direction vector from the enum ordinal, then rotate by camera yaw.
        // Assume Directions is ordered starting at South and rotating counter-clockwise in 22.5° steps.
        int stepIndex = direction.ordinal();
        double angleRad = Math.toRadians(stepIndex * 22.5);
        // For stepIndex = 0 (South), we want wx=0, wy=-1, so use sin/cos with a 90° phase shift.
        double wx = -Math.sin(angleRad);
        double wy = -Math.cos(angleRad);

        int yaw = client.getCameraYaw() & 2047;
        double yawRad = yaw * (Math.PI / 1024.0);
        double baseAngle = Math.atan2(wx, wy);
        double total = yawRad + baseAngle;

        double dx = Math.sin(total);
        double dy = -Math.cos(total);

        int tipX = (int) Math.round(centerX + dx * offset);
        int tipY = (int) Math.round(centerY + dy * offset);

        int baseHalfWidth = size * 3 / 4;
        int baseBack = size;

        double leftAngle = Math.atan2(dy, dx) + Math.PI / 2.0;
        double rightAngle = Math.atan2(dy, dx) - Math.PI / 2.0;

        int baseCenterX = (int) Math.round(centerX + dx * (offset - baseBack));
        int baseCenterY = (int) Math.round(centerY + dy * (offset - baseBack));

        int leftX = (int) Math.round(baseCenterX + Math.cos(leftAngle) * baseHalfWidth);
        int leftY = (int) Math.round(baseCenterY + Math.sin(leftAngle) * baseHalfWidth);
        int rightX = (int) Math.round(baseCenterX + Math.cos(rightAngle) * baseHalfWidth);
        int rightY = (int) Math.round(baseCenterY + Math.sin(rightAngle) * baseHalfWidth);

        int[] xs = new int[] { tipX, leftX, rightX };
        int[] ys = new int[] { tipY, leftY, rightY };

        Color border = new Color(0, 0, 0, Math.min(255, color.getAlpha()));
        Stroke previous = graphics.getStroke();

        graphics.setColor(color);
        graphics.fillPolygon(xs, ys, 3);

        graphics.setColor(border);
        graphics.setStroke(new BasicStroke(1f));
        graphics.drawPolygon(xs, ys, 3);

        graphics.setStroke(previous);
    }

}
