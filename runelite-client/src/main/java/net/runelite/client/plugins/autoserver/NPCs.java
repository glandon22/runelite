package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldArea;

import java.awt.*;
import java.util.*;
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
        int health;
        int scale;
        int x_coord;
        int y_coord;
        String overheadText;
        int compositionID;
        String interacting;
        int cbLvl;
    }

    Interfaces i = new Interfaces();
    Utilities u = new Utilities();

    public ArrayList<NpcPacket> getNPCsByName(Client client, HashSet<String> npcsToFind) {
        Interfaces.CanvasData canvasData = i.getCanvasData(client);
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if ((n != null && npcsToFind.contains(npc.getName().toUpperCase(Locale.ROOT))) || npcsToFind.isEmpty()) {
                Shape poly = npc.getConvexHull();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                Utilities u = new Utilities();
                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (u.isClickable(client, r)) {
                    String name = null;
                    if (npc.getInteracting() != null) {
                        name = npc.getInteracting().getName();
                    }
                    WorldArea area = client.getLocalPlayer().getWorldArea();
                    NpcPacket np = new NpcPacket(
                            center.get('x'),
                            center.get('y'),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getGraphic(),
                            npc.getHealthRatio(), npc.getHealthScale(),
                            npc.getWorldLocation().getX(),
                            npc.getWorldLocation().getY(),
                            npc.getOverheadText(),
                            npc.getComposition().getId(),
                            name,
                            npc.getCombatLevel()
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
            if (n != null && (npcsToFind.contains(Integer.toString(npc.getId())) || npcsToFind.contains(Integer.toString(npc.getComposition().getId())))) {
                Shape poly = npc.getConvexHull();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
                // For some reason, right as I open an interface it sometimes says the points are all located
                // in a small 50x50 corner of the upper right-hand screen.
                if (u.isClickable(client, r)) {
                    NpcPacket np = new NpcPacket(
                            center.get('x'),
                            center.get('y'),
                            npc.getName(),
                            npc.getId(),
                            npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                            npc.getGraphic(),
                            npc.getHealthRatio(), npc.getHealthScale(),
                            npc.getWorldLocation().getX(),
                            npc.getWorldLocation().getY(),
                            npc.getOverheadText(),
                            npc.getComposition().getId(),
                            npc.isInteracting() ? npc.getInteracting().getName() : null,
                            npc.getCombatLevel()
                    );
                    alnp.add(np);
                }
            }
        }
        return alnp;
    }

    public ArrayList<NpcPacket> getNPCsByToKill(Client client, HashSet<String> npcsToFind) {
        Interfaces.CanvasData canvasData = i.getCanvasData(client);
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if (n != null && npcsToFind.contains(npc.getName()) && npc.getInteracting() == null) {
                try {
                    Polygon poly = npc.getCanvasTilePoly();
                    if (poly == null) {
                        continue;
                    }
                    Rectangle r = poly.getBounds();
                    Utilities u = new Utilities();
                    HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
                    // For some reason, right as I open an interface it sometimes says the points are all located
                    // in a small 50x50 corner of the upper right-hand screen.
                    if (u.isClickable(client, r)) {
                        NpcPacket np = new NpcPacket(
                                center.get('x'),
                                center.get('y'),
                                npc.getName(),
                                npc.getId(),
                                npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                npc.getGraphic(),
                                npc.getHealthRatio(), npc.getHealthScale(),
                                npc.getWorldLocation().getX(),
                                npc.getWorldLocation().getY(),
                                npc.getOverheadText(),
                                npc.getComposition().getId(),
                                npc.isInteracting() ? npc.getInteracting().getName() : null,
                                npc.getCombatLevel()
                        );
                        alnp.add(np);
                    }
                } catch (Exception e) {
                    System.out.println("blew up getting npcs to kill");
                    System.out.println(e.getCause());
                    System.out.println(e.getMessage());
                }
            }
        }

        return alnp;
    }
}
