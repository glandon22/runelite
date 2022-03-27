package net.runelite.client.plugins.autobotminer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoBotMiner")
public interface AutoBotMinerConfig extends Config {
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
            keyName = "rock to mine",
            name = "rock to mine",
            description = "rock to mine",
            position = 2
    )
    default String rockToMine()
    {
        return "";
    }
}
