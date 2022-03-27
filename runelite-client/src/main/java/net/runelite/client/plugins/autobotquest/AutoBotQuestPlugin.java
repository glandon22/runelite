package net.runelite.client.plugins.autobotquest;

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
import java.util.*;
import java.util.List;


@PluginDescriptor(
        name = "AutoBotQuest",
        description = "Does some stuff with fishing data",
        tags = {"quest", "bot"},
        enabledByDefault = false
)

public class AutoBotQuestPlugin extends Plugin {

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

    private static final Set<Integer> RELEVANT_NPCS = ImmutableSet.of(
            NpcID.KAQEMEEX,
            NpcID.SANFEW
    );

    private static final Set<Integer> RELEVANT_OBJECTS = ImmutableSet.of(
            ObjectID.STAIRCASE_16671,
            ObjectID.LADDER_16680,
            ObjectID.CAULDRON_OF_THUNDER,
            //top of 16671, find this to go down
            ObjectID.STAIRCASE_16673,
            ObjectID.PRISON_DOOR_2143,
            ObjectID.LADDER_17385
    );
    @Inject
    private Client client;

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
    private static class playerLocation
    {
        int x;
        int y;
        int z;
        String type;
    }


    @Value
    private static class targetToClick
    {
        double x;
        double y;
        String name;
        int Id;
    }

    @Value
    private static class dataPacket
    {
        playerLocation world;
        playerLocation local;
        List<targetToClick> npcs;
        List<targetToClick> objects;
        List<SlotToDrop> inv;
    }

    private ArrayList<targetToClick> npcsToClick = new ArrayList<targetToClick>();
    private ArrayList<targetToClick> objectsToClick = new ArrayList<targetToClick>();

    private OkHttpClient okClient = new OkHttpClient();

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:1489/");
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

    @Subscribe
    public void onGameTick(GameTick event) {
        ArrayList<WorldPoint> alwp = new ArrayList<WorldPoint>();
        alwp.add(new WorldPoint(2899, 3429, 0));
        alwp.add(new WorldPoint(2884, 3397, 0));
        alwp.add(new WorldPoint(2893, 9831, 0));
        alwp.add(new WorldPoint(2898, 3428, 1));
        alwp.add(new WorldPoint(2889, 9831, 0));
        alwp.add(new WorldPoint(2884, 9797, 0));
        ArrayList<targetToClick> objectsToClickTemp = new ArrayList<targetToClick>();
        for (WorldPoint wpx : alwp) {
            if (wpx == null) {continue;}
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wpx);

            if (localLocation == null) {continue;}

            Tile[][][] tiles = client.getScene().getTiles();
            final Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
            if ((wpx.getX() == 2889 && wpx.getY() == 9831) ) {
                WallObject wo = tile.getWallObject();
                if (wo == null) {continue;}
                Shape s = wo.getClickbox();
                if (s == null) {continue;}
                Rectangle r = s.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = x + (w/2);
                double cy = y + 23 + (h /2);
                targetToClick tc = new targetToClick(cx, cy, "na", wo.getId());
                objectsToClickTemp.add(tc);
            }
            GameObject[] go = tile.getGameObjects();
            for (GameObject g : go) {
                if (g != null && RELEVANT_OBJECTS.contains(g.getId()) && g.getCanvasTilePoly() != null) {
                    Polygon poly = g.getCanvasTilePoly();
                    Rectangle r = poly.getBounds();
                    double x = r.getX();
                    double y = r.getY();
                    double w = r.getWidth();
                    double h = r.getHeight();
                    double cx = x + (w/2);
                    double cy = y + 23 + (h /2);
                    if (cx > 0 && cx < 1900 && cy > 0 && cy < 1000) {
                        objectsToClickTemp.add(new targetToClick(cx, cy, "na", g.getId()));
                    }
                }
            }
        }
        objectsToClick = objectsToClickTemp;
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        playerLocation world = new playerLocation(wp.getX(), wp. getY(), wp.getPlane(), "world");
        playerLocation local = new playerLocation(lp.getX(), lp. getY(), 99, "local");
        List<NPC> npcs = client.getNpcs();
        ArrayList<targetToClick> npcsToClickTemp = new ArrayList<targetToClick>();
        for (NPC npc : npcs) {
            if (RELEVANT_NPCS.contains(npc.getId())) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) { continue;}
                Rectangle r = poly.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = x + (w/2);
                double cy = y + 23 + (h /2);
                targetToClick targ = new targetToClick(cx, cy, npc.getName(), npc.getId());
                npcsToClickTemp.add(targ);
            }
        }
        npcsToClick = npcsToClickTemp;

        Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
        List<SlotToDrop> drop = new ArrayList<SlotToDrop>();
        for (int i = 0; i < items.length; ++i) {;
            if (items[i] != null && items[i].getId() > 0) {
                final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                double cx = r.getX() + (r.getWidth()/2);
                double cy = r.getY() + 23 + (r.getHeight() /2);
                SlotToDrop slot = new SlotToDrop(cx, cy, i, items[i].getId(), items[i].getQuantity());
                drop.add(slot);
            }
        }
        dataPacket dp = new dataPacket(world, local, npcsToClick, objectsToClick, drop);
        post(dp);
    }
}
