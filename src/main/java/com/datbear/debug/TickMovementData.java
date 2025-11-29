package com.datbear.debug;

import java.util.Set;

import com.datbear.data.Directions;

import net.runelite.api.coords.WorldPoint;

public class TickMovementData {
    public int Tick;
    public WorldPoint StartPosition;
    public Directions StartHeading;
    public Set<WorldPoint> PointsVisited;

    public TickMovementData(int tick, WorldPoint startPosition, Directions startHeading, Set<WorldPoint> pointsVisited) {
        Tick = tick;
        StartPosition = startPosition;
        StartHeading = startHeading;
        PointsVisited = pointsVisited;
    }
}
