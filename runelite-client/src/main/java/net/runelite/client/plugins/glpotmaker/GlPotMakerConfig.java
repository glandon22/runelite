package net.runelite.client.plugins.glpotmaker;

import net.runelite.client.config.Config;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("potMaker")
public interface GlPotMakerConfig extends Config {
    @Range(
            max = 100000
    )
    @ConfigItem(
            keyName = "pot",
            name = "Unfinished Pot ID",
            description = "The number value of the unfinished pot you are using. If you are unsure what that value is, search here: https://www.runelocus.com/tools/osrs-item-id-list/",
            position = 1
    )
    default int pot()
    {
        return 0;
    }

    @Range(
            max = 100000
    )
    @ConfigItem(
            keyName = "secondary",
            name = "Secondary",
            description = "The number value of the secondary you are using. If you are unsure what that value is, search here: https://www.runelocus.com/tools/osrs-item-id-list/",
            position = 2
    )
    default int secondary()
    {
        return 0;
    }
}
