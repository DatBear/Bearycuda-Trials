package com.datbear;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import com.datbear.data.AllSails;
import com.datbear.data.ToadFlagColors;
import com.datbear.data.ToadFlagGameObject;
import com.datbear.data.TrialInfo;
import com.datbear.data.TrialLocations;
import com.datbear.data.TrialRanks;
import com.datbear.data.TrialRoute;
import com.google.common.base.Strings;
import com.google.inject.Provides;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.DynamicObject;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.OverheadTextChanged;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

// jubbly jive swordfish trial: https://www.youtube.com/watch?v=uPhcd84uVhY
// jubbly jive shark trial: https://www.youtube.com/watch?v=SKnL37OCWVQ

@Slf4j @PluginDescriptor(name = "Bearracuda Trials", description = "Show info to help with barracuda trials", tags = {
        "overlay", "sailing", "barracuda", "trials" //
})
public class BearracudaTrialsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private Notifier notifier;

    @Inject
    private BearracudaTrialsConfig config;

    @Inject
    private BearracudaTrialsOverlay overlay;

    @Inject
    private BearracudaTrialsPanel panel;

    @Provides
    BearracudaTrialsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BearracudaTrialsConfig.class);
    }

    private static final boolean isDebug = true;

    private final Set<String> CREW_MEMBER_NAMES = Set.of("Ex-Captain Siad", "Jobless Jim");

    final Set<Integer> TRIAL_CRATE_ANIMS = Set.of(8867);
    final Set<Integer> SPEED_BOOST_ANIMS = Set.of(13159, 13160);

    @Getter(AccessLevel.PACKAGE)
    private TrialInfo currentTrial = null;

    @Getter(AccessLevel.PACKAGE)
    private boolean needsTrim = false;

    private final String TRIM_AVAILABLE_TEXT = "you feel a gust of wind.";
    private final String TRIM_SUCCESS_TEXT = "you trim the sails";
    private final String TRIM_FAIL_TEXT = "the wind dies down";

    @Getter(AccessLevel.PACKAGE)
    private static final List<WorldPoint> TemporTantrumSwordfishBestLine = List.of(
            new WorldPoint(3035, 2922, 0), // start
            new WorldPoint(3025, 2911, 0),
            new WorldPoint(3017, 2900, 0),
            new WorldPoint(2996, 2896, 0),
            new WorldPoint(2994, 2882, 0),
            new WorldPoint(2979, 2866, 0),
            new WorldPoint(2983, 2839, 0),
            new WorldPoint(2979, 2827, 0),
            new WorldPoint(2990, 2809, 0),
            new WorldPoint(3001, 2787, 0),
            new WorldPoint(3013, 2769, 0),
            new WorldPoint(3022, 2762, 0),
            new WorldPoint(3039, 2760, 0),
            new WorldPoint(3056, 2763, 0),
            new WorldPoint(3054, 2763, 0),
            new WorldPoint(3057, 2792, 0),
            new WorldPoint(3065, 2811, 0),
            new WorldPoint(3078, 2827, 0),
            new WorldPoint(3078, 2864, 0),
            new WorldPoint(3084, 2875, 0),
            new WorldPoint(3091, 2887, 0),
            new WorldPoint(3072, 2916, 0),
            new WorldPoint(3052, 2920, 0),
            new WorldPoint(3035, 2922, 0) // end
    );

    @Getter(AccessLevel.PACKAGE)
    private static final List<WorldPoint> TemporTantrumSharkBestLine = List.of(
            new WorldPoint(3035, 2922, 0), // start
            new WorldPoint(3017, 2898, 0),
            new WorldPoint(3017, 2889, 0),
            new WorldPoint(3001, 2869, 0),
            new WorldPoint(3002, 2858, 0),
            new WorldPoint(3004, 2827, 0),
            new WorldPoint(3009, 2816, 0),
            new WorldPoint(3019, 2814, 0),
            new WorldPoint(3027, 2798, 0),
            new WorldPoint(3039, 2778, 0),
            new WorldPoint(3045, 2777, 0),
            new WorldPoint(3057, 2792, 0),
            new WorldPoint(3069, 2814, 0),
            new WorldPoint(3076, 2825, 0),
            new WorldPoint(3082, 2873, 0),
            new WorldPoint(3076, 2883, 0),
            new WorldPoint(3077, 2896, 0),
            new WorldPoint(3060, 2906, 0),
            new WorldPoint(3040, 2921, 0),
            new WorldPoint(3027, 2913, 0),
            new WorldPoint(3013, 2910, 0),
            new WorldPoint(2994, 2896, 0),
            new WorldPoint(2994, 2882, 0),
            new WorldPoint(2977, 2865, 0),
            new WorldPoint(2982, 2847, 0),
            new WorldPoint(2979, 2830, 0),
            new WorldPoint(2991, 2806, 0),
            new WorldPoint(3014, 2763, 0),
            new WorldPoint(3038, 2758, 0),
            new WorldPoint(3054, 2761, 0),
            new WorldPoint(3066, 2768, 0),
            new WorldPoint(3075, 2776, 0),
            new WorldPoint(3084, 2801, 0),
            new WorldPoint(3081, 2813, 0),
            new WorldPoint(3094, 2828, 0),
            new WorldPoint(3093, 2843, 0),
            new WorldPoint(3093, 2864, 0),
            new WorldPoint(3100, 2872, 0),
            new WorldPoint(3092, 2884, 0),
            new WorldPoint(3073, 2916, 0),
            new WorldPoint(3053, 2921, 0),
            new WorldPoint(3035, 2922, 0) // end
    );

    @Getter(AccessLevel.PACKAGE)
    private static final List<WorldPoint> TemporTantrumMarlinBestLine = List.of(
            new WorldPoint(3035, 2922, 0), // start
            new WorldPoint(3017, 2898, 0),
            new WorldPoint(3017, 2889, 0),
            new WorldPoint(3001, 2869, 0),
            new WorldPoint(3002, 2858, 0),
            new WorldPoint(3004, 2827, 0),
            new WorldPoint(3009, 2816, 0),
            new WorldPoint(3019, 2814, 0),
            new WorldPoint(3030, 2815, 0),
            new WorldPoint(3027, 2798, 0),
            new WorldPoint(3039, 2778, 0),
            new WorldPoint(3045, 2777, 0),
            new WorldPoint(3057, 2792, 0),
            new WorldPoint(3069, 2814, 0),
            new WorldPoint(3076, 2825, 0),
            new WorldPoint(3078, 2863, 0),
            new WorldPoint(3082, 2873, 0),
            new WorldPoint(3073, 2875, 0),
            new WorldPoint(3060, 2882, 0),
            new WorldPoint(3060, 2906, 0),
            new WorldPoint(3040, 2921, 0),
            new WorldPoint(3027, 2913, 0),
            new WorldPoint(3013, 2910, 0),
            new WorldPoint(2994, 2896, 0),
            new WorldPoint(2994, 2882, 0),
            new WorldPoint(2977, 2865, 0),
            new WorldPoint(2982, 2847, 0),
            new WorldPoint(2979, 2830, 0),
            new WorldPoint(2991, 2806, 0),
            new WorldPoint(3014, 2763, 0),
            new WorldPoint(3038, 2758, 0),
            new WorldPoint(3054, 2761, 0),
            new WorldPoint(3066, 2768, 0),
            new WorldPoint(3075, 2776, 0),
            new WorldPoint(3084, 2801, 0),
            new WorldPoint(3081, 2813, 0),
            new WorldPoint(3094, 2828, 0),
            new WorldPoint(3093, 2843, 0),
            new WorldPoint(3093, 2864, 0),
            new WorldPoint(3100, 2872, 0),
            new WorldPoint(3092, 2884, 0),
            new WorldPoint(3073, 2916, 0),
            new WorldPoint(3053, 2921, 0),
            new WorldPoint(3035, 2922, 0),
            new WorldPoint(3012, 2910, 0),
            new WorldPoint(2993, 2895, 0),
            new WorldPoint(2979, 2883, 0),
            new WorldPoint(2962, 2882, 0),
            new WorldPoint(2956, 2872, 0),
            new WorldPoint(2965, 2865, 0),
            new WorldPoint(2966, 2851, 0),
            new WorldPoint(2958, 2840, 0),
            new WorldPoint(2953, 2810, 0),
            new WorldPoint(2968, 2794, 0),
            new WorldPoint(2983, 2787, 0),
            new WorldPoint(2987, 2777, 0),
            new WorldPoint(3004, 2768, 0),
            new WorldPoint(3022, 2762, 0),
            new WorldPoint(3039, 2758, 0),
            new WorldPoint(3056, 2761, 0),
            new WorldPoint(3068, 2766, 0),
            new WorldPoint(3090, 2764, 0),
            new WorldPoint(3098, 2774, 0),
            new WorldPoint(3103, 2797, 0),
            new WorldPoint(3110, 2825, 0),
            new WorldPoint(3118, 2836, 0),
            new WorldPoint(3117, 2850, 0),
            new WorldPoint(3121, 2864, 0),
            new WorldPoint(3103, 2878, 0),
            new WorldPoint(3082, 2900, 0),
            new WorldPoint(3072, 2917, 0),
            new WorldPoint(3059, 2921, 0),

            new WorldPoint(3035, 2922, 0) // end
    );

    private static final List<WorldPoint> JubblySwordfishBestLine = List.of(
            new WorldPoint(2436, 3018, 0),
            new WorldPoint(2423, 3012, 0),
            new WorldPoint(2413, 3015, 0),
            new WorldPoint(2396, 3010, 0),
            new WorldPoint(2373, 3008, 0),
            new WorldPoint(2357, 2991, 0),
            new WorldPoint(2353, 2979, 0),
            new WorldPoint(2342, 2974, 0),
            new WorldPoint(2323, 2976, 0),
            new WorldPoint(2309, 2974, 0),
            new WorldPoint(2285, 2980, 0),
            new WorldPoint(2267, 2990, 0),
            new WorldPoint(2251, 2995, 0),
            new WorldPoint(2239, 3005, 0),
            new WorldPoint(2239, 3016, 0),
            new WorldPoint(2252, 3025, 0),
            new WorldPoint(2261, 3021, 0),
            new WorldPoint(2281, 2999, 0),
            new WorldPoint(2298, 3002, 0),
            new WorldPoint(2300, 3014, 0),
            new WorldPoint(2311, 3021, 0),
            new WorldPoint(2352, 3004, 0),
            new WorldPoint(2360, 2999, 0),
            new WorldPoint(2358, 2969, 0),
            new WorldPoint(2358, 2960, 0),
            new WorldPoint(2374, 2940, 0),
            new WorldPoint(2428, 2939, 0),
            new WorldPoint(2435, 2949, 0),
            new WorldPoint(2436, 2985, 0),
            new WorldPoint(2437, 2990, 0),
            new WorldPoint(2433, 3005, 0),
            new WorldPoint(2436, 3018, 0)//end
    );

    private static final List<ToadFlagColors> JubblySwordfishToadOrder = List.of(
            ToadFlagColors.Orange,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White//end
    );

    private static final List<WorldPoint> JubblySharkBestLine = List.of(
            new WorldPoint(2436, 3018, 0),
            new WorldPoint(2422, 3012, 0),
            new WorldPoint(2413, 3016, 0),
            new WorldPoint(2402, 3017, 0),
            new WorldPoint(2395, 3010, 0),
            new WorldPoint(2378, 3008, 0),
            new WorldPoint(2362, 2998, 0),
            new WorldPoint(2351, 2979, 0),
            new WorldPoint(2340, 2973, 0),
            new WorldPoint(2330, 2974, 0),
            new WorldPoint(2299, 2975, 0),
            new WorldPoint(2276, 2984, 0),
            new WorldPoint(2263, 2992, 0),
            new WorldPoint(2250, 2993, 0),
            new WorldPoint(2239, 3007, 0), // collect toad
            new WorldPoint(2240, 3016, 0),
            new WorldPoint(2250, 3023, 0),
            new WorldPoint(2253, 3025, 0),
            new WorldPoint(2261, 3021, 0),
            new WorldPoint(2278, 3001, 0),
            new WorldPoint(2295, 3000, 0), // click yellow outcrop
            new WorldPoint(2299, 3007, 0),
            new WorldPoint(2302, 3017, 0), // click red outcrop
            new WorldPoint(2310, 3021, 0),
            new WorldPoint(2329, 3016, 0),
            new WorldPoint(2339, 3004, 0),
            new WorldPoint(2345, 2990, 0),
            new WorldPoint(2359, 2974, 0),
            new WorldPoint(2358, 2965, 0),
            new WorldPoint(2365, 2948, 0), // click yellow outcrop
            new WorldPoint(2373, 2939, 0),
            new WorldPoint(2386, 2940, 0),
            new WorldPoint(2399, 2939, 0),
            new WorldPoint(2420, 2938, 0), // click green outcrop
            new WorldPoint(2426, 2936, 0),
            new WorldPoint(2434, 2949, 0),
            new WorldPoint(2434, 2969, 0),
            new WorldPoint(2438, 2989, 0),
            new WorldPoint(2438, 2989, 0), // click pink outcrop
            new WorldPoint(2434, 2998, 0),
            new WorldPoint(2432, 3021, 0),
            new WorldPoint(2413, 3026, 0), // click white outcrop
            new WorldPoint(2402, 3021, 0),
            new WorldPoint(2394, 3020, 0),
            new WorldPoint(2382, 3025, 0),
            new WorldPoint(2370, 3022, 0),
            new WorldPoint(2357, 3025, 0),
            new WorldPoint(2340, 3031, 0),
            new WorldPoint(2333, 3028, 0),
            new WorldPoint(2327, 3016, 0),
            new WorldPoint(2339, 3006, 0),
            new WorldPoint(2353, 3005, 0), // click blue outcrop
            new WorldPoint(2379, 2993, 0),
            new WorldPoint(2384, 2985, 0),
            new WorldPoint(2379, 2974, 0),
            new WorldPoint(2388, 2959, 0), // click orange outcrop
            new WorldPoint(2403, 2951, 0),
            new WorldPoint(2413, 2955, 0),
            new WorldPoint(2420, 2959, 0), // click teal outcrop
            new WorldPoint(2424, 2974, 0),
            new WorldPoint(2418, 2988, 0), // click pink outcrop
            new WorldPoint(2414, 2993, 0),
            new WorldPoint(2417, 3003, 0), //click white outcrop
            new WorldPoint(2436, 3023, 0) // end
    );

    private static final List<ToadFlagColors> JubblySharkToadOrder = List.of(
            ToadFlagColors.Yellow,
            ToadFlagColors.Red,
            ToadFlagColors.Orange,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White,
            ToadFlagColors.Blue,
            ToadFlagColors.Orange,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White //fin
    );

    private static final List<WorldPoint> JubblyMarlinBestLine = List.of(
            /*0*/new WorldPoint(2436, 3018, 0),
            /*1*/new WorldPoint(2424, 3025, 0),
            /*2*/new WorldPoint(2412, 3026, 0),
            /*3*/new WorldPoint(2405, 3023, 0),
            /*4*/new WorldPoint(2400, 3011, 0),
            /*5*/new WorldPoint(2396, 3009, 0),
            /*6*/new WorldPoint(2373, 3009, 0),
            /*7*/new WorldPoint(2350, 2977, 0),
            /*8*/new WorldPoint(2332, 2974, 0),
            /*9*/new WorldPoint(2303, 2975, 0),
            /*10*/new WorldPoint(2281, 2980, 0),
            /*11*/new WorldPoint(2265, 2991, 0),
            /*12*/new WorldPoint(2251, 2994, 0),
            /*13*/new WorldPoint(2248, 3000, 0),
            /*14*/new WorldPoint(2268, 3013, 0),
            /*15*/new WorldPoint(2280, 3000, 0),
            /*16*/new WorldPoint(2298, 3001, 0),
            /*17*/new WorldPoint(2302, 3017, 0),
            /*18*/new WorldPoint(2316, 3023, 0),
            /*19*/new WorldPoint(2350, 2981, 0),
            /*20*/new WorldPoint(2359, 2958, 0),
            /*21*/new WorldPoint(2375, 2936, 0),
            /*22*/new WorldPoint(2387, 2940, 0),
            /*23*/new WorldPoint(2420, 2939, 0),
            /*24*/new WorldPoint(2434, 2942, 0),
            /*25*/new WorldPoint(2435, 2967, 0),
            /*26*/new WorldPoint(2435, 2986, 0),
            /*27*/new WorldPoint(2437, 2991, 0),
            /*28*/new WorldPoint(2433, 3004, 0),
            /*29*/new WorldPoint(2435, 3010, 0),
            ///*29*/new WorldPoint(2440, 3009, 0),
            /*30*/new WorldPoint(2422, 3012, 0),
            /*31*/new WorldPoint(2415, 3000, 0),
            /*32*/new WorldPoint(2414, 2990, 0),
            /*33*/new WorldPoint(2423, 2978, 0),
            /*34*/new WorldPoint(2421, 2964, 0),
            /*35*/new WorldPoint(2417, 2957, 0),
            /*36*/new WorldPoint(2405, 2950, 0),
            /*37*/new WorldPoint(2390, 2957, 0),
            /*38*/new WorldPoint(2380, 2974, 0),
            /*39*/new WorldPoint(2384, 2985, 0),
            /*40*/new WorldPoint(2384, 2989, 0),
            /*41*/new WorldPoint(2369, 2997, 0),
            /*42*/new WorldPoint(2359, 2991, 0),
            /*43*/new WorldPoint(2350, 2977, 0),
            /*44*/new WorldPoint(2340, 2974, 0),
            /*45*/new WorldPoint(2305, 2974, 0),
            /*46*/new WorldPoint(2288, 2980, 0),
            /*47*/new WorldPoint(2278, 2981, 0),
            /*48*/new WorldPoint(2268, 2990, 0),
            /*49*/new WorldPoint(2256, 2992, 0),
            /*50*/new WorldPoint(2238, 3006, 0),
            /*51*/new WorldPoint(2242, 3020, 0),
            /*52*/new WorldPoint(2251, 3025, 0),
            /*53*/new WorldPoint(2257, 3024, 0),
            /*54*/new WorldPoint(2280, 2999, 0),
            /*55*/new WorldPoint(2292, 2997, 0), //wind mote HERE
            /*56*/new WorldPoint(2312, 2987, 0),
            /*57*/new WorldPoint(2324, 2984, 0),
            /*58*/new WorldPoint(2333, 2977, 0),
            /*59*/new WorldPoint(2335, 2954, 0),
            /*60*/new WorldPoint(2345, 2931, 0),
            /*61*/new WorldPoint(2365, 2928, 0),
            /*62*/new WorldPoint(2378, 2942, 0),
            /*63*/new WorldPoint(2395, 2939, 0),
            /*64*/new WorldPoint(2400, 2927, 0),
            /*65*/new WorldPoint(2417, 2924, 0),
            /*66*/new WorldPoint(2427, 2921, 0),
            /*67*/new WorldPoint(2442, 2927, 0),
            /*68*/new WorldPoint(2454, 2930, 0),
            /*69*/new WorldPoint(2469, 2953, 0),
            /*70*/new WorldPoint(2447, 2974, 0),
            /*71*/new WorldPoint(2447, 2986, 0), //wind mote HERE
            /*72*/new WorldPoint(2445, 3009, 0),
            /*73*/new WorldPoint(2437, 3010, 0),
            /*74*/new WorldPoint(2434, 3007, 0),
            /*75*/new WorldPoint(2403, 3017, 0),
            /*76*/new WorldPoint(2395, 3020, 0),
            /*77*/new WorldPoint(2387, 3020, 0),
            /*78*/new WorldPoint(2378, 3026, 0),
            /*79*/new WorldPoint(2371, 3022, 0),
            /*80*/new WorldPoint(2355, 3023, 0),
            /*81*/new WorldPoint(2343, 3031, 0),
            /*82*/new WorldPoint(2329, 3030, 0),
            /*83*/new WorldPoint(2313, 3045, 0),
            /*84*/new WorldPoint(2304, 3038, 0),
            /*85*/new WorldPoint(2313, 3025, 0),
            /*86*/new WorldPoint(2341, 3007, 0),
            /*87*/new WorldPoint(2355, 3005, 0),
            /*88*/new WorldPoint(2361, 3000, 0),
            /*89*/new WorldPoint(2390, 2986, 0),
            /*90*/new WorldPoint(2418, 2961, 0),
            /*91*/new WorldPoint(2430, 2953, 0), //shoot teal
            /*92*/new WorldPoint(2435, 2965, 0),
            /**/new WorldPoint(2433, 3000, 0),

            new WorldPoint(2436, 3023, 0) // end
    );

    private static final List<ToadFlagColors> JubblyMarlinToadOrder = List.of(
            ToadFlagColors.Yellow,
            ToadFlagColors.Red,
            ToadFlagColors.Orange,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White,
            ToadFlagColors.Teal,
            ToadFlagColors.Orange,
            ToadFlagColors.Blue,
            ToadFlagColors.Yellow,
            ToadFlagColors.Orange,
            ToadFlagColors.White,
            ToadFlagColors.Pink,
            ToadFlagColors.Red,
            ToadFlagColors.Blue,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White//end    
    );

    private static final List<Integer> JubblyMarlinWindMoteIndices = List.of(13, 55, 71, 89, 90);

    @Getter(AccessLevel.PACKAGE)
    private static final List<TrialRoute> AllTrialRoutes = List.of(
            new TrialRoute(TrialLocations.TemporTantrum, TrialRanks.Swordfish, TemporTantrumSwordfishBestLine),
            new TrialRoute(TrialLocations.TemporTantrum, TrialRanks.Shark, TemporTantrumSharkBestLine),
            new TrialRoute(TrialLocations.TemporTantrum, TrialRanks.Marlin, TemporTantrumMarlinBestLine),
            new TrialRoute(TrialLocations.JubblyJive, TrialRanks.Swordfish, JubblySwordfishBestLine, JubblySwordfishToadOrder, Collections.emptyList()),
            new TrialRoute(TrialLocations.JubblyJive, TrialRanks.Shark, JubblySharkBestLine, JubblySharkToadOrder, Collections.emptyList()),
            new TrialRoute(TrialLocations.JubblyJive, TrialRanks.Marlin, JubblyMarlinBestLine, JubblyMarlinToadOrder, JubblyMarlinWindMoteIndices));

    @Getter(AccessLevel.PACKAGE)
    private int lastVisitedIndex = -1;

    @Getter(AccessLevel.PACKAGE)
    private int toadsThrown = 0;

    private static final int VISIT_TOLERANCE = 10;

    public void markNextWaypointVisited(final WorldPoint player, final TrialRoute route, final int tolerance) {
        if (player == null || route == null || route.Points == null || route.Points.isEmpty()) {
            return;
        }
        int nextIdx = lastVisitedIndex + 1;
        if (nextIdx >= route.Points.size()) {
            return; // finished route
        }
        WorldPoint target = route.Points.get(nextIdx);
        if (target == null) {
            return;
        }
        double dist = Math.hypot(player.getX() - target.getX(), player.getY() - target.getY());
        if (dist <= tolerance) {
            lastVisitedIndex = nextIdx;
            log.info("Visited waypoint {} / {} for route {}", lastVisitedIndex, route.Points.size() - 1, route.Rank);
        }
    }

    public List<Integer> getNextIndicesAfterLastVisited(final TrialRoute route, final int limit) {
        if (route == null || route.Points == null || route.Points.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        int start = lastVisitedIndex;
        if (start >= route.Points.size()) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<>(limit);
        for (int i = start; i < route.Points.size() && out.size() < limit; i++) {
            out.add(i);
        }
        return out;
    }

    public List<WorldPoint> getVisibleLineForRoute(final WorldPoint player, final TrialRoute route, final int limit) {
        if (player == null || route == null) {
            return Collections.emptyList();
        }

        final List<Integer> nextIdx = getNextIndicesAfterLastVisited(route, limit);
        if (nextIdx.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> out = new ArrayList<>();
        for (int idx : nextIdx) {
            WorldPoint real = route.Points.get(idx);
            out.add(real);
        }
        return out;
    }

    private final Set<Integer> BOAT_WORLD_ENTITY_IDS = Set.of(12);
    // Cache of currently-spawning GameObjects keyed by object id. We only track
    // objects whose ids appear in any ToadFlagGameObject.All GameObjectIds set.
    private final Map<Integer, List<GameObject>> toadFlagsById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, GameObject> trialCratesById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, List<GameObject>> trialBoostsById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private GameObject sailGameObject = null;

    // last position where the menu was opened (canvas coordinates) — used for debug 'Copy tile worldpoint'
    // so we copy according to menu-open location instead of where the mouse is at click time.
    @Getter(AccessLevel.PACKAGE)
    private Point lastMenuCanvasPosition = null;
    @Getter(AccessLevel.PACKAGE)
    private WorldPoint lastMenuCanvasWorldPoint = null;

    @Getter(AccessLevel.PACKAGE)
    private int cargoItemCount = 0;

    @Override
    protected void startUp() {
        log.info("Bearracuda Trials Plugin started!");
        overlayManager.add(overlay);
        overlayManager.add(panel);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(panel);
        reset();
        log.info("BearracudaTrialsPlugin shutDown: panel removed and state reset.");
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }
        TrialRoute prevActiveRoute = getActiveTrialRoute();
        TrialInfo newTrialInfo = TrialInfo.getCurrent(client);
        if (newTrialInfo != null) {
            TrialRoute newActiveRoute = getActiveTrialRoute();
            if (prevActiveRoute != newActiveRoute) {
                resetRouteData();
                log.info("Active route changed; resetting lastVisitedIndex (prev={}, new={})", prevActiveRoute == null ? "null" : prevActiveRoute.Rank, newActiveRoute == null ? "null" : newActiveRoute.Rank);
            }
            updateToadsThrown(newTrialInfo);
        } else if (currentTrial != null) {
            log.info("No active trial detected - resetting lastVisitedIndex.");
            resetRouteData();
        }
        currentTrial = newTrialInfo;

        final var player = client.getLocalPlayer();
        var playerPoint = BoatLocation.fromLocal(client, player.getLocalLocation());

        if (playerPoint == null)
            return;

        TrialRoute active = getActiveTrialRoute();
        if (active != null) {
            markNextWaypointVisited(playerPoint, active, VISIT_TOLERANCE);
        }
    }

    private void resetRouteData() {
        lastVisitedIndex = -1;
        toadsThrown = 0;
    }

    private void updateToadsThrown(TrialInfo newTrialInfo) {
        if (currentTrial == null) {
            toadsThrown = 0;
            return;
        }
        if (newTrialInfo.ToadCount < currentTrial.ToadCount) {
            toadsThrown += 1;
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        var obj = event.getGameObject();
        if (obj == null) {
            return;
        }
        var id = obj.getId();

        var isToadFlag = ToadFlagGameObject.All.stream().anyMatch(t -> t.GameObjectIds.contains(id));
        if (isToadFlag) {
            toadFlagsById.computeIfAbsent(id, k -> new ArrayList<>()).add(obj);
        }
        var isSail = AllSails.GAMEOBJECT_IDS.contains(id);
        if (isSail) {
            sailGameObject = obj;
        }
        var renderable = obj.getRenderable();
        if (renderable != null) {
            if (renderable instanceof DynamicObject) {
                var dynObj = (DynamicObject) renderable;
                var anim = dynObj.getAnimation();
                var animId = anim != null ? anim.getId() : -1;
                if (TRIAL_CRATE_ANIMS.contains(animId)) {
                    trialCratesById.put(id, obj);
                } else if (SPEED_BOOST_ANIMS.contains(animId)) {
                    trialBoostsById.computeIfAbsent(id, k -> new ArrayList<>()).add(obj);
                }
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
        var obj = event.getGameObject();
        if (obj == null)
            return;
        var id = obj.getId();
        List<GameObject> cacheList = toadFlagsById.get(id);
        if (cacheList != null) {
            cacheList.removeIf(x -> x == null || x.getHash() == obj.getHash());
            if (cacheList.isEmpty()) {
                toadFlagsById.remove(id);
            }
            log.debug("Cached gameobject despawn id={} -> remaining={}", id, toadFlagsById.getOrDefault(id, Collections.emptyList()).size());
        }
    }

    @Subscribe
    public void onGroundObjectSpawned(GroundObjectSpawned event) {
        var groundObject = event.getGroundObject();
    }

    @Subscribe
    public void onGraphicsObjectCreated(GraphicsObjectCreated event) {
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        NPC npc = npcSpawned.getNpc();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (event.getGameState() == GameState.LOADING) {
            // on region changes the tiles and gameobjects get set to null
            reset();
        } else if (event.getGameState() == GameState.LOGIN_SCREEN) {

        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {

    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!isDebug) {
            return;
        }

        final var copyOption = "Copy worldpoint";
        final var copyTileOption = "Copy tile worldpoint";
        if (event.getMenuOption() != null && event.getMenuOption().equals(copyOption)) {
            var player = client.getLocalPlayer();
            if (player == null)
                return;

            var wp = BoatLocation.fromLocal(client, player.getLocalLocation());
            if (wp == null)
                return;

            var toCopy = String.format("new WorldPoint(%d, %d, %d),", wp.getX(), wp.getY(), wp.getPlane());

            try {
                var sel = new java.awt.datatransfer.StringSelection(toCopy);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                notifier.notify("Copied worldpoint to clipboard: " + toCopy);
            } catch (Exception ex) {
                log.warn("Failed to copy worldpoint to clipboard: {}", ex.toString());
            }

            event.consume();
        } else if (event.getMenuOption() != null && event.getMenuOption().equals(copyTileOption)) {
            var mouse = lastMenuCanvasPosition != null ? lastMenuCanvasPosition : client.getMouseCanvasPosition();
            lastMenuCanvasWorldPoint = null;

            try {
                var worldView = client.getTopLevelWorldView();
                var scene = worldView.getScene();
                var z = worldView.getPlane();
                var tiles = scene.getTiles();

                if (tiles != null && z >= 0 && z < tiles.length) {
                    var plane = tiles[z];
                    for (var x = 0; x < plane.length; x++) {
                        for (var y = 0; y < plane[x].length; y++) {
                            var tile = plane[x][y];
                            if (tile == null)
                                continue;
                            var lp = tile.getLocalLocation();
                            var poly = net.runelite.api.Perspective.getCanvasTilePoly(client, lp);
                            if (poly == null || mouse == null)
                                continue;
                            if (poly.contains(mouse.getX(), mouse.getY())) {
                                lastMenuCanvasWorldPoint = WorldPoint.fromLocalInstance(client, lp);
                                break;
                            }
                        }
                        if (lastMenuCanvasWorldPoint != null)
                            break;
                    }
                }
            } catch (Throwable ex) {
                // fall back to null
            }

            var worldPoint = lastMenuCanvasWorldPoint == null ? client.getLocalPlayer() == null ? null
                    : client.getLocalPlayer().getWorldLocation() : lastMenuCanvasWorldPoint;
            if (worldPoint == null)
                return;

            var toCopy = String.format("new WorldPoint(%d, %d, %d),", worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
            try {
                var sel = new java.awt.datatransfer.StringSelection(toCopy);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                notifier.notify("Copied tile worldpoint to clipboard: " + toCopy);
            } catch (Exception ex) {
                log.warn("Failed to copy tile worldpoint to clipboard: {}", ex.toString());
            }

            event.consume();
            // Clear the stored menu-open position so we don't reuse it on subsequent clicks
            lastMenuCanvasPosition = null;
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage e) {
        if (e.getType() != ChatMessageType.GAMEMESSAGE && e.getType() != ChatMessageType.SPAM) {
            //log.info("[CHAT-IGNORED] {}", e.getMessage());
            return;
        }

        String msg = e.getMessage().toLowerCase();
        //log.info("[CHAT] {}", msg);
        if (msg == null || msg.isEmpty()) {
            return;
        }
        if (msg.contains(TRIM_AVAILABLE_TEXT)) {
            needsTrim = true;
        } else if (msg.contains(TRIM_SUCCESS_TEXT) || msg.contains(TRIM_FAIL_TEXT)) {
            needsTrim = false;
        }
    }

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) {
        if (CREW_MEMBER_NAMES.contains(event.getActor().getName())) {
            event.getActor().setOverheadText(" ");
            return;
        }
    }

    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort e) {
        if (client.isMenuOpen()) {
            return;
        }

        var entries = swapMenuEntries(client.getMenuEntries());
        if (!isDebug) {
            return;
        }

        var p = client.getLocalPlayer();
        if (p == null) {
            return;
        }

        var hasCopyPlayerLocation = false;
        var hasCopyTileLocation = false;
        if (entries != null) {
            for (var entry : entries) {
                if (entry == null) {
                    continue;
                }
                if (entry.getOption().equals("Copy worldpoint")) {
                    hasCopyPlayerLocation = true;
                }
                if (entry.getOption().equals("Copy tile worldpoint")) {
                    hasCopyTileLocation = true;
                }
            }
        }

        var list = new ArrayList<MenuEntry>();

        if (!hasCopyTileLocation) {
            var copyTile = client.getMenu().createMenuEntry(-1).setOption("Copy tile worldpoint")
                    .setTarget("").setType(MenuAction.RUNELITE);
            list.add(copyTile);
        }

        if (!hasCopyPlayerLocation) {
            var copyPlayer = client.getMenu().createMenuEntry(-1).setOption("Copy worldpoint")
                    .setTarget("").setType(MenuAction.RUNELITE);
            list.add(copyPlayer);
        }

        // Capture the menu-open canvas position so we can later use that exact location for "Copy tile worldpoint" when the menu item is clicked.
        lastMenuCanvasPosition = client.getMouseCanvasPosition();
        if (entries != null) {
            list.addAll(Arrays.asList(entries));
        }

        client.setMenuEntries(list.toArray(new MenuEntry[0]));
    }

    /**
     * Move any menu entries whose option starts with "Start-previous" to the
     * end of the list while preserving relative order.
     */
    private List<String> FirstMenuEntries = List.of("start-previous");

    private MenuEntry[] swapMenuEntries(MenuEntry[] entries) {
        if (entries == null || entries.length == 0) {
            return entries;
        }
        var toMove = new ArrayList<MenuEntry>();
        var entriesAsList = new ArrayList<>(Arrays.asList(entries));
        var it = entriesAsList.iterator();
        while (it.hasNext()) {
            var menuEntry = it.next();
            if (menuEntry == null) {
                continue;
            }
            var opt = menuEntry.getOption();
            if (opt == null) {
                continue;
            }
            if (FirstMenuEntries.stream().anyMatch(x -> opt.toLowerCase().contains(x))) {
                toMove.add(menuEntry);
                it.remove();
            }
        }
        if (!toMove.isEmpty()) {
            entriesAsList.addAll(toMove);
        }
        entries = entriesAsList.toArray(new MenuEntry[0]);
        return entries;
    }

    private void reset() {
        // Clear runtime caches and tracked state on region change / shutdown
        toadFlagsById.clear();
        trialCratesById.clear();
        trialBoostsById.clear();
        sailGameObject = null;
    }

    public List<GameObject> getCachedGameObjectsForId(int id) {
        var list = toadFlagsById.get(id);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public List<GameObject> getCachedGameObjectsForIds(Set<Integer> ids) {
        var out = new ArrayList<GameObject>();
        if (ids == null || ids.isEmpty()) {
            return out;
        }
        for (int id : ids) {
            var list = toadFlagsById.get(id);
            if (list != null && !list.isEmpty()) {
                out.addAll(list);
            }
        }
        return out;
    }

    public TrialRoute getActiveTrialRoute() {
        if (currentTrial == null)
            return null;

        for (TrialRoute r : AllTrialRoutes) {
            if (r == null) {
                continue;
            }

            if (r.Location == currentTrial.Location && r.Rank == currentTrial.Rank) {
                return r;
            }
        }
        return null;
    }

    public List<WorldPoint> getVisibleActiveLineForPlayer(final WorldPoint player, final int limit) {
        var rt = getActiveTrialRoute();
        if (rt == null) {
            return Collections.emptyList();
        }

        // Route-agnostic: delegate to generic per-route logic
        return getVisibleLineForRoute(player, rt, limit);
    }

    public List<Integer> getNextUnvisitedIndicesForActiveRoute(final int limit) {
        var rt = getActiveTrialRoute();
        if (rt == null) {
            return Collections.emptyList();
        }
        return getNextIndicesAfterLastVisited(rt, limit);
    }

    public int getHighlightedToadFlagIndex() {
        var route = getActiveTrialRoute();
        if (route == null || currentTrial == null) {
            return 0;
        }
        return getHighlightedToadFlagIndex(route);
    }

    private int getHighlightedToadFlagIndex(TrialRoute route) {
        return toadsThrown < route.ToadOrder.size() ? toadsThrown : 0;
    }

    public List<GameObject> getToadFlagToHighlight() {
        if (currentTrial == null || currentTrial.Location != TrialLocations.JubblyJive || currentTrial.ToadCount <= 0) {
            return Collections.emptyList();
        }

        var route = getActiveTrialRoute();
        if (route == null || route.ToadOrder == null || route.ToadOrder.isEmpty()) {
            return Collections.emptyList();
        }

        var nextToadIdx = getHighlightedToadFlagIndex(route);
        if (nextToadIdx >= 0 && nextToadIdx < route.ToadOrder.size()) {
            var nextToadColor = route.ToadOrder.get(nextToadIdx);
            var nextToadGameObject = ToadFlagGameObject.getByColor(nextToadColor);
            List<GameObject> cached = getCachedGameObjectsForIds(nextToadGameObject.GameObjectIds);
            if (!cached.isEmpty()) {
                return cached;
            }
        }

        return Collections.emptyList();
    }

    public Collection<GameObject> getTrialBoatsToHighlight() {
        // var route = getActiveTrialRoute();
        // if (currentTrial == null || trialBoatsById.isEmpty() || route == null) {
        //     return Collections.emptyList();
        // }

        // if (route.Location == TrialLocations.JubblyJive && !currentTrial.HasToads) {
        //     return trialBoatsById.values();
        // }

        return Collections.emptyList();
    }

    private void logCrateAndBoostSpawns(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        if (gameObject == null) {
            return;
        }

        Renderable renderable = gameObject.getRenderable();
        if (!(renderable instanceof net.runelite.api.DynamicObject)) {
            return; // not an animating dynamic object
        }

        net.runelite.api.DynamicObject dyn = (net.runelite.api.DynamicObject) renderable;
        net.runelite.api.Animation anim = dyn.getAnimation();
        if (anim == null) {
            return;
        }

        final int animId = anim.getId();
        final boolean isCrateAnim = TRIAL_CRATE_ANIMS.contains(animId);
        final boolean isSpeedAnim = SPEED_BOOST_ANIMS.contains(animId);

        if (!isCrateAnim && !isSpeedAnim) {
            return; // ignore unrelated animations
        }

        WorldPoint wp = null;
        try {
            wp = gameObject.getWorldLocation();
        } catch (Exception ex) {
            log.info(
                    "GameObject (id={}) spawned but getWorldLocation threw: {}",
                    gameObject.getId(),
                    ex.toString());
        }

        ObjectComposition objectComposition = client.getObjectDefinition(gameObject.getId());
        if (objectComposition.getImpostorIds() == null) {
            String name = objectComposition.getName();
            log.info("Gameobject (id={}) spawned with name='{}'", gameObject.getId(), name);
            if (Strings.isNullOrEmpty(name) || name.equals("null")) {
                // name has changed?
                return;
            }
        }

        var minLocation = gameObject.getSceneMinLocation();
        var poly = gameObject.getCanvasTilePoly();

        String type = isCrateAnim ? "CRATE" : "SPEED BOOST";
        if (wp != null) {
            if (isCrateAnim) {
                log.info("[SPAWN] {} -> GameObject id={} world={} (hash={}) minLocation={} poly={}", type, animId, gameObject.getId(), wp, gameObject.getHash(), minLocation, poly);
            }

        } else {
            log.info("[SPAWN] {} -> GameObject id={} (no world point available)", type, gameObject.getId());
        }
    }

}
