package com.datbear.data;

import java.util.List;
import java.util.Objects;

import net.runelite.api.coords.*;

public class TrialRoute {
    public TrialLocations Location;
    public TrialRanks Rank;
    public List<WorldPoint> Points;
    public int ToadsPerPickup;
    public List<ToadFlagColors> ToadOrder;

    public TrialRoute(TrialLocations location, TrialRanks rank, List<WorldPoint> points) {
        Location = location;
        Rank = rank;
        Points = points;
        ToadOrder = null;
    }

    public TrialRoute(TrialLocations location, TrialRanks rank, List<WorldPoint> points, int toadsPerPickup, List<ToadFlagColors> toadOrder) {
        this(location, rank, points);
        ToadsPerPickup = toadsPerPickup;
        ToadOrder = toadOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        TrialRoute that = (TrialRoute) o;
        return Location == that.Location && Rank == that.Rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Location, Rank);
    }
}
