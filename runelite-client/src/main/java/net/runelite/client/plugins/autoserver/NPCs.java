package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.autobotstander.AutoBotStanderPlugin;

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
                NpcPacket np = new NpcPacket(
                        center.get('x'),
                        center.get('y'),
                        npc.getName(),
                        npc.getId(),
                        npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())
                );
                alnp.add(np);
            }
        }

        return alnp;
    }
}
