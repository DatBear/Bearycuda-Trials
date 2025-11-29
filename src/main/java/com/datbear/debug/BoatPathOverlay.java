package com.datbear.debug;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.Set;

import com.datbear.BearycudaTrialsConfig;
import com.datbear.BearycudaTrialsPlugin;
import com.datbear.data.Directions;
import com.datbear.debug.BoatPathHelper;
import com.datbear.debug.TickMovementData;
import com.datbear.overlay.WorldPerspective;
import com.google.inject.Inject;

import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

public class BoatPathOverlay extends Overlay {
    private Client client;
    private BearycudaTrialsPlugin plugin;
    private BearycudaTrialsConfig config;

    @Inject
    public BoatPathOverlay(Client client, BearycudaTrialsPlugin plugin, BearycudaTrialsConfig config) {
        super();
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
    }

    private static final Set<Color> TickColors = Set.of(
            Color.RED,
            Color.GREEN,
            Color.BLUE,
            Color.YELLOW,
            Color.ORANGE,
            Color.CYAN);

    public BoatPathOverlay() {
        super();
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (client == null || graphics == null) {
            return null;
        }

        if (plugin.getBoardedBoat() == 0) {
            return null;
        }
        if (!config.enableBoatPathDebug()) {
            return null;
        }

        if (client.getGameState() != GameState.LOGGED_IN) {
            return null;
        }

        if (client.getTickCount() <= 0) {
            return null;
        }

        var tickColorOrder = TickColors.toArray(new Color[0]);
        if (tickColorOrder.length == 0) {
            return null;
        }

        var currentTick = client.getTickCount();
        var ticksToRender = 6;

        for (var offset = 0; offset < ticksToRender; offset++) {
            var targetTick = currentTick - offset;
            if (targetTick < 0) {
                continue;
            }

            var tickData = BoatPathHelper.GetTickData(targetTick);
            if (tickData == null || tickData.PointsVisited == null || tickData.PointsVisited.isEmpty()) {
                continue;
            }

            var outlineColor = tickColorOrder[offset % tickColorOrder.length];
            var insetEvenTicks = (tickData.Tick % 2) == 0;
            drawVisitedTileOutlines(graphics, tickData, outlineColor, insetEvenTicks);
            renderTickLabel(graphics, tickData, outlineColor);
        }

        return null;
    }

    private void drawVisitedTileOutlines(Graphics2D graphics, TickMovementData tickData, Color outlineColor, boolean insetOutline) {
        if (tickData == null || tickData.PointsVisited == null || tickData.PointsVisited.isEmpty()) {
            return;
        }

        var previousStroke = graphics.getStroke();
        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(outlineColor);

        for (WorldPoint visitedPoint : tickData.PointsVisited) {
            if (visitedPoint == null) {
                continue;
            }

            var tilePolygon = getCanvasPolygonForWorldPoint(visitedPoint);
            if (tilePolygon != null) {
                if (insetOutline) {
                    var insetPolygon = insetPolygon(tilePolygon, 3);
                    if (insetPolygon != null) {
                        tilePolygon = insetPolygon;
                    }
                }
                graphics.draw(tilePolygon);
            }
        }

        graphics.setStroke(previousStroke);
    }

    private void renderTickLabel(Graphics2D graphics, TickMovementData tickData, Color textColor) {
        if (tickData == null || tickData.StartPosition == null) {
            return;
        }

        var startTile = getCanvasPolygonForWorldPoint(tickData.StartPosition);
        if (startTile == null) {
            return;
        }

        var tileBounds = startTile.getBounds();
        var textX = tileBounds.x + tileBounds.width + 6;
        var textY = tileBounds.y + (tileBounds.height / 2);

        var heading = tickData.StartHeading;
        var headingLabel = heading != null ? heading.name() : "UNKNOWN";
        var label = String.format("tick %d %s", tickData.Tick, headingLabel);

        graphics.setColor(Color.BLACK);
        graphics.drawString(label, textX + 1, textY + 1);
        graphics.setColor(textColor);
        graphics.drawString(label, textX, textY);
    }

    private Polygon getCanvasPolygonForWorldPoint(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return null;
        }

        var localPoints = WorldPerspective.getInstanceLocalPointFromReal(client, worldPoint);
        if (localPoints == null || localPoints.isEmpty()) {
            return null;
        }

        for (LocalPoint localPoint : localPoints) {
            if (localPoint == null) {
                continue;
            }
            var polygon = Perspective.getCanvasTilePoly(client, localPoint);
            if (polygon != null) {
                return polygon;
            }
        }

        return null;
    }

    private Polygon insetPolygon(Polygon polygon, int insetPixels) {
        if (polygon == null || insetPixels <= 0) {
            return polygon;
        }

        double centerX = 0;
        double centerY = 0;
        int nPoints = polygon.npoints;
        if (nPoints == 0) {
            return polygon;
        }

        for (int i = 0; i < nPoints; i++) {
            centerX += polygon.xpoints[i];
            centerY += polygon.ypoints[i];
        }
        centerX /= nPoints;
        centerY /= nPoints;

        var insetPoly = new Polygon();
        for (int i = 0; i < nPoints; i++) {
            double dx = polygon.xpoints[i] - centerX;
            double dy = polygon.ypoints[i] - centerY;
            double distance = Math.hypot(dx, dy);
            if (distance == 0) {
                continue;
            }
            double scale = Math.max((distance - insetPixels) / distance, 0);
            int newX = (int) Math.round(centerX + dx * scale);
            int newY = (int) Math.round(centerY + dy * scale);
            insetPoly.addPoint(newX, newY);
        }

        return insetPoly.npoints > 0 ? insetPoly : polygon;
    }
}
