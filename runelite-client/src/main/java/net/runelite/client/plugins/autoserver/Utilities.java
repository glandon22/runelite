package net.runelite.client.plugins.autoserver;

import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Utilities {
    public static class PointData {
        int x;
        int y;
        int z;
    }

    public HashMap<Character, Integer> getCenter(@NotNull Rectangle r, int xOffset, int yOffset) {
        System.out.println(r.getX());
        System.out.println(xOffset);
        System.out.println(r.getY());
        System.out.println(yOffset);
        double x = r.getX() + xOffset;
        double y = r.getY() + yOffset;
        double w = r.getWidth();
        double h = r.getHeight();
        int cx = (int)(x + (w/2));
        int cy = (int)(y + (h /2));
        HashMap<Character, Integer> ret = new HashMap<Character, Integer>();
        ret.put('x', cx);
        ret.put('y', cy);
        return ret;
    }

    public void setYaw(Client client, Integer value) {
        try {
            client.setCameraYawTarget(value);
        } catch (Exception e) {
            System.out.println("eeee");
            System.out.println(e);
        }
    }

    public PointData getPlayerWorldPoint(Client client) {
        Interfaces i = new Interfaces();
        PointData pd = new PointData();
        WorldView wv = client.getTopLevelWorldView();
        List<net.runelite.api.Player> players = wv == null ? Collections.emptyList() : wv.players()
                .stream()
                .collect(Collectors.toCollection(ArrayList::new));
        for (net.runelite.api.Player player : players) {
            if (Objects.requireNonNull(player.getName()).equalsIgnoreCase(Objects.requireNonNull(client.getLocalPlayer().getName()))) {
                pd.x = player.getWorldLocation().getX();
                pd.y = player.getWorldLocation().getY();
                pd.z = player.getWorldLocation().getPlane();
            }
        }
        return pd;
    }
}
