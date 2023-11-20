package net.runelite.client.plugins.glblastfurnace;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BarTypes {
    MITH("Mith"),
    ADDY("Addy"),
    Rune("Rune");

    private final String name;

    @Override
    public String toString()
    {
        return getName();
    }
}
