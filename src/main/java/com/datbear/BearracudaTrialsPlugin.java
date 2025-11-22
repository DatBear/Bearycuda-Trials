package com.datbear;

import com.datbear.data.*;
import com.datbear.overlay.*;
import com.google.common.base.Strings;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import javax.inject.Inject;

import java.util.*;

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

    private final Set<Integer> CARGO_CONTAINER_IDS = Set.of(33733);
    private final Set<Integer> SALVAGING_HOOK_SLOTS = Set.of(14);

    private final Set<String> CREW_MEMBER_NAMES = Set.of("Ex-Captain Siad", "Jobless Jim");

    private TrialInfo currentTrial = null;

    @Getter(AccessLevel.PACKAGE)
    private static final List<WorldPoint> TemporTantrumSwordfishBestLine = List.of(
            new WorldPoint(3034, 2918, 0), // start
            new WorldPoint(3013, 2910, 0), // boost (near start, closing arc)

            // head north-west -> north arc
            new WorldPoint(2994, 2891, 0), // crate
            new WorldPoint(2978, 2866, 0), // crate
            new WorldPoint(2981, 2848, 0), // crate

            // western / inner arc
            // new WorldPoint(2954, 2819, 0),// crate

            new WorldPoint(2978, 2828, 0), // crate
            new WorldPoint(2990, 2808, 0), // crate
            new WorldPoint(3001, 2788, 0), // crate

            // south / southwest side
            new WorldPoint(3012, 2768, 0), // crate
            new WorldPoint(3037, 2758, 0), // boost (southern arc)

            // east / southeast side (boosts)
            new WorldPoint(3054, 2761, 0), // boost
            new WorldPoint(3057, 2792, 0), // crate
            new WorldPoint(3065, 2811, 0), // crate
            new WorldPoint(3077, 2825, 0), // crate
            new WorldPoint(3074, 2834, 0), // boost

            // east -> northeast -> north
            new WorldPoint(3078, 2863, 0), // crate
            new WorldPoint(3082, 2875, 0), // crate
            new WorldPoint(3084, 2896, 0), // crate
            new WorldPoint(3049, 2918, 0), // crate

            new WorldPoint(3034, 2918, 0) // return to start
    );

    @Getter(AccessLevel.PACKAGE)
    private static final List<WorldPoint> TemporTantrumMarlinBestLine = List.of(
            new WorldPoint(3034, 2918, 0), // start
            new WorldPoint(3027, 2913, 0), // boost
            new WorldPoint(3017, 2899, 0), // boost
            new WorldPoint(3018, 2889, 0),
            new WorldPoint(3014, 2885, 0),
            new WorldPoint(3002, 2868, 0),
            new WorldPoint(3002, 2868, 0),
            new WorldPoint(3004, 2834, 0),
            new WorldPoint(3006, 2819, 0),
            new WorldPoint(3020, 2814, 0),
            new WorldPoint(3030, 2815, 0),
            new WorldPoint(3028, 2789, 0),
            new WorldPoint(3045, 2775, 0), // click rum boat here
            new WorldPoint(3057, 2792, 0),
            new WorldPoint(3066, 2811, 0),
            new WorldPoint(3078, 2827, 0),
            new WorldPoint(3077, 2862, 0),
            new WorldPoint(3082, 2875, 0),
            new WorldPoint(3060, 2883, 0),
            new WorldPoint(3061, 2901, 0),
            new WorldPoint(3027, 2913, 0), // lap 1 complete
            new WorldPoint(3013, 2910, 0),
            new WorldPoint(2995, 2896, 0),
            new WorldPoint(2978, 2866, 0),
            new WorldPoint(2981, 2848, 0),
            new WorldPoint(2978, 2828, 0),
            new WorldPoint(2987, 2818, 0),
            new WorldPoint(2990, 2808, 0),
            new WorldPoint(3001, 2787, 0),
            new WorldPoint(3012, 2768, 0),
            new WorldPoint(3020, 2762, 0),
            new WorldPoint(3037, 2760, 0),
            new WorldPoint(3054, 2762, 0),
            new WorldPoint(3077, 2776, 0), // 32
            new WorldPoint(3082, 2801, 0),
            new WorldPoint(3081, 2813, 0),
            new WorldPoint(3093, 2825, 0),
            new WorldPoint(3093, 2835, 0),
            new WorldPoint(3094, 2862, 0),
            new WorldPoint(3099, 2875, 0),
            new WorldPoint(3073, 2916, 0),
            new WorldPoint(3053, 2920, 0),
            new WorldPoint(3013, 2910, 0),
            new WorldPoint(2981, 2884, 0),
            new WorldPoint(2963, 2882, 0),
            new WorldPoint(2959, 2870, 0),
            new WorldPoint(2968, 2862, 0),
            new WorldPoint(2960, 2842, 0),
            new WorldPoint(2955, 2831, 0),
            new WorldPoint(2953, 2809, 0),
            new WorldPoint(2967, 2794, 0),
            new WorldPoint(2984, 2787, 0),
            new WorldPoint(2988, 2777, 0),
            new WorldPoint(3020, 2762, 0),
            new WorldPoint(3037, 2758, 0),
            new WorldPoint(3088, 2766, 0),
            new WorldPoint(3097, 2774, 0),
            new WorldPoint(3109, 2825, 0),
            new WorldPoint(3120, 2866, 0),
            new WorldPoint(3105, 2876, 0),
            new WorldPoint(3082, 2900, 0),
            new WorldPoint(3072, 2915, 0),
            new WorldPoint(3059, 2921, 0),
            new WorldPoint(3037, 2926, 0),
            new WorldPoint(3034, 2918, 0) // return to start
    );

    @Getter(AccessLevel.PACKAGE)
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
            ToadFlagColors.Pickup,
            ToadFlagColors.Yellow,
            ToadFlagColors.Red,
            ToadFlagColors.Yellow,
            ToadFlagColors.Green,
            ToadFlagColors.Pink,
            ToadFlagColors.White,
            ToadFlagColors.Blue,
            ToadFlagColors.Orange,
            ToadFlagColors.Teal,
            ToadFlagColors.Pink,
            ToadFlagColors.White //fin
    );

    @Getter(AccessLevel.PACKAGE)
    private static final List<TrialRoute> AllTrialRoutes = List.of(
            new TrialRoute(TrialLocations.TemporTantrum, TrialRanks.Swordfish, TemporTantrumSwordfishBestLine),
            new TrialRoute(TrialLocations.TemporTantrum, TrialRanks.Marlin, TemporTantrumMarlinBestLine),
            new TrialRoute(TrialLocations.JubblyJive, TrialRanks.Shark, JubblySharkBestLine, 12, JubblySharkToadOrder));

    @Getter(AccessLevel.PACKAGE)
    private int lastVisitedIndex = -1;

    private static final int VISIT_TOLERANCE = 6;

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
        int start = lastVisitedIndex + 1;
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
        out.add(player);
        for (int idx : nextIdx) {
            WorldPoint real = route.Points.get(idx);
            out.add(real);
        }
        return out;
    }

    private final Set<Integer> TRIAL_CRATE_PROJECTILE_IDS = Set.of(3497);// crates spawn this projectile when hit

    private final Set<Integer> TRIAL_RUM_PROJECTILE_IDS = Set.of(3501);// rum boats spawn this projectile when clicked
    private final Set<Integer> SPEED_BOOST_IDS = Set.of(60352);

    private final Set<Integer> OBSTACLE_IDS = Set.of(60442, 60444, 60443, 60468, 60452);

    private final Set<Integer> TRIAL_CRATE_ANIMS = Set.of(8867);
    private final Set<Integer> SPEED_BOOST_ANIMS = Set.of(13159);

    // last position where the menu was opened (canvas coordinates) — used for debug 'Copy tile worldpoint'
    // so we copy according to menu-open location instead of where the mouse is at click time.
    private volatile Point lastMenuCanvasPosition = null;

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
        Item[] items = event.getItemContainer().getItems();
        if (CARGO_CONTAINER_IDS.stream().anyMatch(id -> id == event.getContainerId())) {
            cargoItemCount = (int) Arrays.stream(items).filter(x -> x.getId() != -1).count();
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }
        TrialRoute prevActive = getActiveTrialRoute();
        TrialInfo trialInfo = TrialInfo.getCurrent(client);
        if (trialInfo != null) {
            currentTrial = trialInfo;
            TrialRoute newActive = getActiveTrialRoute();
            if (prevActive != newActive) {
                // Route actually changed: reset single index tracker
                lastVisitedIndex = -1;
                log.info("Active route changed; resetting lastVisitedIndex (prev={}, new={})", prevActive == null ? "null" : prevActive.Rank, newActive == null ? "null" : newActive.Rank);
            }
        }

        final var player = client.getLocalPlayer();
        var playerPoint = BoatLocation.fromLocal(client, player.getLocalLocation());

        if (playerPoint == null)
            return;

        TrialRoute active = getActiveTrialRoute();
        if (active != null) {
            markNextWaypointVisited(playerPoint, active, VISIT_TOLERANCE);
        }
    }

    public TrialRoute getActiveTrialRoute() {
        if (currentTrial == null)
            return null;

        for (TrialRoute r : AllTrialRoutes) {
            if (r == null)
                continue;
            if (r.Location == currentTrial.Location && r.Rank == currentTrial.Rank)
                return r;
        }
        return null;
    }

    public List<WorldPoint> getVisibleActiveLineForPlayer(final WorldPoint player, final int limit) {
        var rt = getActiveTrialRoute();
        if (rt == null)
            return Collections.emptyList();

        // Route-agnostic: delegate to generic per-route logic
        return getVisibleLineForRoute(player, rt, limit);
    }

    public List<Integer> getNextUnvisitedIndicesForActiveRoute(final WorldPoint player, final int limit) {
        var rt = getActiveTrialRoute();
        if (rt == null) {
            return Collections.emptyList();
        }
        return getNextIndicesAfterLastVisited(rt, limit);
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        // logCrateBoostSpawns(event);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event) {
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
            // on region changes the tiles get set to null
            reset();
        } else if (event.getGameState() == GameState.LOGIN_SCREEN) {

        }
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {

    }

    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() != ChatMessageType.SPAM
                && chatMessage.getType() != ChatMessageType.GAMEMESSAGE)
            return;

        String msg = chatMessage.getMessage();
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked event) {
        if (!isDebug) {
            return;
        }

        final String copyOption = "Copy worldpoint";
        final String copyTileOption = "Copy tile worldpoint";
        if (event.getMenuOption() != null && event.getMenuOption().equals(copyOption)) {
            var player = client.getLocalPlayer();
            if (player == null)
                return;

            WorldPoint wp = BoatLocation.fromLocal(client, player.getLocalLocation());
            if (wp == null)
                return;

            String toCopy = String
                    .format("new WorldPoint(%d, %d, %d),", wp.getX(), wp.getY(), wp.getPlane());

            try {
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(
                        toCopy);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                notifier.notify("Copied worldpoint to clipboard: " + toCopy);
            } catch (Exception ex) {
                log.warn("Failed to copy worldpoint to clipboard: {}", ex.toString());
            }

            // mark event consumed so other handlers don't process it
            event.consume();
        } else if (event.getMenuOption() != null && event.getMenuOption().equals(copyTileOption)) {
            // Find the scene tile whose canvas polygon contains the menu
            // position
            // Use the stored menu-open position; fall back to current mouse pos
            Point mouse = lastMenuCanvasPosition != null ? lastMenuCanvasPosition
                    : client.getMouseCanvasPosition();
            WorldPoint tileWp = null;

            try {
                WorldView wv = client.getTopLevelWorldView();
                Scene scene = wv.getScene();
                int z = wv.getPlane();
                Tile[][][] tiles = scene.getTiles();

                if (tiles != null && z >= 0 && z < tiles.length) {
                    Tile[][] plane = tiles[z];
                    for (int x = 0; x < plane.length; x++) {
                        for (int y = 0; y < plane[x].length; y++) {
                            Tile tile = plane[x][y];
                            if (tile == null)
                                continue;
                            var lp = tile.getLocalLocation();
                            var poly = net.runelite.api.Perspective.getCanvasTilePoly(client, lp);
                            if (poly == null || mouse == null)
                                continue;
                            if (poly.contains(mouse.getX(), mouse.getY())) {
                                tileWp = WorldPoint.fromLocalInstance(client, lp);
                                break;
                            }
                        }
                        if (tileWp != null)
                            break;
                    }
                }
            } catch (Throwable ex) {
                // fall back to null
            }

            WorldPoint wp = tileWp == null ? client.getLocalPlayer() == null ? null
                    : client.getLocalPlayer().getWorldLocation() : tileWp;
            if (wp == null)
                return;

            String toCopy = String
                    .format("new WorldPoint(%d, %d, %d),", wp.getX(), wp.getY(), wp.getPlane());
            try {
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(
                        toCopy);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                notifier.notify("Copied tile worldpoint to clipboard: " + toCopy);
            } catch (Exception ex) {
                log.warn("Failed to copy tile worldpoint to clipboard: {}", ex.toString());
            }

            // mark event consumed so other handlers don't process it
            event.consume();
            // Clear the stored menu-open position so we don't reuse it on
            // subsequent clicks
            lastMenuCanvasPosition = null;
        }
    }

    @Subscribe
    public void onOverheadTextChanged(OverheadTextChanged event) {
        if (CREW_MEMBER_NAMES.contains(event.getActor().getName())) {
            event.getActor().setOverheadText(" ");
            return;
        }
        // log.info("[OVERHEAD] {} says {}", event.getActor().getName(),
        // event.getOverheadText());
    }

    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort e) {
        if (client.isMenuOpen()) {
            return;
        }
        if (!isDebug) {
            return;
        }

        Player p = client.getLocalPlayer();
        if (p == null) {
            return;
        }

        // Make sure we don't add duplicates — but allow adding missing entries
        // separately so both options can be present.
        MenuEntry[] entries = client.getMenuEntries();
        boolean hasCopyPlayer = false;
        boolean hasCopyTile = false;
        if (entries != null) {
            for (MenuEntry me : entries) {
                if (me == null)
                    continue;
                if ("Copy worldpoint".equals(me.getOption())) {
                    hasCopyPlayer = true;
                }
                if ("Copy tile worldpoint".equals(me.getOption())) {
                    hasCopyTile = true;
                }
            }
        }

        var list = new ArrayList<MenuEntry>();

        // Create tile copy entry if missing
        if (!hasCopyTile) {
            MenuEntry copyTile = client.getMenu().createMenuEntry(-1)
                    .setOption("Copy tile worldpoint").setTarget("").setType(MenuAction.RUNELITE);
            list.add(copyTile);
        }

        // Create player copy entry if missing
        if (!hasCopyPlayer) {
            MenuEntry copyPlayer = client.getMenu().createMenuEntry(-1).setOption("Copy worldpoint")
                    .setTarget("").setType(MenuAction.RUNELITE);
            list.add(copyPlayer);
        }

        // Capture the menu-open canvas position so we can later use that exact
        // location for "Copy tile worldpoint" when the menu item is clicked.
        lastMenuCanvasPosition = client.getMouseCanvasPosition();
        // Append existing entries after our added options
        if (entries != null) {
            list.addAll(Arrays.asList(entries));
        }

        client.setMenuEntries(list.toArray(new MenuEntry[0]));
    }

    private void SwapMenu(Menu menu) {
        // var entries = menu.getMenuEntries();
        // menu.setMenuEntries(entries);
    }

    private void reset() {
    }

    private void logCrateBoostSpawns(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        if (gameObject == null) {
            return;
        }

        // Check the spawned object's animation via the renderable. We're
        // looking for
        // the crate/speed boost animation ids (TRIAL_CRATE_ANIM /
        // SPEED_BOOST_ANIM).
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
