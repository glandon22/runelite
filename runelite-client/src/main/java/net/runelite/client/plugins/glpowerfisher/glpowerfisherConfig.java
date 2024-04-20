package net.runelite.client.plugins.glpowerfisher;

import net.runelite.client.config.Config;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("glpowerfisher")
public interface glpowerfisherConfig extends Config {
    
    @ConfigItem(
        keyName = "fish",
        name = "fish",
        description = "fish to catch",
        position = 1
    )
    default fishEnum fish()
    {
        return fishEnum.SHRIMP;
    }

}
