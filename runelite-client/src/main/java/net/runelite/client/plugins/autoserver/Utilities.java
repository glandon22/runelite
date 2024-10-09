package net.runelite.client.plugins.autoserver;

import net.runelite.api.Client;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Utilities {
    Interfaces interfaceHelper = new Interfaces();
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

    public boolean isClickable(Client client, Rectangle r) {
        Interfaces.CanvasData canvas = interfaceHelper.getCanvasData(client);
        Interfaces.EnrichedInterfaceData chatButtons = interfaceHelper.getWidget(client, "162,1");
        Interfaces.EnrichedInterfaceData invInterface = interfaceHelper.getWidget(client, "161,97");
        Interfaces.EnrichedInterfaceData worldMapInterface = interfaceHelper.getWidget(client, "161,95");
        HashMap<Character, Integer> center = getCenter(r, canvas.getXOffset(), canvas.getYOffset());
        Point centerPoint = new Point(center.get('x'), center.get('y'));
        Widget cb = client.getWidget(162, 1);
        Widget invWidg = client.getWidget(161, 97);
        Widget map = client.getWidget(161, 95);
        Rectangle gameScreen = new Rectangle(
                canvas.getXMin(),
                canvas.getYMin(),
                canvas.getXMax() - canvas.getXMin(),
                canvas.getYMax() - canvas.getYMin()
        );

        // point is outside of the client area
        if (!gameScreen.contains(centerPoint)) {
            return false;
        }

        if (cb != null && cb.contains(
                new net.runelite.api.Point(
                        (int)(r.getX() + r.getWidth() / 2),
                        (int)(r.getY() + r.getHeight() / 2)
                )
        )
        ) {
            return false;
        }

        if (map != null && map.contains(
                new net.runelite.api.Point(
                        (int)(r.getX() + r.getWidth() / 2),
                        (int)(r.getY() + r.getHeight() / 2)
                )
        )
        ) {
            return false;
        }

        if (invWidg != null && invWidg.contains(
                new net.runelite.api.Point(
                        (int)(r.getX() + r.getWidth() / 2),
                        (int)(r.getY() + r.getHeight() / 2)
                )
        )
        ) {
            return false;
        }

        return true;
    }
}
