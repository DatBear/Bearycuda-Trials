package com.datbear;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class BearycudaTrialsPanel extends OverlayPanel {
    private Client client;
    private BearycudaTrialsPlugin plugin;
    private BearycudaTrialsConfig config;

    @Inject
    public BearycudaTrialsPanel(Client client, BearycudaTrialsPlugin plugin, BearycudaTrialsConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        getMenuEntries().add(
                new OverlayMenuEntry(
                        RUNELITE_OVERLAY_CONFIG,
                        OPTION_CONFIGURE,
                        "Bearycuda Trials Panel"));
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Clear previous children each frame to prevent uncontrolled growth
        panelComponent.getChildren().clear();
        // var container = client.getItemContainer(33733);
        // var itemCount = 0;
        // if (container != null) {
        // itemCount = container.count();
        // }
        // panelComponent.getChildren().add(
        // LineComponent.builder().left("Sailing!")
        // .right(plugin.getCargoItemCount() + " items").build());

        return super.render(graphics);
    }

}
