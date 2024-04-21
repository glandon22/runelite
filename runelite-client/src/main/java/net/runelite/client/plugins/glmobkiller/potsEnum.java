package net.runelite.client.plugins.glmobkiller;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum potsEnum {
    SC("SUPER COMBATS"),
    SASS("SUPER STRENGTH AND ATTACK"),
    RANGE("RANGING POTION"),
    MAGIC("MAGIC POTION"),
    NONE("NONE");

    private final String name;

    @Override
    public String toString()
    {
        return getName();
    }
}