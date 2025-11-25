package com.datbear.data;

public class PortalDirection {
    public int Index;
    public PortalColors Color;
    public Directions BoatDirection;
    public Directions FirstMovementDirection;

    public PortalDirection(int index, PortalColors color, Directions boatDirection, Directions firstMovementDirection) {
        Index = index;
        Color = color;
        BoatDirection = boatDirection;
        FirstMovementDirection = firstMovementDirection;
    }

}
