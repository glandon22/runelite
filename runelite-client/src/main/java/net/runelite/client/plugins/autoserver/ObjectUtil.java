package net.runelite.client.plugins.autoserver;

import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class ObjectUtil {
    @Value
    public static class GameObjData
    {
        int x;
        int y;
        int dist;
    }

    @Value
    public static class ParsedTilesAndObjects
    {
        HashSet<Integer> RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps;
    }

    public ParsedTilesAndObjects parseTilesAndObjects(Object gameObjectsToFind) {
        HashSet<Integer> RELEVANT_OBJECTS = new HashSet<>();
        ArrayList<WorldPoint> wps = new ArrayList<>();

        JSONArray jsonGameObjsAndTilesToFind = (JSONArray) gameObjectsToFind;
        Object[] parsedGameObjsAndTiles = jsonGameObjsAndTilesToFind.toArray();
        for (Object t : parsedGameObjsAndTiles) {
            try {
                JSONObject tileAndObject = (JSONObject) t;
                String tileHash = (String) tileAndObject.get("tile");
                int gameObjectToFind = Integer.parseInt((String) tileAndObject.get("object"));
                String[] tileCoords = tileHash.split(",");
                wps.add(
                        new WorldPoint(Integer.parseInt(tileCoords[0]), Integer.parseInt(tileCoords[1]),Integer.parseInt(tileCoords[2]))
                );
                RELEVANT_OBJECTS.add(gameObjectToFind);
            } catch (Exception e) {
                System.out.println("Failed to find game object data for tile: ");
                System.out.println(t);
            }
        }

        return new ParsedTilesAndObjects(
                RELEVANT_OBJECTS,
                wps
        );
    }

    public HashMap<Integer, GameObjData> findGameObjects(Client client, Object gameObjectsToFind) {
        HashMap<Integer, GameObjData> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(gameObjectsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    System.out.println("do");
                    System.out.println(tile.getDecorativeObject().getCanvasTilePoly().getBounds().getX());
                    GameObject[] go = tile.getGameObjects();
                    for (GameObject g : go) {
                        if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                            Polygon poly = g.getCanvasTilePoly();
                            Rectangle r = poly.getBounds();
                            HashMap<Character, Integer> center = u.getCenter(r);
                            if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                //return key value pair key object id and values of x adn y
                                returnData.put(g.getId(), new GameObjData(center.get('x'), center.get('y'), g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                            }
                        }
                    }
                }
            }
        }

        return returnData;
    }

    public HashMap<Integer, GameObjData> findGroundObjects(Client client, Object groundObjectsToFind) {
        HashMap<Integer, GameObjData> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(groundObjectsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    GroundObject wo = tile.getGroundObject();
                    if (wo != null && RELEVANT_OBJECTS.contains(wo.getId())) {
                        Shape s = wo.getClickbox();
                        if (s == null) {
                            continue;
                        }
                        Rectangle r = s.getBounds();
                        HashMap<Character, Integer> center = u.getCenter(r);
                        if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                            returnData.put(wo.getId(), new GameObjData(center.get('x'), center.get('y'), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                        }
                    }
                }
            }
        }
        return returnData;
    }

    public HashMap<Integer, ArrayList<GameObjData>> findWallObjects(Client client, Object wallObjectsToFind) {
        HashMap<Integer, ArrayList<GameObjData>> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(wallObjectsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    WallObject wo = tile.getWallObject();
                    if (wo != null) {
                        if (RELEVANT_OBJECTS.contains(wo.getId())) {
                            Shape s = wo.getClickbox();
                            if (s != null) {
                                Rectangle r = s.getBounds();
                                HashMap<Character, Integer> center = u.getCenter(r);
                                if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                    if (returnData.get(wo.getId()) != null) {
                                        ArrayList<GameObjData> gobj = returnData.get(wo.getId());
                                        gobj.add(new GameObjData(center.get('x'), center.get('y'), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                                        returnData.put(wo.getId(), gobj);
                                    }

                                    else {
                                        ArrayList<GameObjData> gobj = new ArrayList<>();
                                        gobj.add(new GameObjData(center.get('x'), center.get('y'), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                                        returnData.put(wo.getId(), gobj);
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        return returnData;
    }

    public HashMap<Integer, ArrayList<GameObjData>> findDecorativeObjects(Client client, Object wallObjectsToFind) {
        HashMap<Integer, ArrayList<GameObjData>> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(wallObjectsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    DecorativeObject wo = tile.getDecorativeObject();
                    if (wo != null) {
                        if (RELEVANT_OBJECTS.contains(wo.getId())) {
                            Shape s = wo.getClickbox();
                            if (s != null) {
                                Rectangle r = s.getBounds();
                                HashMap<Character, Integer> center = u.getCenter(r);
                                if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                    if (returnData.get(wo.getId()) != null) {
                                        ArrayList<GameObjData> gobj = returnData.get(wo.getId());
                                        gobj.add(new GameObjData(center.get('x'), center.get('y'), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                                        returnData.put(wo.getId(), gobj);
                                    }

                                    else {
                                        ArrayList<GameObjData> gobj = new ArrayList<>();
                                        gobj.add(new GameObjData(center.get('x'), center.get('y'), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                                        returnData.put(wo.getId(), gobj);
                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        return returnData;
    }
}
