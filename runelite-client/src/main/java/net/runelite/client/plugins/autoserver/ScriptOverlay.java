package net.runelite.client.plugins.autoserver;

import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

import static net.runelite.api.MenuAction.RUNELITE_OVERLAY_CONFIG;
import static net.runelite.client.ui.overlay.OverlayManager.OPTION_CONFIGURE;

public class ScriptOverlay extends OverlayPanel {
    private final AutoServer plugin;

    @Inject
    private ScriptOverlay(AutoServer plugin) {
        super(plugin);
        setPosition(OverlayPosition.TOP_LEFT);
        this.plugin = plugin;
        addMenuEntry(RUNELITE_OVERLAY_CONFIG, OPTION_CONFIGURE, "Script overlay");
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        try {
            HashMap<String, String> search = plugin.getScriptStats();
            if (search == null) {
                return null;
            }
            for (Map.Entry<String, String> entry : search.entrySet()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left(entry.getKey() + ": ")
                        .right(entry.getValue())
                        .build());

                panelComponent.setPreferredSize(new Dimension(
                        graphics.getFontMetrics().stringWidth(entry.getKey() + ": " + entry.getValue()) + 10, 0)
                );

            }
            return super.render(graphics);
        } catch (Exception e) {
            System.out.println("encountered error rendering autoserver script overlay.");
        }
        return null;
    }

}
