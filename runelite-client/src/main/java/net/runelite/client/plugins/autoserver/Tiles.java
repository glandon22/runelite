package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Tiles {
    @Value
    public static class TileData {
        int x;
        int y;
        int x_coord;
        int y_coord;
        int dist;
    }

    public HashMap<String,TileData> getTileData(Client client, JsonArray tilesToFind) {
        Interfaces i = new Interfaces();
        Interfaces.CanvasData canvasData = i.getCanvasData(client);

        ArrayList<WorldPoint> wps = new ArrayList<>();
        HashMap<String, TileData> tileDataPacket = new HashMap<>();
        for (JsonElement elem : tilesToFind) {
            try {
                String tileHash = elem.toString().replace("\"", "");
                String[] tileCoords = tileHash.split(",");
                wps.add(
                        new WorldPoint(Integer.parseInt(tileCoords[0]), Integer.parseInt(tileCoords[1]),Integer.parseInt(tileCoords[2]))
                );
            } catch (Exception e) {
                System.out.println("Failed to find tile data for tile: ");
                System.out.println(elem);
            }
        }

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    final LocalPoint location = LocalPoint.fromWorld(client, tile.getWorldLocation());
                    if (location != null) {
                        final Polygon poly = Perspective.getCanvasTilePoly(client, location);
                        if (poly != null) {
                            Point center = u.findCenterPoint(poly, canvasData.getXOffset(), canvasData.getYOffset());
                            if (u.isClickable(client, center)) {
                                String parsedKey = Integer.toString(wp.getX()) + Integer.toString(wp.getY()) + Integer.toString(wp.getPlane());
                                String parsedKeyV2 = Integer.toString(
                                        wp.getX()) +
                                        ',' +
                                        Integer.toString(wp.getY()) +
                                        ',' +
                                        Integer.toString(wp.getPlane()
                                        );
                                tileDataPacket.put(parsedKey, new TileData(
                                        center.getX(),
                                        center.getY(),
                                        wp.getX(),
                                        wp.getY(),
                                        tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())
                                ));
                                tileDataPacket.put(
                                        parsedKeyV2,
                                        new TileData(
                                                center.getX(),
                                                center.getY(),
                                                wp.getX(),
                                                wp.getY(),
                                                tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }

        return tileDataPacket;
    }
}
