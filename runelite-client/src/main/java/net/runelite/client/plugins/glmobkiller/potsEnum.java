package net.runelite.client.plugins.glmobkiller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum potsEnum {
    SC("SUPER_COMBATS"),
    SASS("SUPER_STRENGTH_AND_ATTACK"),
    RANGE("RANGING_POTION"),
    MAGIC("MAGIC_POTION"),
    NONE("NONE");

    private final String name;

    @Override
    public String toString()
    {
        return getName();
    }
}