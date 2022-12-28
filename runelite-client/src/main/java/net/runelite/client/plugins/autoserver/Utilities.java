package net.runelite.client.plugins.autoserver;

import java.awt.*;
import java.util.HashMap;

public class Utilities {
    public HashMap<Character, Integer> getCenter(Rectangle r) {
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
}
