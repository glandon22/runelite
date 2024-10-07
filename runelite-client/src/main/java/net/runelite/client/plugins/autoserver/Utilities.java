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

        if (chatButtons != null) {
            Rectangle chatArea = new Rectangle(
                    chatButtons.getXMin(),
                    chatButtons.getYMin(),
                    chatButtons.getXMax() - chatButtons.getXMin(),
                    chatButtons.getYMax() - chatButtons.getYMin()
            );

            // Point is over one of the chat buttons
            if (chatArea.contains(centerPoint)) {
                return false;
            }

        }

        // point is over the world map
        if (worldMapInterface != null) {
            Rectangle worldMapArea = new Rectangle(
                    worldMapInterface.getXMin(),
                    canvas.getYOffset(),
                    worldMapInterface.getXMax() - worldMapInterface.getXMin(),
                    worldMapInterface.getYMax() - worldMapInterface.getYMin()
            );

            if (worldMapArea.contains(centerPoint)) {
                return false;
            }

        }

        // point is behind the inventory
        if (invInterface != null) {
            Rectangle invArea = new Rectangle(
                    invInterface.getXMin(),
                    invInterface.getYMin(),
                    invInterface.getXMax() - invInterface.getXMin(),
                    invInterface.getYMax() - invInterface.getYMin()
            );

            if (invArea.contains(centerPoint)) {
                return false;
            }

        }
        return true;
    }
}
