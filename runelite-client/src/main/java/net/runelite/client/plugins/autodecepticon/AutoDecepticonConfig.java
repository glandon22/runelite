package net.runelite.client.plugins.autodecepticon;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("autodecepticon")
public interface AutoDecepticonConfig extends Config {
    @ConfigItem(
            keyName = "showPlayer",
            name = "Show player info",
            description = "expose player info to backend",
            position = 1
    )
    default boolean showPlayer()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showInv",
            name = "Show inv info",
            description = "expose inv info to backend",
            position = 2
    )
    default boolean showInv()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showWallObjects",
            name = "Show wall obj info",
            description = "expose wall obj info to backend",
            position = 3
    )
    default boolean showWallObjects()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showGameObjects",
            name = "Show game obj info",
            description = "expose game obj info to backend",
            position = 4
    )
    default boolean showGameObjects()
    {
        return true;
    }

    @ConfigItem(
            keyName = "gameObject",
            name = "game object to find",
            description = "game object to find",
            position = 5
    )
    default int gameObject()
    {
        return 0;
    }
}
