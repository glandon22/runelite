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
}
