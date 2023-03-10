package net.runelite.client.plugins.autoserver;

import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.HashMap;

public class Utilities {
    public static class PointData {
        int x;
        int y;
        int z;
    }

    public HashMap<Character, Integer> getCenter(@NotNull Rectangle r) {
        double x = r.getX();
        double y = r.getY();
        double w = r.getWidth();
        double h = r.getHeight();
        int cx = (int)(x + (w/2));
        int cy = (int)(y + 23 + (h /2));
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
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        PointData pd = new PointData();
        pd.x = wp.getX();
        pd.y = wp.getY();
        pd.z = wp.getPlane();
        return pd;
    }
}
