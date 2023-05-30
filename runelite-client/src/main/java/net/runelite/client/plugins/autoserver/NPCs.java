package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.NPC;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class NPCs {
    @Value
    public static class NpcPacket
    {
        int x;
        int y;
        String name;
        int id;
        int dist;
        int graphic;
    }

    public ArrayList<NpcPacket> getNPCsByName(Client client, HashSet<String> npcsToFind) {
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if (n != null && npcsToFind.contains(npc.getName())) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                Utilities u = new Utilities();
                HashMap<Character, Integer> center = u.getCenter(r);
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (center.get('x') > 50 && center.get('y') > 50) {
                    NpcPacket np = new NpcPacket(
                            center.get('x'),
                            center.get('y'),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getGraphic()
                    );
                    alnp.add(np);
                }
            }
        }

        return alnp;
    }

    public ArrayList<NpcPacket> getNPCsByID(Client client, HashSet<String> npcsToFind) {
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if (n != null && npcsToFind.contains(Integer.toString(npc.getId()))) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                Utilities u = new Utilities();
                HashMap<Character, Integer> center = u.getCenter(r);
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (center.get('x') > 50 && center.get('y') > 50) {
                    NpcPacket np = new NpcPacket(
                            center.get('x'),
                            center.get('y'),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getGraphic()
                    );
                    alnp.add(np);
                }
            }
        }
        return alnp;
    }

    public ArrayList<NpcPacket> getNPCsByToKill(Client client, HashSet<String> npcsToFind) {
        System.out.println("getting convex");
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if (n != null && npcsToFind.contains(npc.getName()) && npc.getInteracting() == null) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                Utilities u = new Utilities();
                HashMap<Character, Integer> center = u.getCenter(r);
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (center.get('x') > 50 && center.get('y') > 50) {
                    NpcPacket np = new NpcPacket(
                            center.get('x'),
                            center.get('y'),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getGraphic()
                    );
                    alnp.add(np);
                }
            }
        }

        return alnp;
    }
}
