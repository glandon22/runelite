package net.runelite.client.plugins.goonlite;

import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.goonlite.input.Mouse;

import javax.inject.Inject;

@PluginDescriptor(
        name = "Empty Plugin to Debug Stuff!",
        description = "Debug things",
        tags = {"dev", "debug", "goonlite"},
        enabledByDefault = false
)
public class debuggingplugin extends Plugin {
    Mouse m = new Mouse();

    @Inject
    Client client;

    @Override
    protected void startUp() {
        Goonlite.setClient(client);
    }

    @Subscribe
    void onGameTick(GameTick e) {
        System.out.println("tick tock");
        m.click(new Point(1000, 1000), false);
    }
}
