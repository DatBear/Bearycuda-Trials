package com.datbear.data;

import java.util.List;
import java.util.Set;

public class ToadFlagGameObject {
    public Set<Integer> GameObjectIds;
    public ToadFlagColors Color;

    public ToadFlagGameObject(Set<Integer> gameObjectIds, ToadFlagColors color) {
        GameObjectIds = gameObjectIds;
        Color = color;
    }

    public static ToadFlagGameObject Green = new ToadFlagGameObject(Set.of(59121, 59124), ToadFlagColors.Green);
    public static ToadFlagGameObject Yellow = new ToadFlagGameObject(Set.of(59127, 59130), ToadFlagColors.Yellow);
    public static ToadFlagGameObject Red = new ToadFlagGameObject(Set.of(59133, 59136), ToadFlagColors.Red);
    public static ToadFlagGameObject Blue = new ToadFlagGameObject(Set.of(59139, 59142), ToadFlagColors.Blue);
    public static ToadFlagGameObject Orange = new ToadFlagGameObject(Set.of(59145, 59148), ToadFlagColors.Orange);
    public static ToadFlagGameObject Teal = new ToadFlagGameObject(Set.of(59151, 59154), ToadFlagColors.Teal);
    public static ToadFlagGameObject Pink = new ToadFlagGameObject(Set.of(59157, 59160), ToadFlagColors.Pink);
    public static ToadFlagGameObject White = new ToadFlagGameObject(Set.of(59163, 59166), ToadFlagColors.White);

    public static List<ToadFlagGameObject> All = List.of(
            Green,
            Yellow,
            Red,
            Blue,
            Orange,
            Teal,
            Pink,
            White);
}
