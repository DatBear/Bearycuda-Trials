package com.datbear;

import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class BearracudaTrialsPanel extends OverlayPanel {
    private Client client;
    private BearracudaTrialsPlugin plugin;
    private BearracudaTrialsConfig config;

    @Inject
    public BearracudaTrialsPanel(Client client, BearracudaTrialsPlugin plugin,
            BearracudaTrialsConfig config) {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.TOP_CENTER);
        getMenuEntries().add(
                new OverlayMenuEntry(
                        RUNELITE_OVERLAY_CONFIG,
                        OPTION_CONFIGURE,
                        "Bearracuda Trials Panel"));
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
