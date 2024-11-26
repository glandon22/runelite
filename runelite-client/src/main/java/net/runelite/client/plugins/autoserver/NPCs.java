package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.client.game.NpcUtil;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;

public class NPCs {
    @Inject
    public NPCs(NpcUtil npcUtil) {
        this.npcUtil = npcUtil;
    }
    @Value
    public static class NpcPacket
    {
        int x;
        int y;
        String name;
        int id;
        int dist;
        int animation;
        int health;
        int scale;
        int x_coord;
        int y_coord;
        String overheadText;
        int compositionID;
        String interacting;
        int cbLvl;
        int size;
        int orientation;
        int poseAnimation;
    }

    private final NpcUtil npcUtil;

    Interfaces i = new Interfaces();
    Utilities u = new Utilities();

    public ArrayList<NpcPacket> getNPCsByName(Client client, JsonArray input) {
        HashSet<String> npcsToFind = new HashSet<>();
        for (JsonElement elem : input) {
            String tileHash = elem.toString().replace("\"", "");
            npcsToFind.add(tileHash);
        }
        Interfaces.CanvasData canvasData = i.getCanvasData(client);
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            if (npcUtil.isDying(npc)) continue;
            String n = npc.getName();
            String npcID = Integer.toString(npc.getId());
            if (n != null && (
                    npcsToFind.contains(npcID) ||
                            npcsToFind.contains(n.toUpperCase(Locale.ROOT)) ||
                            npcsToFind.isEmpty()
            )
            ) {
                Shape poly = npc.getConvexHull();
                if (poly == null) {continue;}
                Utilities u = new Utilities();
                Point center = u.findCenterPoint(poly, canvasData.getXOffset(), canvasData.getYOffset());
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (u.isClickable(client, center)) {
                    NPCComposition npcComposition = npc.getTransformedComposition();
                    NpcPacket np = new NpcPacket(
                            center.getX(),
                            center.getY(),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getAnimation(),
                            npc.getHealthRatio(), npc.getHealthScale(),
                            npc.getWorldLocation().getX(),
                            npc.getWorldLocation().getY(),
                            npc.getOverheadText(),
                            npc.getComposition().getId(),
                            npc.isInteracting() && npc.getInteracting() != null ? npc.getInteracting().getName() : null,
                            npc.getCombatLevel(),
                            npcComposition != null ? npcComposition.getSize() : 1,
                            npc.getOrientation(),
                            npc.getPoseAnimation()
                    );
                    alnp.add(np);
                }
            }
        }

        return alnp;
    }

    public ArrayList<NpcPacket> getNPCsByID(Client client, HashSet<String> npcsToFind) {
        Interfaces.CanvasData canvasData = i.getCanvasData(client);
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            String npcID = Integer.toString(npc.getId());
            if (n != null && (
                    npcsToFind.contains(npcID) ||
                    npcsToFind.contains(n.toUpperCase(Locale.ROOT)) ||
                            npcsToFind.isEmpty()
                )
            ) {
                Shape poly = npc.getConvexHull();
                if (poly == null) {continue;}
                Point center = u.findCenterPoint(poly, canvasData.getXOffset(), canvasData.getYOffset());
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (u.isClickable(client, center)) {
                    NPCComposition npcComposition = npc.getTransformedComposition();
                    NpcPacket np = new NpcPacket(
                            center.getX(),
                            center.getY(),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getAnimation(),
                            npc.getHealthRatio(), npc.getHealthScale(),
                            npc.getWorldLocation().getX(),
                            npc.getWorldLocation().getY(),
                            npc.getOverheadText(),
                            npc.getComposition().getId(),
                            npc.isInteracting() ? npc.getInteracting().getName() : null,
                            npc.getCombatLevel(),
                            npcComposition != null ? npcComposition.getSize() : 1,
                            npc.getOrientation(),
                            npc.getPoseAnimation()
                    );
                    alnp.add(np);
                }
            }
        }
        return alnp;
    }
}
