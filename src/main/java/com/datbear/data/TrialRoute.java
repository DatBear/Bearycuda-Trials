package com.datbear.data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.runelite.api.coords.*;

public class TrialRoute {
    public TrialLocations Location;
    public TrialRanks Rank;
    public List<WorldPoint> Points;
    public List<ToadFlagColors> ToadOrder;
    public List<Integer> WindMoteIndices;

    public TrialRoute(TrialLocations location, TrialRanks rank, List<WorldPoint> points) {
        Location = location;
        Rank = rank;
        Points = points;
        ToadOrder = Collections.emptyList();
        WindMoteIndices = Collections.emptyList();
    }

    public TrialRoute(TrialLocations location, TrialRanks rank, List<WorldPoint> points, List<ToadFlagColors> toadOrder, List<Integer> windMoteIndices) {
        this(location, rank, points);
        ToadOrder = toadOrder;
        WindMoteIndices = windMoteIndices;
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
