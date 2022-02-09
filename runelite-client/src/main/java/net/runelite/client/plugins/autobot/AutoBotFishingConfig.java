package net.runelite.client.plugins.autobot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AutoBotFishing")
public interface AutoBotFishingConfig extends Config {
    enum TargetFish {
        SHRIMP,
        SALMON,
        SHARK,
        MONKFISH,
        LOBSTER,
        TUNA,
        SWORDFISH,
        BARBARIAN,
        HARPOONFISH
    }

    @ConfigItem(
            position = 0,
            keyName = "fishingFor",
            name = "Fishing for",
            description = "Return closest spot for the fish you are targeting."
    )
    default TargetFish fishingFor()
    {
        return TargetFish.SALMON;
    }
}
