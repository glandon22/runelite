package net.runelite.client.plugins.autoserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ObjectUtil {
    @Inject
    private PluginManager pluginManager;

    @Value
    public static class EnhancedObjData
    {
        int x;
        int y;
        int dist;
        int x_coord;
        int y_coord;
        int id;
    }

    @Value
    public static class ParsedTilesAndObjects
    {
        HashSet<Integer> RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps;
    }

    class Search
    {
        String tile;
        String object;

        // Getters and Setters
    }

    public ParsedTilesAndObjects parseTilesAndObjects(JsonArray gameObjectsToFind) {
        HashSet<Integer> RELEVANT_OBJECTS = new HashSet<>();
        ArrayList<WorldPoint> wps = new ArrayList<>();
        Gson gson = new Gson();
        Search[] searchArr = gson.fromJson(gameObjectsToFind, Search[].class);
        for(Search s: searchArr) {
            try {
                String tileHash = s.tile;
                int gameObjectToFind = Integer.parseInt(s.object);
                String[] tileCoords = tileHash.split(",");
                wps.add(
                        new WorldPoint(Integer.parseInt(tileCoords[0]), Integer.parseInt(tileCoords[1]),Integer.parseInt(tileCoords[2]))
                );
                RELEVANT_OBJECTS.add(gameObjectToFind);
            } catch (Exception e) {
                System.out.println("Failed to find game object data for tile: ");
                System.out.println(s);
            }
        }
        System.out.println("Successfully parsed tiles and objects / npcs to find. Searching the following tiles: ");
        System.out.println(new Gson().toJson(wps));
        return new ParsedTilesAndObjects(
                RELEVANT_OBJECTS,
                wps
        );
    }

    public HashMap<Integer, EnhancedObjData> findGameObjects(Client client, JsonArray gameObjectsToFind) {
        HashMap<Integer, EnhancedObjData> returnData = new HashMap<>();
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
                    GameObject[] go = tile.getGameObjects();
                    for (GameObject g : go) {
                        if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                            Shape poly = g.getClickbox();
                            if (poly != null) {
                                Rectangle r = poly.getBounds();
                                HashMap<Character, Integer> center = u.getCenter(r);
                                if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                    //return key value pair key object id and values of x adn y
                                    returnData.put(g.getId(), new EnhancedObjData(
                                            center.get('x'),
                                            center.get('y'),
                                            g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            tile.getWorldLocation().getX(),
                                            tile.getWorldLocation().getY(),
                                            g.getId()
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Successfully found game objects.");
        return returnData;
    }

    public HashMap<Integer, EnhancedObjData> findGroundObjects(Client client, JsonArray groundObjectsToFind) {
        HashMap<Integer, EnhancedObjData> returnData = new HashMap<>();

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
                            returnData.put(
                                    wo.getId(),
                                    new EnhancedObjData(
                                            center.get('x'),
                                            center.get('y'),
                                            wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            wp.getX(),
                                            wp.getY(),
                                            wo.getId()
                                    )
                            );
                        }
                    }
                }
            }
        }
        return returnData;
    }

    public HashMap<Integer, ArrayList<EnhancedObjData>> findWallObjects(Client client, JsonArray wallObjectsToFind) {
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

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
                                        ArrayList<EnhancedObjData> gobj = returnData.get(wo.getId());
                                        gobj.add(new EnhancedObjData(
                                                    center.get('x'),
                                                    center.get('y'),
                                                    wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                    tile.getWorldLocation().getX(),
                                                    tile.getWorldLocation().getY(),
                                                    wo.getId()
                                                )
                                        );
                                        returnData.put(wo.getId(), gobj);
                                    }

                                    else {
                                        ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                                        gobj.add(new EnhancedObjData(
                                                        center.get('x'),
                                                        center.get('y'),
                                                        wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                        tile.getWorldLocation().getX(),
                                                        tile.getWorldLocation().getY(),
                                                        wo.getId()
                                                )
                                        );
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

    public HashMap<Integer, ArrayList<EnhancedObjData>> findMultipleGameObjects(Client client, JsonArray gameObjectsToFind) {
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

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
                    GameObject[] go = tile.getGameObjects();
                    for (GameObject g : go) {
                        if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                            Shape poly = g.getClickbox();
                            Rectangle r = poly.getBounds();
                            HashMap<Character, Integer> center = u.getCenter(r);
                            if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                //return key value pair key object id and values of x adn y
                                //returnData.put(g.getId(), new GameObjData(center.get('x'), center.get('y'), g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation())));
                                if (returnData.get(g.getId()) != null) {
                                    ArrayList<EnhancedObjData> gobj = returnData.get(g.getId());
                                    gobj.add(
                                            new EnhancedObjData(
                                                    center.get('x'),
                                                    center.get('y'),
                                                    g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                    tile.getWorldLocation().getX(),
                                                    tile.getWorldLocation().getY(),
                                                    g.getId()
                                            )
                                    );
                                    returnData.put(g.getId(), gobj);
                                }

                                else {
                                    ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                                    gobj.add(
                                            new EnhancedObjData(
                                                    center.get('x'),
                                                    center.get('y'),
                                                    g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                    tile.getWorldLocation().getX(),
                                                    tile.getWorldLocation().getY(),
                                                    g.getId()
                                            )
                                    );
                                    returnData.put(g.getId(), gobj);
                                }
                            }
                        }
                    }
                }
            }
        }

        return returnData;
    }

    public HashMap<Integer, ArrayList<EnhancedObjData>> findDecorativeObjects(Client client, JsonArray wallObjectsToFind) {
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

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
                                        ArrayList<EnhancedObjData> gobj = returnData.get(wo.getId());
                                        gobj.add(new EnhancedObjData(
                                                center.get('x'),
                                                center.get('y'),
                                                wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                wp.getX(),
                                                wp.getY(),
                                                wo.getId()
                                        ));
                                        returnData.put(wo.getId(), gobj);
                                    }

                                    else {
                                        ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                                        gobj.add(new EnhancedObjData(
                                                center.get('x'),
                                                center.get('y'),
                                                wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                wp.getX(),
                                                wp.getY(),
                                                wo.getId()
                                        ));
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

    public HashMap<Integer, ArrayList<EnhancedObjData>> getGroundItems(Client client, JsonArray itemsToFind) {
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(itemsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    List<TileItem> wo = tile.getGroundItems();
                    if (wo != null) {
                        for (TileItem ti: wo) {
                            // 9999999 is a hack to search for any items on a tile. I should improve this
                            if (RELEVANT_OBJECTS.contains(9999999) || RELEVANT_OBJECTS.contains(ti.getId())) {
                                final Polygon poly = Perspective.getCanvasTilePoly(client, localLocation);
                                Rectangle r = poly.getBounds();
                                HashMap<Character, Integer> center = u.getCenter(r);
                                if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                    if (returnData.get(ti.getId()) != null) {
                                        ArrayList<EnhancedObjData> gobj = returnData.get(ti.getId());
                                        gobj.add(
                                                new EnhancedObjData(
                                                        center.get('x'),
                                                        center.get('y'),
                                                        tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                        wp.getX(),
                                                        wp.getY(),
                                                        ti.getId()
                                                )
                                        );
                                        returnData.put(ti.getId(), gobj);
                                    }

                                    else {
                                        ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                                        gobj.add(
                                                new EnhancedObjData(
                                                        center.get('x'),
                                                        center.get('y'),
                                                        tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                        wp.getX(),
                                                        wp.getY(),
                                                        ti.getId()
                                                )
                                        );
                                        returnData.put(ti.getId(), gobj);
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

    public HashMap<Integer, ArrayList<EnhancedObjData>> getGroundItemsAnyId(Client client, JsonArray itemsToFind) {
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjects(itemsToFind);
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    List<TileItem> wo = tile.getGroundItems();
                    if (wo != null) {
                        for (TileItem ti: wo) {
                            System.out.println("wp coords");
                            System.out.println(wp.getX());
                            System.out.println(wp.getY());
                            final Polygon poly = Perspective.getCanvasTilePoly(client, localLocation);
                            Rectangle r = poly.getBounds();
                            HashMap<Character, Integer> center = u.getCenter(r);
                            if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                                if (returnData.get(ti.getId()) != null) {
                                    ArrayList<EnhancedObjData> gobj = returnData.get(ti.getId());
                                    gobj.add(new EnhancedObjData(
                                            center.get('x'),
                                            center.get('y'),
                                            tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            wp.getX(),
                                            wp.getY(),
                                            ti.getId())
                                    );
                                    returnData.put(ti.getId(), gobj);
                                }

                                else {
                                    ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                                    gobj.add(
                                            new EnhancedObjData(
                                                    center.get('x'),
                                                    center.get('y'),
                                                    tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                    wp.getX(),
                                                    wp.getY(),
                                                    ti.getId()
                                            )
                                    );
                                    returnData.put(ti.getId(), gobj);
                                }

                            }
                        }
                    }
                }
            }
        }
        return returnData;
    }

    public void getInteractedObject(Client client) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Plugin qhp = pluginManager.getPlugins().stream()
                .filter(e -> e.getName().equals("Interact Highlight"))
                .findAny().orElse(null);
        if (qhp == null) return;

        Object qh = qhp.getClass().getMethod("interacting").invoke(qhp);
        if (qh == null) return;
        System.out.println("testing refelction");
        System.out.println(qh);
    }
}
