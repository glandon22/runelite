package net.runelite.client.plugins.glblastfurnace;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;
import net.runelite.client.plugins.camera.ControlFunction;

@ConfigGroup("glBlastFurnace")
public interface GlBlastFurnaceConfig extends Config {
    @ConfigItem(
            keyName = "barTypes",
            name = "Bar Type",
            description = "Select which bar to smelt.",
            position = 1
    )
    default BarTypes barTypes()
    {
        return BarTypes.MITH;
    }
}
