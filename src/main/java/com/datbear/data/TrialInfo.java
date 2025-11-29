package com.datbear.data;

import java.util.regex.Pattern;

import com.google.common.base.Strings;

import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID.SailingBtHud;

public class TrialInfo {

    public TrialLocations Location;
    public TrialRanks Rank;

    public int CurrentTimeSeconds;
    public int GoalTimeSeconds;

    public String ObjectiveText;
    public int CollectedPrimaryObjectives;
    public int TotalPrimaryObjectivesNeeded;

    public int CollectedCrates;
    public int TotalCratesNeeded;

    public boolean HasRum;
    public boolean HasToads;
    public int ToadCount;

    public static TrialInfo getCurrent(Client client) {
        var trialWidget = client.getWidget(SailingBtHud.BARRACUDA_TRIALS);
        if (trialWidget == null || trialWidget.isHidden()) {
            return null;
        }

        var info = new TrialInfo();
        var locationText = client.getWidget(SailingBtHud.BT_TITLE).getChild(9).getText();
        info.Location = parseLocation(locationText);

        var rankSprite = client.getWidget(SailingBtHud.BT_RANK_GFX).getSpriteId();
        info.Rank = parseRank(rankSprite);

        var currentTimeSecondsString = client.getWidget(SailingBtHud.BT_CURRENT_TIME).getText();
        info.CurrentTimeSeconds = parseTimeSeconds(currentTimeSecondsString);

        var goalTimeSecondsString = client.getWidget(SailingBtHud.BT_RANK_TIME).getText();
        info.GoalTimeSeconds = parseTimeSeconds(goalTimeSecondsString);

        var primaryObjectiveText = client.getWidget(SailingBtHud.BT_TRACKER_PROGRESS).getText();
        var crateText = client.getWidget(SailingBtHud.BT_OPTIONAL_PROGRESS).getText();

        var primaryObjectiveInfo = parseObjectiveText(primaryObjectiveText);
        info.CollectedPrimaryObjectives = primaryObjectiveInfo.Collected;
        info.TotalPrimaryObjectivesNeeded = primaryObjectiveInfo.TotalNeeded;

        var crateInfo = parseObjectiveText(crateText);
        info.CollectedCrates = crateInfo.Collected;
        info.TotalCratesNeeded = crateInfo.TotalNeeded;

        var partialGfxSpriteId = client.getWidget(SailingBtHud.BT_PARTIAL_GFX).getSpriteId();
        info.HasRum = HasRum(info.Location, partialGfxSpriteId);
        info.HasToads = HasToads(info.Location, partialGfxSpriteId);

        var partialText = client.getWidget(SailingBtHud.BT_PARTIAL_TEXT).getText();
        info.ToadCount = ToadCount(info.Location, partialText);

        return info;
    }

    @Override
    public String toString() {
        return String.format("Location=%s, Rank=%s, PrimaryObjectives=%d/%d, Crates=%d/%d, HasRum=%b, HasFrogs=%b", Location.toString(), Rank.toString(), CollectedPrimaryObjectives, TotalPrimaryObjectivesNeeded, CollectedCrates, TotalCratesNeeded, HasRum, HasToads);
    }

    private static int parseTimeSeconds(String timeText) {
        if (Strings.isNullOrEmpty(timeText)) {
            return 0;
        }

        var pattern = Pattern.compile("(\\d+):(\\d+)");
        var matcher = pattern.matcher(timeText);
        if (matcher.find() && matcher.groupCount() == 2) {
            var minutes = Integer.parseInt(matcher.group(1));
            var seconds = Integer.parseInt(matcher.group(2));
            return minutes * 60 + seconds;
        }
        return 0;
    }

    private static boolean HasToads(TrialLocations location, int spriteId) {
        switch (location) {
            case JubblyJive:
                return spriteId == 7024;
            default:
                return false;
        }
    }

    private static int ToadCount(TrialLocations location, String text) {
        switch (location) {
            case JubblyJive:
                var pattern = Pattern.compile("(\\d+)");
                var matcher = pattern.matcher(text);
                if (matcher.find() && matcher.groupCount() == 1) {
                    return Integer.parseInt(matcher.group(1));
                }
                return 0;
            default:
                return 0;
        }
    }

    private static boolean HasRum(TrialLocations location, int spriteId) {
        switch (location) {
            case TemporTantrum:
                return spriteId == 7022;
            default:
                return false;
        }
    }

    private static ObjectiveInfo parseObjectiveText(String text) {
        var info = new ObjectiveInfo();
        if (Strings.isNullOrEmpty(text)) {
            return info;
        }

        var pattern = Pattern.compile("(\\d+) / (\\d+).*?");
        var matcher = pattern.matcher(text);
        if (matcher.find() && matcher.groupCount() == 2) {
            info.Collected = Integer.parseInt(matcher.group(1));
            info.TotalNeeded = Integer.parseInt(matcher.group(2));
        }
        return info;
    }

    private static TrialRanks parseRank(int spriteId) {
        switch (spriteId) {
            case 7026:
                return TrialRanks.Unranked;// todo check
            case 7027:
                return TrialRanks.Swordfish;
            case 7028:
                return TrialRanks.Shark;
            case 7029:
                return TrialRanks.Marlin;
        }
        return TrialRanks.Unknown;
    }

    private static TrialLocations parseLocation(String text) {
        if (Strings.isNullOrEmpty(text)) {
            return TrialLocations.Unknown;
        }

        switch (text) {
            case "Gwenith Glide":
                return TrialLocations.GwenithGlide;
            case "Jubbly Jive":
                return TrialLocations.JubblyJive;
            case "Tempor Tantrum":
                return TrialLocations.TemporTantrum;
        }
        return TrialLocations.Unknown;
    }
}
