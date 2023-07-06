package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Tiles {
    @Value
    public static class TileData {
        int x;
        int y;
    }

    public HashMap<String,TileData> getTileData(Client client, JsonArray tilesToFind) {
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
                        Rectangle r = poly.getBounds();
                        HashMap<Character, Integer> center = u.getCenter(r);
                        if (
                                center.get('x') > 0 && center.get('x') < 1920 &&
                                        center.get('y') > 0 && center.get('y') < 1035
                        ) {
                            String parsedKey = Integer.toString(wp.getX()) + Integer.toString(wp.getY()) + Integer.toString(wp.getPlane());
                            String parsedKeyV2 = Integer.toString(
                                    wp.getX()) +
                                    ',' +
                                    Integer.toString(wp.getY()) +
                                    ',' +
                                    Integer.toString(wp.getPlane()
                            );
                            tileDataPacket.put(parsedKey, new TileData(center.get('x'), center.get('y')));
                            tileDataPacket.put(parsedKeyV2, new TileData(center.get('x'), center.get('y')));
                        }
                    }
                }
            }
        }

        return tileDataPacket;
    }
}
