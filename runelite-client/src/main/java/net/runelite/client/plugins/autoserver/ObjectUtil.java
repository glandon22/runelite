package net.runelite.client.plugins.autoserver;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import java.awt.*;
import java.util.*;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectUtil {
    private static final Logger logger = LoggerFactory.getLogger(ObjectUtil.class);

    @Inject
    private PluginManager pluginManager;


    public enum SearchObjectType {
        Wall,
        GroundObject,
        Game,
        Decorative
    }
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
    public static class GroundItemData
    {
        int x;
        int y;
        int dist;
        int x_coord;
        int y_coord;
        int id;
        int quantity;
    }

    @Value
    public static class ObjectAndGroundItemData
    {
        int x;
        int y;
        int dist;
        int x_coord;
        int y_coord;
        int id;
        int quantity;
        int ownership;
        int animation;
        int height;
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

    class SearchV2
    {
        JsonArray tiles;
        JsonArray objects;

        // Getters and Setters
    }

    static class allObjectSearch {
        JsonArray game;
        JsonArray wall;
        JsonArray decorative;
        JsonArray ground_items;
        JsonArray ground;
        JsonArray graphics;
        int dist;
    }

    Utilities u = new Utilities();
    Interfaces interfaceHelper = new Interfaces();

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
                logger.info("Failed to find game object data for tile: ");
                logger.info(s.toString());
            }
        }
        logger.info("Successfully parsed tiles and objects / npcs to find. Searching the following tiles: ");
        logger.info(new Gson().toJson(wps));
        return new ParsedTilesAndObjects(
                RELEVANT_OBJECTS,
                wps
        );
    }

    /**
     *
     * {
     *     'tiles': ['1,2,3','4,5,6'],
     *     'objects': ['1','2','3',]
     * }
     **/

    public ParsedTilesAndObjects parseTilesAndObjectsV2(JsonObject gameObjectsToFind) {
        HashSet<Integer> relevantObjects = new HashSet<>();
        ArrayList<WorldPoint> worldPoints = new ArrayList<>();
        try {
            Gson gson = new Gson();
            // Parse my input as SearchV2 class
            SearchV2 search = gson.fromJson(gameObjectsToFind, SearchV2.class);
            // Iterate through the tiles i passed
            for (JsonElement tile : search.tiles) {
                String[] tileCoords = tile.getAsString().split(",");
                worldPoints.add(
                        new WorldPoint(Integer.parseInt(tileCoords[0]), Integer.parseInt(tileCoords[1]),Integer.parseInt(tileCoords[2]))
                );
            }

            for (JsonElement object: search.objects) {
                relevantObjects.add(object.getAsInt());
            }
        } catch (Exception e) {
            logger.info("Failed to find game object data for tile: ");
            logger.info(e.getMessage());
        }
        return new ParsedTilesAndObjects(
                relevantObjects,
                worldPoints
        );
    }

    public HashMap<Integer, ArrayList<EnhancedObjData>> gameObjects(Client client, JsonObject gameObjectsToFind, SearchObjectType searchType) {
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
        HashMap<Integer, ArrayList<EnhancedObjData>> returnData = new HashMap<>();

        ParsedTilesAndObjects ptao = parseTilesAndObjectsV2(gameObjectsToFind);
        HashSet<Integer> RELEVANT_OBJECTS = ptao.RELEVANT_OBJECTS;
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);

            if (localLocation == null) {
                continue;
            }

            Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];

            if (tile == null) {
                continue;
            }

            TileObject[] objects;
            if (searchType == SearchObjectType.Decorative) {
                objects = new DecorativeObject[]{tile.getDecorativeObject()};
            }

            else if (searchType == SearchObjectType.Wall) {
                objects = new WallObject[]{tile.getWallObject()};
            }

            else if (searchType == SearchObjectType.GroundObject) {
                objects = new GroundObject[]{tile.getGroundObject()};
            }

            else {
                objects = tile.getGameObjects();
            }


            for (TileObject g : objects) {
                // Return all objects if not looking for a specific one
                if (
                        g != null && (RELEVANT_OBJECTS.contains(g.getId()) || RELEVANT_OBJECTS.isEmpty())
                                && g.getCanvasTilePoly() != null
                ) {
                    Shape poly = g.getClickbox();
                    if (poly == null) {
                        poly = g.getClickbox();
                    }
                    if (poly == null) {
                        continue;
                    }
                    Rectangle r = poly.getBounds();
                    HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
                    if (center.get('x') > 0 && center.get('x') < 1920 && center.get('y') > 0 && center.get('y') < 1035) {
                        EnhancedObjData data = new EnhancedObjData(
                                center.get('x'),
                                center.get('y'),
                                g.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                tile.getWorldLocation().getX(),
                                tile.getWorldLocation().getY(),
                                g.getId()
                        );

                        if (returnData.get(g.getId()) != null) {
                            ArrayList<EnhancedObjData> gobj = returnData.get(g.getId());
                            gobj.add(data);
                            returnData.put(g.getId(), gobj);
                        }

                        else {
                            ArrayList<EnhancedObjData> gobj = new ArrayList<>();
                            gobj.add(data);
                            returnData.put(g.getId(), gobj);
                        }
                    }
                }
            }
        }

        return returnData;
    }

    public HashMap<Integer, EnhancedObjData> findGameObjects(Client client, JsonArray gameObjectsToFind) {
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
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
                                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        logger.info("Successfully found game objects.");
        return returnData;
    }

    public HashMap<Integer, EnhancedObjData> findGroundObjects(Client client, JsonArray groundObjectsToFind) {
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
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
                        HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
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
                                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);

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
                            HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
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
                                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
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
                                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);

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
                            logger.info("wp coords");
                            logger.info(String.valueOf(wp.getX()));
                            logger.info(String.valueOf(wp.getY()));
                            final Polygon poly = Perspective.getCanvasTilePoly(client, localLocation);
                            Rectangle r = poly.getBounds();
                            HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
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

    public ArrayList<GroundItemData> getGroundItemsV2(Client client, JsonArray itemsToFind) {
        Interfaces.CanvasData canvasData = interfaceHelper.getCanvasData(client);
        ArrayList<GroundItemData> output = new ArrayList<>();
        ParsedTilesAndObjects ptao = parseTilesAndObjects(itemsToFind);
        ArrayList<WorldPoint> wps = ptao.wps;

        Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();
        Utilities u = new Utilities();
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client.getTopLevelWorldView(), wp);
            if (localLocation == null) continue;

            Tile tile = tiles[client.getTopLevelWorldView().getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];

            if (tile == null) continue;

            List<TileItem> groundItemList = tile.getGroundItems();

            if (groundItemList == null) continue;

            for (TileItem tileItem : groundItemList) {
                final Polygon poly = Perspective.getCanvasTilePoly(client, localLocation);
                Rectangle r = poly.getBounds();
                HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
                output.add(new GroundItemData(
                        center.get('x'),
                        center.get('y'),
                        tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                        wp.getX(),
                        wp.getY(),
                        tileItem.getId(),
                        tileItem.getQuantity()
                ));
            }
        }

        return output;
    }

    private HashMap<String, ArrayList<Integer>> allObjectQueryParser(JsonObject query) {
        HashMap<String, ArrayList<Integer>> res = new HashMap<>();
        Gson gson = new Gson();
        allObjectSearch search = gson.fromJson(query, allObjectSearch.class);
        ArrayList<Integer> gameObjectIds = new ArrayList<>();
        for (JsonElement e : search.game) {
            int id = Integer.parseInt(e.toString());
            gameObjectIds.add(id);
        }
        res.put("game", gameObjectIds);
        ArrayList<Integer> graphicObjectIds = new ArrayList<>();
        for (JsonElement e : search.graphics) {
            int id = Integer.parseInt(e.toString());
            graphicObjectIds.add(id);
        }
        res.put("graphics", graphicObjectIds);
        ArrayList<Integer> wallObjectIds = new ArrayList<>();
        for (JsonElement e : search.wall) {
            int id = Integer.parseInt(e.toString());
            wallObjectIds.add(id);
        }
        res.put("wall", wallObjectIds);
        ArrayList<Integer> decorObjectIds = new ArrayList<>();
        for (JsonElement e : search.decorative) {
            int id = Integer.parseInt(e.toString());
            decorObjectIds.add(id);
        }
        res.put("decorative", decorObjectIds);
        ArrayList<Integer> groundItemIds = new ArrayList<>();
        for (JsonElement e : search.ground_items) {
            int id = Integer.parseInt(e.toString());
            groundItemIds.add(id);
        }
        res.put("ground", groundItemIds);
        ArrayList<Integer> groundObjectIds = new ArrayList<>();
        for (JsonElement e : search.ground) {
            int id = Integer.parseInt(e.toString());
            groundObjectIds.add(id);
        }
        res.put("ground", groundObjectIds);
        ArrayList<Integer> distL = new ArrayList<>();
        distL.add(search.dist);
        res.put("dist", distL);
        return res;
    }

    public HashMap<String, ArrayList<ObjectAndGroundItemData>> getAllObjects(Client client, JsonObject itemsToFind) {
        HashMap<String, ArrayList<Integer>> parsedQuery = allObjectQueryParser(itemsToFind);
        Interfaces.CanvasData canvas = interfaceHelper.getCanvasData(client);
        ArrayList<ObjectAndGroundItemData> gameObjectData = new ArrayList<>();
        ArrayList<ObjectAndGroundItemData> wallObjectData = new ArrayList<>();
        ArrayList<ObjectAndGroundItemData> decorativeObjectData = new ArrayList<>();
        ArrayList<ObjectAndGroundItemData> groundObjectData = new ArrayList<>();
        ArrayList<ObjectAndGroundItemData> groundItemData = new ArrayList<>();
        ArrayList<ObjectAndGroundItemData> graphicsObjectData = new ArrayList<>();
        Tile[][][] tiles = client.getTopLevelWorldView().getScene().getTiles();

        int plane = client.getLocalPlayer().getWorldLocation().getPlane();
        for (int j = 0; j < 104; j++) {
            for (int k = 0; k < 104; k++) {
                Tile tile = tiles[plane][j][k];
                if (tile == null) continue;
                // This tile is too far away
                if (
                        parsedQuery.get("dist").get(0) != -1 &&
                                tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()) > parsedQuery.get("dist").get(0)
                ) {
                    continue;
                }
                GameObject[] gameObjects = tile.getGameObjects();
                if (!parsedQuery.get("game").isEmpty()) {
                    for (GameObject gameObject : gameObjects) {
                        if (gameObject != null && gameObject.getConvexHull() != null && parsedQuery.get("game").contains(gameObject.getId())) {
                            Shape s = gameObject.getClickbox();
                            if (s == null) continue;
                            Point center = u.findCenterPoint(s, canvas.getXOffset(), canvas.getYOffset());
                            if (u.isClickable(client, center)) {
                                int objAnimation = -1;
                                if (gameObject.getRenderable() instanceof DynamicObject)
                                {
                                    Animation animation = ((DynamicObject) gameObject.getRenderable()).getAnimation();
                                    if (animation != null)
                                    {
                                        objAnimation = animation.getId();
                                    }
                                }
                                gameObjectData.add(
                                        new ObjectAndGroundItemData(
                                                center.getX(),
                                                center.getY(),
                                                tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                                tile.getWorldLocation().getX(),
                                                tile.getWorldLocation().getY(),
                                                gameObject.getId(),
                                                1,
                                                -1,
                                                objAnimation,
                                                (int) gameObject.getConvexHull().getBounds().getHeight()
                                        )
                                );
                            }
                        }
                    }
                }

                if (!parsedQuery.get("wall").isEmpty()) {
                    WallObject wallObject = tile.getWallObject();
                    if (wallObject != null && wallObject.getConvexHull() != null && parsedQuery.get("wall").contains(wallObject.getId())) {
                        Shape s = wallObject.getClickbox();
                        if (s == null) continue;
                        Point center = u.findCenterPoint(s, canvas.getXOffset(), canvas.getYOffset());
                        if (u.isClickable(client, center)) {
                            wallObjectData.add(
                                    new ObjectAndGroundItemData(
                                            center.getX(),
                                            center.getY(),
                                            tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            tile.getWorldLocation().getX(),
                                            tile.getWorldLocation().getY(),
                                            wallObject.getId(),
                                            1,
                                            -1,
                                            -1,
                                            (int) wallObject.getConvexHull().getBounds().getHeight()
                                    )
                            );
                        }
                    }
                }

                if (!parsedQuery.get("ground").isEmpty()) {
                    GroundObject groundObject = tile.getGroundObject();
                    if (groundObject != null && groundObject.getConvexHull() != null && parsedQuery.get("ground").contains(groundObject.getId())) {
                        Shape s = groundObject.getClickbox();
                        if (s == null) continue;
                        Point center = u.findCenterPoint(s, canvas.getXOffset(), canvas.getYOffset());
                        if (u.isClickable(client, center)) {
                            groundObjectData.add(
                                    new ObjectAndGroundItemData(
                                            center.getX(),
                                            center.getY(),
                                            tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            tile.getWorldLocation().getX(),
                                            tile.getWorldLocation().getY(),
                                            groundObject.getId(),
                                            1,
                                            -1,
                                            -1,
                                            (int) groundObject.getConvexHull().getBounds().getHeight()
                                    )
                            );
                        }
                    }
                }

                if (!parsedQuery.get("decorative").isEmpty()) {
                    DecorativeObject decorativeObject = tile.getDecorativeObject();
                    if (decorativeObject != null && decorativeObject.getConvexHull() != null && parsedQuery.get("decorative").contains(decorativeObject.getId())) {
                        Shape s = decorativeObject.getClickbox();
                        if (s == null) continue;
                        Point center = u.findCenterPoint(s, canvas.getXOffset(), canvas.getYOffset());
                        if (u.isClickable(client, center)) {
                            decorativeObjectData.add(
                                    new ObjectAndGroundItemData(
                                            center.getX(),
                                            center.getY(),
                                            tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            tile.getWorldLocation().getX(),
                                            tile.getWorldLocation().getY(),
                                            decorativeObject.getId(),
                                            1,
                                            -1,
                                            -1,
                                            (int) decorativeObject.getConvexHull().getBounds().getHeight()
                                    )
                            );
                        }
                    }
                }

                List<TileItem> groundItemList = tile.getGroundItems();
                if (groundItemList != null) {
                    for (TileItem item : groundItemList) {
                        final Polygon poly = Perspective.getCanvasTilePoly(client, tile.getLocalLocation());
                        if (poly == null) continue;
                        Point center = u.findCenterPoint(poly, canvas.getXOffset(), canvas.getYOffset());
                        if (u.isClickable(client, center)) {
                            groundItemData.add(
                                    new ObjectAndGroundItemData(
                                            center.getX(),
                                            center.getY(),
                                            tile.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                            tile.getWorldLocation().getX(),
                                            tile.getWorldLocation().getY(),
                                            item.getId(),
                                            item.getQuantity(),
                                            item.getOwnership(),
                                            -1,
                                            -1
                                    )
                            );
                        }
                    }
                }
            }
        }

        if (!parsedQuery.get("graphics").isEmpty()) {
            Iterator<GraphicsObject> tt = client.getTopLevelWorldView().getGraphicsObjects().iterator();
            int ii = 0;
            while (tt.hasNext()) {
                ii++;
                GraphicsObject graphicsObject = tt.next();
                if (!parsedQuery.get("graphics").contains(graphicsObject.getId())) continue;
                LocalPoint lp = graphicsObject.getLocation();
                WorldPoint wp = WorldPoint.fromLocal(client, lp);
                Polygon poly = Perspective.getCanvasTilePoly(client, lp);
                if (poly == null) continue;
                Point center = u.findCenterPoint(poly, canvas.getXOffset(), canvas.getYOffset());
                if (u.isClickable(client, center)) {
                    graphicsObjectData.add(
                            new ObjectAndGroundItemData(
                                    center.getX(),
                                    center.getY(),
                                    wp.distanceTo2D(client.getLocalPlayer().getWorldLocation()),
                                    wp.getX(),
                                    wp.getY(),
                                    graphicsObject.getId(),
                                    -1,
                                    -1,
                                    graphicsObject.getAnimationFrame(),
                                    -1
                            )
                    );
                }
            }
            System.out.println("hhkjhkj");
            System.out.println(ii);
            System.out.println("new");

        }
        HashMap<String, ArrayList<ObjectAndGroundItemData>> returnData = new HashMap<>();
        System.out.println("ttttt");
        System.out.println(graphicsObjectData.size());
        returnData.put("game", gameObjectData);
        returnData.put("wall", wallObjectData);
        returnData.put("decorative", decorativeObjectData);
        returnData.put("ground", groundObjectData);
        returnData.put("ground_items", groundItemData);
        returnData.put("graphics", graphicsObjectData);
        return returnData;
    }
}
