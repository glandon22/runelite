package net.runelite.client.plugins.autoserver;

import com.monst.polylabel.PolyLabel;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.geom.PathIterator;
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

    public net.runelite.api.Point findCenterPoint(@NotNull Shape shape, int xOffset, int yOffset) {
        Rectangle r = shape.getBounds();
        if (shape.contains(new java.awt.Point((int) r.getX(), (int) r.getY()))) {
            System.out.println("Obj is a standard poly and contains center points. Lazy calculating.");
            double x = r.getX() + xOffset;
            double y = r.getY() + yOffset;
            double w = r.getWidth();
            double h = r.getHeight();
            int cx = (int)(x + (w/2));
            int cy = (int)(y + (h /2));
            return new Point(cx, cy);
        }

        final double flatness = 0.1;
        PathIterator pi = shape.getPathIterator(null, flatness);
        double coords[] = new double[6];
        ArrayList<Integer[]> coord_list = new ArrayList<>();

        while (!pi.isDone())
        {
            int s = pi.currentSegment(coords);
            switch (s)
            {
                case PathIterator.SEG_MOVETO:

                case PathIterator.SEG_CLOSE:
                    // Ignore
                    break;

                case PathIterator.SEG_LINETO:
                    Integer[] cc = new Integer[2];
                    cc[0] = (int) coords[0];
                    cc[1] = (int) coords[1];
                    coord_list.add(cc);
                    break;

                case PathIterator.SEG_QUADTO:
                    throw new AssertionError(
                            "SEG_QUADTO in flattening path iterator");
                case PathIterator.SEG_CUBICTO:
                    throw new AssertionError(
                            "SEG_CUBICTO in flattening path iterator");
            }
            pi.next();
        }
        int len = coord_list.size();
        Integer[][] holder = new Integer[len][2];
        for (int i = 0; i < coord_list.size(); i++) {
            holder[i] = coord_list.get(i);
        }
        Integer[][][] final_list = {holder};
        PolyLabel polyLabel = PolyLabel.polyLabel(final_list);
        return new Point((int)polyLabel.getX() + xOffset, (int)polyLabel.getY() + yOffset);
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

    public boolean isClickable(Client client, net.runelite.api.Point centerPoint) {
        Interfaces.CanvasData canvas = interfaceHelper.getCanvasData(client);
        Interfaces.EnrichedInterfaceData chatButtons = interfaceHelper.getWidget(client, "162,1");
        Interfaces.EnrichedInterfaceData invInterface = interfaceHelper.getWidget(client, "161,97");
        Interfaces.EnrichedInterfaceData worldMapInterface = interfaceHelper.getWidget(client, "161,95");
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
        if (!gameScreen.contains(new java.awt.Point(gameScreen.x, gameScreen.y))) {
            return false;
        }

        if (cb != null && cb.contains(
                centerPoint
        )
        ) {
            return false;
        }

        if (map != null && map.contains(centerPoint)
        ) {
            return false;
        }

        if (invWidg != null && invWidg.contains(centerPoint)
        ) {
            return false;
        }

        return true;
    }
}
