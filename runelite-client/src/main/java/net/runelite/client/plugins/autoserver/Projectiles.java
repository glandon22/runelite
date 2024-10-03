package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Projectile;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Projectiles {

    @Value
    public static class IncomingProjectile
    {
        double x;
        double y;
        int id;
        int startCycle;
        int endCycle;
        WorldPoint destination;
        String target;
    }
    public List<IncomingProjectile> getProjectiles(Client client) {
        List<IncomingProjectile> projs = new ArrayList<>();
        for (Projectile p : client.getTopLevelWorldView().getProjectiles()) {
            String target = null;
            if (p.getInteracting() != null) {
                target = p.getInteracting().getName();
            }
            IncomingProjectile ip = new IncomingProjectile(
                    p.getX(),
                    p.getY(),
                    p.getId(),
                    p.getStartCycle(),
                    p.getEndCycle(),
                    WorldPoint.fromLocal(client, p.getTarget()),
                    target
            );
            projs.add(ip);
        }
        return projs;
    }
}
