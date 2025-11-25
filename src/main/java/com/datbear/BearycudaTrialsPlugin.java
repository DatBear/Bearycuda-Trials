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
import com.datbear.data.PortalDirection;
import com.datbear.data.ToadFlagGameObject;
import com.datbear.data.TrialInfo;
import com.datbear.data.TrialLocations;
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
import net.runelite.api.ObjectComposition;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.Notifier;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j @PluginDescriptor(name = "Bearycuda Trials", description = "Show info to help with barracuda trials", tags = { "overlay", "sailing", "barracuda", "trials" })
public class BearycudaTrialsPlugin extends Plugin {
    @Inject
    private Client client;

    @Inject
    private OverlayManager overlayManager;
    @Inject
    private Notifier notifier;

    @Inject
    private BearycudaTrialsConfig config;

    @Inject
    private BearycudaTrialsOverlay overlay;

    @Inject
    private BearycudaTrialsPanel panel;

    @Provides
    BearycudaTrialsConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(BearycudaTrialsConfig.class);
    }

    private final Set<Integer> TRIAL_CRATE_ANIMS = Set.of(8867);
    private final Set<Integer> SPEED_BOOST_ANIMS = Set.of(13159, 13160, 13163);

    private final String TRIM_AVAILABLE_TEXT = "you feel a gust of wind.";
    private final String TRIM_SUCCESS_TEXT = "you trim the sails";
    private final String TRIM_FAIL_TEXT = "the wind dies down";

    private final String MENU_OPTION_START_PREVIOUS_RANK = "start-previous";
    private final String MENU_OPTION_QUICK_RESET = "quick-reset";
    private final String MENU_OPTION_STOP_NAVIGATING = "stop-navigating";

    private static final int VISIT_TOLERANCE = 10;

    private List<String> FirstMenuEntries = new ArrayList<String>();
    private List<String> RemoveMenuEntriesDuringTrial = new ArrayList<String>();

    @Getter(AccessLevel.PACKAGE)
    private TrialInfo currentTrial = null;

    @Getter(AccessLevel.PACKAGE)
    private boolean needsTrim = false;

    @Getter(AccessLevel.PACKAGE)
    private int lastVisitedIndex = -1;

    @Getter(AccessLevel.PACKAGE)
    private int toadsThrown = 0;

    // Number of consecutive game ticks where TrialInfo.getCurrent(client) returned null
    // Used to allow a grace period before clearing currentTrial during transient nulls
    private int nullTrialConsecutiveTicks = 0;

    private final Set<Integer> BOAT_WORLD_ENTITY_IDS = Set.of(12);

    private final Map<Integer, List<GameObject>> toadFlagsById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, GameObject> trialCratesById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private final Map<Integer, List<GameObject>> trialBoostsById = new HashMap<>();
    @Getter(AccessLevel.PACKAGE)
    private GameObject sailGameObject = null;

    @Getter(AccessLevel.PACKAGE)
    private Point lastMenuCanvasPosition = null;
    @Getter(AccessLevel.PACKAGE)
    private WorldPoint lastMenuCanvasWorldPoint = null;

    @Getter(AccessLevel.PACKAGE)
    private int cargoItemCount = 0;

    @Override
    protected void startUp() {
        //log.info("Bearycuda Trials Plugin started!");
        overlayManager.add(overlay);
        overlayManager.add(panel);
        if (config.enableStartPreviousRankLeftClick()) {
            FirstMenuEntries.add(MENU_OPTION_START_PREVIOUS_RANK);
        }
        if (config.enableQuickResetLeftClick()) {
            FirstMenuEntries.add(MENU_OPTION_QUICK_RESET);
        }
        if (config.disableStopNavigating()) {
            RemoveMenuEntriesDuringTrial.add(MENU_OPTION_STOP_NAVIGATING);
        }
        if (config.showGwenithGlideRoutes()) {
            TrialRoute.AddGwenithGlideRoutes();
        }
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(overlay);
        overlayManager.remove(panel);
        reset();
        //log.info("BearycudaTrialsPlugin shutDown: panel removed and state reset.");
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        if (client == null || client.getLocalPlayer() == null) {
            return;
        }
        TrialInfo newTrialInfo = TrialInfo.getCurrent(client);

        // Allow a grace period of 18 game ticks where TrialInfo may be null (e.g., region/load transitions) before clearing currentTrial.
        // We only apply the null after 18 consecutive null ticks.
        if (newTrialInfo != null) {
            // If trial info reappears, clear any null-grace countdown
            nullTrialConsecutiveTicks = 0;

            // If the trial changed (location/rank/reset time), reset route state
            if (currentTrial == null || currentTrial.Location != newTrialInfo.Location || currentTrial.Rank != newTrialInfo.Rank || newTrialInfo.CurrentTimeSeconds < currentTrial.CurrentTimeSeconds) {
                resetRouteData();
            }

            updateToadsThrown(newTrialInfo);
            currentTrial = newTrialInfo;
        } else {
            if (currentTrial != null) {
                nullTrialConsecutiveTicks += 1;
                if (nullTrialConsecutiveTicks >= 18) {
                    resetRouteData();
                    currentTrial = null;
                }
            } else {
                nullTrialConsecutiveTicks = 0;
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
        }
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
        if (!config.showDebugMenuCopyTileOptions()) {
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
    public void onConfigChanged(ConfigChanged event) {
        if (event == null) {
            return;
        }
        // Only react to our plugin's config group
        if (!event.getGroup().equals("bearycudaTrials")) {
            return;
        }

        if (config.showGwenithGlideRoutes() && TrialRoute.AllTrialRoutes.stream().noneMatch(x -> x.Location == TrialLocations.GwenithGlide)) {
            TrialRoute.AddGwenithGlideRoutes();
        } else if (!config.showGwenithGlideRoutes()) {
            TrialRoute.AllTrialRoutes.removeIf(x -> x.Location == TrialLocations.GwenithGlide);
        }

        if (config.enableStartPreviousRankLeftClick()) {
            if (FirstMenuEntries.stream().noneMatch(x -> x.equals(MENU_OPTION_START_PREVIOUS_RANK))) {
                FirstMenuEntries.add(MENU_OPTION_START_PREVIOUS_RANK);
            }
        } else {
            if (FirstMenuEntries.stream().anyMatch(x -> x.equals(MENU_OPTION_START_PREVIOUS_RANK))) {
                FirstMenuEntries.remove(MENU_OPTION_START_PREVIOUS_RANK);
            }
        }

        if (config.enableQuickResetLeftClick()) {
            if (FirstMenuEntries.stream().noneMatch(x -> x.equals(MENU_OPTION_QUICK_RESET))) {
                FirstMenuEntries.add(MENU_OPTION_QUICK_RESET);
            }
        } else {
            if (FirstMenuEntries.stream().anyMatch(x -> x.equals(MENU_OPTION_QUICK_RESET))) {
                FirstMenuEntries.remove(MENU_OPTION_QUICK_RESET);
            }
        }

        if (config.disableStopNavigating()) {
            if (RemoveMenuEntriesDuringTrial.stream().noneMatch(x -> x.equals(MENU_OPTION_STOP_NAVIGATING))) {
                RemoveMenuEntriesDuringTrial.add(MENU_OPTION_STOP_NAVIGATING);
            }
        } else {
            if (RemoveMenuEntriesDuringTrial.stream().anyMatch(x -> x.equals(MENU_OPTION_STOP_NAVIGATING))) {
                RemoveMenuEntriesDuringTrial.remove(MENU_OPTION_STOP_NAVIGATING);
            }
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

    @Subscribe(priority = -1)
    public void onPostMenuSort(PostMenuSort e) {
        if (client.isMenuOpen()) {
            return;
        }

        var entries = swapMenuEntries(client.getMenuEntries());
        entries = addDebugMenuEntries(entries);

        if (currentTrial != null && !RemoveMenuEntriesDuringTrial.isEmpty()) {
            entries = removeMenuEntries(entries, RemoveMenuEntriesDuringTrial);
        }

        client.setMenuEntries(entries);
    }

    private MenuEntry[] removeMenuEntries(MenuEntry[] entries, Collection<String> toRemove) {
        if (entries == null || entries.length == 0 || toRemove == null || toRemove.isEmpty()) {
            return entries;
        }
        var walkHereEntry = Arrays.stream(entries).filter(x -> x != null && x.getOption().toLowerCase().equals("walk here")).findFirst().orElse(null);
        var entryList = new ArrayList<MenuEntry>(Arrays.asList(entries));
        var it = entryList.iterator();
        while (it.hasNext()) {
            var menuEntry = it.next();
            if (menuEntry == null) {
                continue;
            }
            var opt = menuEntry.getOption();
            if (opt == null) {
                continue;
            }
            if (toRemove.stream().anyMatch(x -> opt.toLowerCase().contains(x))) {
                if (walkHereEntry != null) {
                    var walkIdx = entryList.indexOf(walkHereEntry);
                    var currIdx = entryList.indexOf(menuEntry);
                    entryList.set(walkIdx, menuEntry);
                    entryList.set(currIdx, walkHereEntry);
                } else {
                    it.remove();
                }
            }
        }
        return entryList.toArray(new MenuEntry[0]);
    }

    private MenuEntry[] addDebugMenuEntries(MenuEntry[] entries) {
        if (!config.showDebugMenuCopyTileOptions()) {
            return entries;
        }

        var p = client.getLocalPlayer();
        if (p == null) {
            return entries;
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
        return list.toArray(new MenuEntry[0]);
    }

    private void resetRouteData() {
        lastVisitedIndex = -1;
        toadsThrown = 0;
    }

    private void reset() {
        // Clear runtime caches and tracked state on region change / shutdown
        toadFlagsById.clear();
        trialCratesById.clear();
        trialBoostsById.clear();
        sailGameObject = null;
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
            //log.info("Visited waypoint {} / {} for route {}", lastVisitedIndex, route.Points.size() - 1, route.Rank);
        }
    }

    public List<Integer> getNextIndicesAfterLastVisited(final TrialRoute route, final int limit) {
        if (route == null || route.Points == null || route.Points.isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }
        int start = Math.max(0, lastVisitedIndex);
        if (start >= route.Points.size()) {
            return Collections.emptyList();
        }
        List<Integer> out = new ArrayList<>(limit);
        var nextPortal = route.PortalDirections.stream()
                .filter(x -> x.Index >= lastVisitedIndex)
                .min((a, b) -> Integer.compare(a.Index, b.Index))
                .orElse(null);
        for (int i = start; i < route.Points.size() && out.size() < limit; i++) {
            if (nextPortal != null && i > nextPortal.Index) {
                break;
            }
            out.add(i);
        }
        return out;
    }

    public List<WorldPoint> getVisibleLineForRoute(final WorldPoint player, final TrialRoute route, final int limit) {
        if (player == null || route == null || lastVisitedIndex == -1) {
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

    public PortalDirection getVisiblePortalDirection(TrialRoute route) {
        var portalDirection = route.PortalDirections.stream()
                .filter(x -> x.Index - 1 == lastVisitedIndex || x.Index == lastVisitedIndex || x.Index + 1 == lastVisitedIndex)
                .min((a, b) -> Integer.compare(a.Index, b.Index))
                .orElse(null);

        return portalDirection;
    }

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

        for (TrialRoute route : TrialRoute.AllTrialRoutes) {
            if (route == null) {
                continue;
            }

            if (route.Location == currentTrial.Location && route.Rank == currentTrial.Rank) {
                return route;
            }
        }
        return null;
    }

    public List<WorldPoint> getVisibleActiveLineForPlayer(final WorldPoint player, final int limit) {
        var route = getActiveTrialRoute();
        if (route == null) {
            return Collections.emptyList();
        }

        return getVisibleLineForRoute(player, route, limit);
    }

    public List<Integer> getNextUnvisitedIndicesForActiveRoute(final int limit) {
        var route = getActiveTrialRoute();
        if (route == null) {
            return Collections.emptyList();
        }
        return getNextIndicesAfterLastVisited(route, limit);
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
