package net.runelite.client.plugins.autoserver;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.plugins.glblastfurnace.BarTypes;

@ConfigGroup("AutoServer")
public interface AutoServerConfig extends Config {
    @ConfigItem(
            keyName = "port",
            name = "port",
            description = "What port to listen on",
            position = 1
    )
    default int port()
    {
        return 56799;
    }
}
