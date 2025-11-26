/*
 * Copyright (c) 2021, Zoinkwiz <https://github.com/Zoinkwiz>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.datbear.overlay;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.WorldView;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static net.runelite.api.Constants.CHUNK_SIZE;

public class WorldPerspective {
    private final static int SW = 0;
    private final static int NW = 3;
    private final static int NE = 2;
    private final static int SE = 1;

    public static Collection<WorldPoint> toLocalInstanceFromReal(Client client, WorldPoint worldPoint) {
        if (!client.isInInstancedRegion()) {
            return Collections.singleton(worldPoint);
        }

        if (worldPoint == null)
            return Collections.singleton(null);

        List<WorldPoint> worldPoints = new ArrayList<>();

        int[][][] instanceTemplateChunks = client.getInstanceTemplateChunks();
        for (int z = 0; z < instanceTemplateChunks.length; ++z) {
            for (int x = 0; x < instanceTemplateChunks[z].length; ++x) {
                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y) {
                    int chunkData = instanceTemplateChunks[z][x][y];
                    int rotation = chunkData >> 1 & 0x3;
                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE) {
                        WorldPoint p = new WorldPoint(
                                client.getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                                client.getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                                z);
                        p = rotate(p, rotation);
                        if (p.isInScene(client)) {
                            worldPoints.add(p);
                        }
                    }
                }
            }
        }
        return worldPoints;
    }

    private static WorldPoint rotate(WorldPoint point, int rotation) {
        int chunkX = point.getX() & -CHUNK_SIZE;
        int chunkY = point.getY() & -CHUNK_SIZE;
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation) {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    public static List<LocalPoint> getInstanceLocalPointFromReal(Client client, WorldPoint wp) {
        List<WorldPoint> instanceWorldPoint = new ArrayList<>(WorldPerspective.toLocalInstanceFromReal(client, wp));

        List<LocalPoint> localPoints = new ArrayList<>();
        for (WorldPoint worldPoint : instanceWorldPoint) {
            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
            if (lp != null) {
                localPoints.add(lp);
            }
        }

        return localPoints;
    }

    public static WorldPoint getInstanceWorldPointFromReal(Client client, WorldPoint wp) {
        if (wp == null)
            return null;
        Collection<WorldPoint> points = WorldPerspective.toLocalInstanceFromReal(client, wp);

        if (points.isEmpty())
            return null;

        // If multiple instance candidates are returned, prefer the one that
        // matches the client's current plane. This reduces mismatches where a
        // real-world point could map to multiple instance planes and the
        // overlay ends up rendering a point on a different plane than the
        // player (causing apparent 'missing' points).
        int clientPlane = client.getPlane();
        WorldPoint fallback = null;
        for (WorldPoint point : points) {
            if (point == null)
                continue;

            // Prefer a mapping that is on the same plane as the client
            if (point.getPlane() == clientPlane) {
                if (point.isInScene(client)) {
                    return point;
                }
                // keep as preferred candidate if it's not yet chosen
                fallback = point;
            } else if (fallback == null) {
                // keep any first non-null candidate as a fallback
                fallback = point;
            }
        }

        return fallback;
    }

    /**
     * Like getInstanceWorldPointFromReal(Client, WorldPoint) but prefer a candidate
     * which maps into the supplied WorldView. This mirrors how DevTools renders
     * and allows mapping using the player's worldview for more accurate
     * on-screen coordinates.
     */
    public static WorldPoint getInstanceWorldPointFromReal(Client client, WorldView worldView, WorldPoint wp) {
        if (wp == null)
            return null;

        Collection<WorldPoint> points = WorldPerspective.toLocalInstanceFromReal(client, wp);

        if (points.isEmpty())
            return null;

        WorldPoint fallback = null;

        // First, prefer a mapping that produces a non-null LocalPoint from the
        // provided WorldView (i.e., it maps into the player's visible worldview)
        if (worldView != null) {
            for (WorldPoint point : points) {
                if (point == null) {
                    continue;
                }

                LocalPoint lp = LocalPoint.fromWorld(worldView, point);
                if (lp != null) {
                    // If it maps into that worldview it's a good match
                    return point;
                }
                if (fallback == null) {
                    fallback = point;
                }
            }
        }

        // Fall back to the regular selection logic if worldview-based selection
        // didn't return an immediate result.
        int clientPlane = client.getPlane();
        for (WorldPoint point : points) {
            if (point == null)
                continue;

            if (point.getPlane() == clientPlane) {
                if (point.isInScene(client)) {
                    return point;
                }
                fallback = point;
            } else if (fallback == null) {
                fallback = point;
            }
        }

        return fallback;
    }

    public static WorldPoint getRealWorldPointFromLocal(Client client, WorldPoint wp) {
        LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
        if (lp == null)
            return null;

        return WorldPoint.fromLocalInstance(client, lp);
    }

    public static Rectangle getWorldMapClipArea(Client client) {
        Widget widget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (widget == null) {
            return null;
        }

        return widget.getBounds();
    }

    public static Point mapWorldPointToGraphicsPoint(Client client, WorldPoint worldPoint) {
        var worldMap = client.getWorldMap();
        if (worldPoint == null)
            return null;
        if (!worldMap.getWorldMapData().surfaceContainsPosition(worldPoint.getX(), worldPoint.getY())) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();

        Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (map != null) {
            Rectangle worldMapRect = map.getBounds();

            int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / pixelsPerTile);
            int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);

            var worldMapPosition = worldMap.getWorldMapPosition();

            int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
            int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;
            int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

            int xGraphDiff = ((int) (xTileOffset * pixelsPerTile));
            int yGraphDiff = (int) (yTileOffset * pixelsPerTile);

            yGraphDiff -= pixelsPerTile - Math.ceil(pixelsPerTile / 2);
            xGraphDiff += pixelsPerTile - Math.ceil(pixelsPerTile / 2);

            yGraphDiff = worldMapRect.height - yGraphDiff;
            yGraphDiff += (int) worldMapRect.getY();
            xGraphDiff += (int) worldMapRect.getX();

            return new Point(xGraphDiff, yGraphDiff);
        }
        return null;
    }

    public static List<Point> worldToCanvasWithOffset(Client client, WorldPoint worldPoint, int zOffset) {
        List<Point> canvasPoints = new ArrayList<>();

        if (worldPoint == null) {
            return canvasPoints;
        }

        Collection<WorldPoint> instances = WorldPerspective.toLocalInstanceFromReal(client, worldPoint);
        for (WorldPoint wp : instances) {
            if (wp == null) {
                continue;
            }

            LocalPoint lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
            if (lp == null) {
                continue;
            }

            Point canvas = net.runelite.api.Perspective.localToCanvas(client, lp, wp.getPlane(), zOffset);
            if (canvas != null) {
                canvasPoints.add(canvas);
            }
        }

        return canvasPoints;
    }

    public static Point getMinimapPoint(Client client, WorldPoint start, WorldPoint destination) {
        var worldMapData = client.getWorldMap().getWorldMapData();
        if (worldMapData.surfaceContainsPosition(start.getX(), start.getY()) != worldMapData
                .surfaceContainsPosition(destination.getX(), destination.getY())) {
            return null;
        }

        int x = (destination.getX() - start.getX());
        int y = (destination.getY() - start.getY());

        float maxDistance = Math.max(Math.abs(x), Math.abs(y));
        x = x * 100;
        y = y * 100;
        x /= maxDistance;
        y /= maxDistance;

        Widget minimapDrawWidget;
        if (client.isResized()) {
            if (client.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 1) {
                minimapDrawWidget = client.getWidget(InterfaceID.ToplevelPreEoc.MINIMAP);
            } else {
                minimapDrawWidget = client.getWidget(InterfaceID.ToplevelOsrsStretch.MINIMAP);
            }
        } else {
            minimapDrawWidget = client.getWidget(InterfaceID.Toplevel.MINIMAP);
        }

        if (minimapDrawWidget == null) {
            return null;
        }

        final int angle = client.getCameraYawTarget() & 0x7FF;

        final int sin = net.runelite.api.Perspective.SINE[angle];
        final int cos = net.runelite.api.Perspective.COSINE[angle];

        final int xx = y * sin + cos * x >> 16;
        final int yy = sin * x - y * cos >> 16;

        Point loc = minimapDrawWidget.getCanvasLocation();
        int miniMapX = loc.getX() + xx + minimapDrawWidget.getWidth() / 2;
        int miniMapY = minimapDrawWidget.getHeight() / 2 + loc.getY() + yy;
        return new Point(miniMapX, miniMapY);
    }

    public static Polygon getZonePoly(Client client, Zone zone) {
        Polygon areaPoly = new Polygon();
        if (zone == null)
            return areaPoly;

        for (int x = zone.getMinX(); x < zone.getMaxX(); x++) {
            addToPoly(client, areaPoly, new WorldPoint(x, zone.getMaxY(), zone.getMinWorldPoint().getPlane()), NW);
        }

        addToPoly(client, areaPoly, new WorldPoint(zone.getMaxX(), zone.getMaxY(), zone.getMinWorldPoint().getPlane()), NW, NE, SE);

        for (int y = zone.getMaxY() - 1; y > zone.getMinY(); y--) {
            addToPoly(client, areaPoly, new WorldPoint(zone.getMaxX(), y, zone.getMinWorldPoint().getPlane()), SE);
        }

        addToPoly(client, areaPoly, new WorldPoint(zone.getMaxX(), zone.getMinY(), zone.getMinWorldPoint().getPlane()), SE, SW);

        for (int x = zone.getMaxX() - 1; x > zone.getMinX(); x--) {
            addToPoly(client, areaPoly, new WorldPoint(x, zone.getMinY(), zone.getMinWorldPoint().getPlane()), SW);
        }

        addToPoly(client, areaPoly, new WorldPoint(zone.getMinX(), zone.getMinY(), zone.getMinWorldPoint().getPlane()), SW, NW);

        for (int y = zone.getMinY() + 1; y < zone.getMaxY(); y++) {
            addToPoly(client, areaPoly, new WorldPoint(zone.getMinX(), y, zone.getMinWorldPoint().getPlane()), NW);
        }

        return areaPoly;
    }

    private static void addToPoly(Client client, Polygon areaPoly, WorldPoint wp, int... points) {
        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
        if (localPoint == null)
            return;

        Polygon poly = net.runelite.api.Perspective.getCanvasTilePoly(client, localPoint);
        if (poly != null) {
            for (int point : points) {
                areaPoly.addPoint(poly.xpoints[point], poly.ypoints[point]);
            }
        }
    }
}