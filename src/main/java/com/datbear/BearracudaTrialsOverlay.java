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
    private SpriteManager spriteManager;

    @Inject
    private ModelOutlineRenderer modelOutlineRenderer;

    private Client client;
    private BearracudaTrialsPlugin plugin;
    private BearracudaTrialsConfig config;

    private final int MOTE_SPRITE_ID = 7075;
    private int nextMoteIndex = -1;

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

        var playerLoc = BoatLocation.fromLocal(client, player.getLocalLocation());
        if (playerLoc == null)
            return null;

        renderLastMenuCanvasWorldPointOutline(graphics);
        highlightTrimmableSails(graphics);

        var active = plugin.getActiveTrialRoute();
        if (active == null) {
            if (config.showDebugOverlay()) {
                renderDebugInfo(graphics, active);
            }
            return null;
        }

        var boatLoc = BoatLocation.fromLocal(client, player.getLocalLocation());

        highlightToadFlags(graphics, boatLoc);
        highlightCrates(graphics);
        highlightBoosts(graphics);
        renderWindMote(graphics);
        highlightTrialBoat(graphics);

        // Draw only the next up-to-5 waypoints (linear polyline beginning at the player's instance location).
        var visible = plugin.getVisibleActiveLineForPlayer(playerLoc, 5);
        if (visible.size() >= 2) {
            WorldLines.drawLinesOnWorld(graphics, client, visible, GREEN, playerLoc.getPlane());
        }

        // Render markers/labels for the next unvisited targets
        var nextIndices = plugin.getNextUnvisitedIndicesForActiveRoute(5);
        for (int idx : nextIndices) {
            if (active.Points == null || idx < 0 || idx >= active.Points.size())
                continue;
            var real = active.Points.get(idx);
            var wp = WorldPerspective.getInstanceWorldPointFromReal(client, client.getTopLevelWorldView(), real);
            if (wp == null)
                continue;
            var pts = WorldPerspective.worldToCanvasWithOffset(client, wp, wp.getPlane());
            if (pts.isEmpty())
                continue;
            var p = pts.get(0);

            renderLineDots(graphics, wp, GREEN, idx, p);
        }

        if (config.showDebugOverlay()) {
            renderDebugInfo(graphics, active);
        }
        return null;
    }

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
        // Highlight toad flags
        var toadGameObjects = plugin.getToadFlagToHighlight();
        if (toadGameObjects == null || toadGameObjects.isEmpty()) {
            return;
        }

        for (var toadGameObject : toadGameObjects) {
            var color = toadGameObject.getWorldLocation().distanceTo(player) < 15.2 ? GREEN : RED;
            modelOutlineRenderer.drawOutline(toadGameObject, 4, color, 2);
        }
    }

    private void highlightCrates(Graphics2D graphics) {
        var crates = plugin.getTrialCratesById();

        for (var crate : crates.values()) {
            modelOutlineRenderer.drawOutline(crate, 2, Color.YELLOW, 2);
        }
    }

    private void highlightBoosts(Graphics2D graphics) {
        var boosts = plugin.getTrialBoostsById();

        for (var boostList : boosts.values()) {
            for (var boost : boostList) {
                var poly = boost.getCanvasTilePoly();
                if (poly != null) {
                    OverlayUtil.renderPolygon(graphics, poly, Color.BLUE);
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
        if (!plugin.isNeedsTrim()) {
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

        OverlayUtil.renderPolygon(graphics, hull, GREEN);
    }

}
