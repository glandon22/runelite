package net.runelite.client.plugins.autoserver;

import net.runelite.api.coords.WorldPoint;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GameInfoPacket {
    long timestamp = Instant.now().getEpochSecond();
    List<Inventory.Slot> inv;
    ArrayList<NPCs.NpcPacket> npcs;
    ArrayList<Bank.BankSlot> bankItems;
    HashMap<Character, Integer> dumpInvButton;
    HashMap<String, Player.SkillData> skills;
    boolean isMining;
    boolean isFishing;
    HashMap<String, Tiles.TileData> tiles;
    Interfaces.InterfaceData clickToPlay;
    HashMap<Integer, ObjectUtil.GameObjData> gameObjects;
    HashMap<Integer, ObjectUtil.GameObjData> groundObjects;
    HashMap<Integer, ArrayList<ObjectUtil.GameObjData>> wallObjects;
    int poseAnimation;
    int playerAnimation;
    int varBit;
    Interfaces.InterfaceData widget;
    Utilities.PointData playerWorldPoint;
    String interactingWith;
    String[] chatOptions;
    HashMap<Integer, ArrayList<ObjectUtil.GameObjData>> decorativeObjects;
    HashMap<Integer, ArrayList<ObjectUtil.ItemObjData>> groundItems;
    int targetObj;
    List<Inventory.Slot> equipmentInv;
    java.awt.List menuEntries;
}
