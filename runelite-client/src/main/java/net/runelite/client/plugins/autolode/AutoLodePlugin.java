package net.runelite.client.plugins.autolode;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


@PluginDescriptor(
        name = "AutoLode",
        description = "nukes the motherchode mine",
        tags = {"bot"},
        enabledByDefault = false
)
public class AutoLodePlugin extends Plugin {
    private static final Set<Integer> RELEVANT_OBJECTS = ImmutableSet.of(
            ObjectID.ROCKFALL_26680,
            ObjectID.HOPPER_26674,
            ObjectID.BANK_DEPOSIT_BOX_25937
    );

    @Value
    private static class SlotToDrop
    {
        double x;
        double y;
        int index;
        int id;
        int quantity;
    }

    @Value
    private static class rockData
    {
        double x;
        double y;
        int id;
        int dist;
    }

    @Value
    private static class gameObjData
    {
        int x;
        int y;
        int id;
    }

    @Value
    private static class steppingTiles
    {
        int x;
        int y;
    }

    @Value
    private static class MiningInfo
    {
        boolean isMining;
        ArrayList<rockData> oreVeins;
        gameObjData bank;
        gameObjData oreCart;
        gameObjData oreSack;
        gameObjData rockfallTile;
        int oreCount;
        steppingTiles i1;
        steppingTiles i2;
        steppingTiles rockfallObject;
        List<SlotToDrop> inv;
    }

    private OkHttpClient okClient = new OkHttpClient();

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:2223/");
        MediaType mt = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mt, json);

        Request request = new Request.Builder().url(url).post(body).build();
        okClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                //log.warn("Failure");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                response.close();
            }
        });
    }


    @Inject
    private Client client;

    private static WidgetItem getWidgetItem(Widget parentWidget, int idx)
    {
        if (parentWidget.isIf3())
        {
            Widget wi = parentWidget.getChild(idx);
            return new WidgetItem(wi.getItemId(), wi.getItemQuantity(), -1, wi.getBounds(), parentWidget, wi.getBounds());
        }
        else
        {
            return parentWidget.getWidgetItem(idx);
        }
    }


    @Override
    protected void startUp() throws Exception
    {

    }

    @Override
    protected void shutDown() throws Exception
    {
        return;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        int oreCount = client.getVarbitValue(Varbits.SACK_NUMBER);
        gameObjData bank = null;
        gameObjData oreCart = null;
        gameObjData oreSack = null;
        gameObjData rockfall = null;
        steppingTiles i1 = null;
        steppingTiles i2 = null;
        steppingTiles rockFallObject = null;
        List<SlotToDrop> inv = null;
        Player local = client.getLocalPlayer();
        int animId = local.getAnimation();

        Pickaxe pickaxe = Pickaxe.fromAnimation(animId);
        boolean isMining = pickaxe != null && (pickaxe.matchesMiningAnimation(client.getLocalPlayer()) || client.getLocalPlayer().getAnimation() == AnimationID.DENSE_ESSENCE_CHIPPING);

        ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
        if (ic != null) {
            Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
            inv = new ArrayList<SlotToDrop>();
            for (int i = 0; i < items.length; ++i) {
                if (items[i] != null && items[i].getId() > 0) {
                    final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                    final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                    double cx = r.getX() + (r.getWidth() / 2);
                    double cy = r.getY() + 23 + (r.getHeight() / 2);
                    SlotToDrop slot = new SlotToDrop(cx, cy, i, items[i].getId(), items[i].getQuantity());
                    inv.add(slot);
                }
            }
        }

        ArrayList<WorldPoint> wps = new ArrayList<>();
        ArrayList<rockData> mps = new ArrayList<>();
        Tile[][][] tiles = client.getScene().getTiles();
        wps.add(new WorldPoint(3720, 5654, 0));
        wps.add(new WorldPoint(3719, 5654, 0));
        wps.add(new WorldPoint(3718, 5653, 0));
        wps.add(new WorldPoint(3718, 5652, 0));
        wps.add(new WorldPoint(3718, 5651, 0));
        wps.add(new WorldPoint(3720, 5654, 0));
        wps.add(new WorldPoint(3719, 5654, 0));
        wps.add(new WorldPoint(3721, 5653, 0));
        wps.add(new WorldPoint(3721, 5652, 0));
        wps.add(new WorldPoint(3714, 5657, 0));
        wps.add(new WorldPoint(3714, 5656, 0));
        wps.add(new WorldPoint(3714, 5655, 0));
        wps.add(new WorldPoint(3715, 5653, 0));
        wps.add(new WorldPoint(3715, 5652, 0));
        wps.add(new WorldPoint(3715, 5651, 0));
        wps.add(new WorldPoint(3715, 5647, 0));
        wps.add(new WorldPoint(3715, 5646, 0));
        wps.add(new WorldPoint(3715, 5645, 0));
        wps.add(new WorldPoint(3759, 5664, 0)); //bank
        wps.add(new WorldPoint(3748, 5672, 0)); //ore cart
        wps.add(new WorldPoint(3727, 5652, 0)); //rock fall
        wps.add(new WorldPoint(3748, 5659, 0)); //ore sack
        wps.add(new WorldPoint(3748, 5655, 0)); //i1, closest to ore sack
        wps.add(new WorldPoint(3737, 5652, 0)); //i2
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
                Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
                if (tile != null) {
                    //bank
                    if ((wp.getX() == 3759 && wp.getY() == 5664) || (wp.getX() == 3748 && wp.getY() == 5672)) {
                        GameObject[] go = tile.getGameObjects();
                        for (GameObject g : go) {
                            if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                                Polygon poly = g.getCanvasTilePoly();
                                Rectangle r = poly.getBounds();
                                double x = r.getX();
                                double y = r.getY();
                                double w = r.getWidth();
                                double h = r.getHeight();
                                int cx = (int)(x + (w/2));
                                int cy = (int)(y + 23 + (h /2));
                                if (cx > 0 && cx < 1920 && cy > 0 && cy < 1035) {
                                    if (wp.getX() == 3759 && wp.getY() == 5664) {
                                        bank = new gameObjData(cx, cy, 25937);
                                    }

                                    else {
                                        oreCart = new gameObjData(cx, cy, 26674);
                                    }
                                }
                            }
                        }
                    }

                    else if ((wp.getX() == 3727 && wp.getY() == 5652) || (wp.getX() == 3748 && wp.getY() == 5659)) {
                        GroundObject wo = tile.getGroundObject();
                        Shape s = wo.getClickbox();
                        if (s == null) {
                            continue;
                        }
                        Rectangle r = s.getBounds();
                        double x = r.getX();
                        double y = r.getY();
                        double w = r.getWidth();
                        double h = r.getHeight();
                        int cx = (int)(x + (w/2));
                        int cy = (int)(y + 23 + (h /2));
                        if (cx > 0 && cx < 1920 && cy > 0 && cy < 1035) {
                            gameObjData gob = new gameObjData(cx, cy, wo.getId());
                            if (wo.getId() == 325) {
                                rockfall = gob;
                                GameObject[] go = tile.getGameObjects();
                                for (GameObject g : go) {
                                    if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                                        Polygon poly = g.getCanvasTilePoly();
                                        r = poly.getBounds();
                                        x = r.getX();
                                        y = r.getY();
                                        w = r.getWidth();
                                        h = r.getHeight();
                                        cx = (int)(x + (w/2));
                                        cy = (int)(y + 23 + (h /2));
                                        if (cx > 0 && cx < 1900 && cy > 0 && cy < 1000) {
                                            rockFallObject = new steppingTiles(cx, cy);
                                        }
                                    }
                                }
                            }

                            else {
                                oreSack = gob;
                            }
                        }
                    }

                    else if ((wp.getX() == 3748 && wp.getY() == 5655) || (wp.getX() == 3737 && wp.getY() == 5652)) {
                        final LocalPoint location = LocalPoint.fromWorld(client, tile.getWorldLocation());
                        if (location == null) {continue;}
                        final Polygon poly = Perspective.getCanvasTilePoly(client, location);
                        Rectangle r = poly.getBounds();
                        double x = r.getX();
                        double y = r.getY();
                        double w = r.getWidth();
                        double h = r.getHeight();
                        int cx = (int)(x + (w/2));
                        int cy = (int)(y + 23 + (h /2));
                        if (cx > 0 && cx < 1920 && cy > 0 && cy < 1035) {
                            steppingTiles st = new steppingTiles(cx, cy);
                            if (wp.getX() == 3748) {
                                i1 = st;
                            }

                            else {
                                i2 = st;
                            }
                        }
                    }

                    else {
                        WallObject wo = tile.getWallObject();

                        if (wo == null) {continue;}
                        if (wo.getId() < 26662 || wo.getId() > 26664) {continue;}
                        Shape s = wo.getClickbox();
                        if (s == null) {continue;}
                        Rectangle r = s.getBounds();
                        double x = r.getX();
                        double y = r.getY();
                        double w = r.getWidth();
                        double h = r.getHeight();
                        double cx = x + (w/2);
                        double cy = y + 23 + (h /2);
                        if (cx > 0 && cx < 1920 && cy > 0 && cy < 1035) {
                            rockData wod = new rockData(cx, cy, wo.getId(), wo.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()));
                            mps.add(wod);
                        }
                    }
                }
            }
        }

        post(new MiningInfo(isMining, mps, bank, oreCart, oreSack, rockfall, oreCount, i1, i2, rockFallObject, inv));
    }
}
