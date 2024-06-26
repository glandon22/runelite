package net.runelite.client.plugins.glmobkiller;

import net.runelite.client.config.Config;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("glmobkiller")
public interface glmobkillerConfig extends Config {
    
    @ConfigItem(
        keyName = "npcToKill",
        name = "npcToKill",
        description = "NPC to kill",
        position = 1
    )
    default String npcToKill()
    {
        return "";
    }

    @ConfigItem(
            keyName = "pot",
            name = "Potion to drink",
            description = "Which potions to drink, if any, during combat",
            position = 2
    )
    default potsEnum pot()
    {
        return potsEnum.NONE;
    }

    @Range(
            max = 30
    )
    @ConfigItem(
            keyName = "potInterval",
            name = "pot interval",
            description = "How often to drink potions (minutes)",
            position = 3
    )
    default int potInterval()
    {
        return 10;
    }

    @Range(
            max = 98,
            min = 1
    )
    @ConfigItem(
            keyName = "minEat",
            name = "Minimum eat health",
            description = "Bot will eat when health is below this threshold",
            position = 4
    )
    default int minEat()
    {
        return 20;
    }

    @Range(
            max = 4500,
            min = -1
    )
    @ConfigItem(
            keyName = "safeSpotX",
            name = "Safe Spot: X Coord",
            description = "X coordinate of safespot to stand on",
            position = 5
    )
    default int safeSpotX()
    {
        return -1;
    }

    @Range(
            max = 15000,
            min = -1
    )
    @ConfigItem(
            keyName = "safeSpotY",
            name = "Safe Spot: Y Coord",
            description = "Y coordinate of safespot to stand on",
            position = 6
    )
    default int safeSpotY()
    {
        return -1;
    }

    @Range(
            max = 4,
            min = -1
    )
    @ConfigItem(
            keyName = "safeSpotZ",
            name = "Safe Spot: Z Coord",
            description = "Z coordinate of safespot to stand on",
            position = 7
    )
    default int safeSpotZ()
    {
        return -1;
    }

}
