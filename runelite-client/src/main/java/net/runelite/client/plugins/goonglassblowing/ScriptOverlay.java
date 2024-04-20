package net.runelite.client.plugins.goonglassblowing;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;

import java.awt.*;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class ScriptOverlay extends OverlayPanel {
    private final GoonGlassBlowingPlugin plugin;

    @Inject
    private ScriptOverlay(GoonGlassBlowingPlugin plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Script overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Status: ")
                .right(plugin.getStatus())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Next Break: ")
                .right(plugin.getBreak_start())
                .build());

        panelComponent.getChildren().add(LineComponent.builder()
                .left("Break End: ")
                .right(plugin.getBreak_end())
                .build());

        return super.render(graphics);
    }
}
