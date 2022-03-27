package net.runelite.client.plugins.autodecepticon;

import net.runelite.api.GameObject;

import java.util.ArrayList;
import java.util.Hashtable;

public class DecepticonPacket {
    PlayerLocation playerLocation;
    ArrayList<InventorySlot> inv;
    Hashtable<Integer, WallObjectData> wallObjects;
    Hashtable<Integer, ArrayList<GameObjectData>> gameObjects;
    int animation;
}
