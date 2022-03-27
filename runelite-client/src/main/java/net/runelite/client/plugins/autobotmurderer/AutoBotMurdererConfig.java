package net.runelite.client.plugins.autobotmurderer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoBotMurderer")
public interface AutoBotMurdererConfig extends Config {
    @ConfigItem(
            keyName = "exposedPort",
            name = "port to expose data on",
            description = "exposed port",
            position = 1
    )
    default String exposedPort()
    {
        return "";
    }

    @ConfigItem(
            keyName = "npcTokill",
            name = "npc to kill",
            description = "npc to kill ",
            position = 2
    )
    default String npcTokill()
    {
        return "";
    }
}
