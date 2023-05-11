package net.runelite.client.plugins.autoserver;

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
    HashMap<Integer, ObjectUtil.EnhancedGameObjData> gameObjects;
    HashMap<Integer, ObjectUtil.GameObjData> groundObjects;
    HashMap<Integer, ArrayList<ObjectUtil.GameObjData>> wallObjects;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedGameObjData>> multipleGameObjects;
    int poseAnimation;
    int playerAnimation;
    int varBit;
    Interfaces.EnrichedInterfaceData widget;
    Utilities.PointData playerWorldPoint;
    String interactingWith;
    String[] chatOptions;
    HashMap<Integer, ArrayList<ObjectUtil.GameObjData>> decorativeObjects;
    HashMap<Integer, ArrayList<ObjectUtil.ItemObjData>> groundItems;
    int targetObj;
    int targetNPC;
    List<Inventory.Slot> equipmentInv;
    java.awt.List menuEntries;
}
