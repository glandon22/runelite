package net.runelite.client.plugins.autoserver;

import net.runelite.api.Deque;
import net.runelite.api.Projectile;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
    HashMap<Integer, ObjectUtil.EnhancedObjData> gameObjects;
    HashMap<Integer, ObjectUtil.EnhancedObjData> groundObjects;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedObjData>> wallObjects;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedObjData>> multipleGameObjects;
    int poseAnimation;
    int playerAnimation;
    int varBit;
    Interfaces.EnrichedInterfaceData widget;
    HashMap<String, Interfaces.EnrichedInterfaceData> widgets;
    Utilities.PointData playerWorldPoint;
    String interactingWith;
    String[] chatOptions;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedObjData>> decorativeObjects;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedObjData>> groundItems;
    HashMap<Integer, ArrayList<ObjectUtil.EnhancedObjData>> allGroundItems;
    int targetObj;
    int targetNPC;
    List<Inventory.Slot> equipmentInv;
    java.awt.List menuEntries;
    HashSet<Integer> projectiles;
    Interfaces.RightClickMenu rightClickMenu;
    String[] chatLines;
}
