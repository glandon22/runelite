package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Projectile;

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
    }
    public List<IncomingProjectile> getProjectiles(Client client) {
        List<IncomingProjectile> projs = new ArrayList<>();
        for (Projectile p : client.getProjectiles()) {
            IncomingProjectile ip = new IncomingProjectile(
                    p.getX(),
                    p.getY(),
                    p.getId(),
                    p.getStartCycle(),
                    p.getEndCycle()
            );
            projs.add(ip);
        }
        return projs;
    }
}
