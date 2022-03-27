package net.runelite.client.plugins.autodecepticon;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.AllArgsConstructor;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.autobotcooker.AutoBotCookerPlugin;
import net.runelite.client.plugins.autobotquest.AutoBotQuestPlugin;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

@AllArgsConstructor
public class Player {
    AutoDecepticonConfig config;
    Client client;
    Hashtable<Integer, Tile> currentWallObjects;
    HashSet<Tile> currentGameObjects;

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

    private void post(Object obj)
    {
        OkHttpClient okClient = new OkHttpClient();
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:6464/");
        MediaType mt = MediaType.parse("application/json; charset=utf-8");
        RequestBody body = RequestBody.create(mt, json);

        assert url != null;
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

    private void getInv(DecepticonPacket dp) {
        ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
        ArrayList<InventorySlot> drop = new ArrayList<InventorySlot>();
        if (ic == null) {
            drop = null;
        } else {
            Item[] items = ic.getItems();
            for (int i = 0; i < items.length; ++i) {
                if (items[i] != null && items[i].getId() > 0) {
                    final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                    final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                    double cx = (int)(r.getX() + (r.getWidth() / 2));
                    double cy = (int)(r.getY() + 23 + (r.getHeight() / 2));
                    //InventorySlot slot = new InventorySlot(cx, cy, i, , );
                    InventorySlot slot = new InventorySlot();
                    slot.x = cx;
                    slot.y = cy;
                    slot.index = i;
                    slot.id = items[i].getId();
                    slot.quantity = items[i].getQuantity();
                    dp.inv.add(slot);
                }
            }
        }


    }

    private void getPlayerLoc(DecepticonPacket dp) {
        WorldPoint wp = client.getLocalPlayer().getWorldLocation();
        dp.playerLocation.type = "world";
        dp.playerLocation.x = wp.getX();
        dp.playerLocation.y = wp.getY();
        dp.playerLocation.z = wp.getPlane();
    }

    private void getWallObjects(DecepticonPacket dp) {
        Set<Integer> ss = currentWallObjects.keySet();
        for (Integer key : ss) {
            Tile tile = currentWallObjects.get(key);
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
            WallObjectData wod = new WallObjectData();
            wod.x = (int)cx;
            wod.y = (int)cy;
            dp.wallObjects.put(wo.getId(), wod);
        }
    }

    private void getGameObjects(DecepticonPacket dp) {
        for (Tile t : currentGameObjects) {
            GameObject[] gos = t.getGameObjects();
            for (GameObject go : gos) {
                if (go == null) {continue;}
                Shape s = go.getConvexHull();
                if (s == null) {continue;}
                Rectangle r = s.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = x + (w/2);
                double cy = y + 23 + (h /2);
                GameObjectData god = new GameObjectData();
                //object is not on screen
                if (cx < 0 || cx > 1920 || cy < 0 || cy > 1080) {
                    continue;
                }
                god.x = (int)cx;
                god.y = (int)cy;
                god.dist = go.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation());
                if (dp.gameObjects.get(go.getId()) != null) {
                    ArrayList<GameObjectData> temp = dp.gameObjects.get(go.getId());
                    temp.add(god);
                    dp.gameObjects.put(go.getId(), temp);
                }

                else {
                    ArrayList<GameObjectData> temp = new ArrayList<>();
                    temp.add(god);
                    dp.gameObjects.put(go.getId(), temp);
                }
            }
        }
    }

    public DecepticonPacket build() {

        DecepticonPacket dp = new DecepticonPacket();
        if (config.showPlayer() && client.getLocalPlayer().getWorldLocation() != null) {
            dp.playerLocation = new PlayerLocation();
            getPlayerLoc(dp);
        }

        if (config.showInv()) {
            dp.inv = new ArrayList<>();
            getInv(dp);
        }

        if (config.showWallObjects())  {
            dp.wallObjects = new Hashtable<Integer, WallObjectData>();
            getWallObjects(dp);
        }

        if (config.showGameObjects()) {
            dp.gameObjects = new Hashtable<Integer, ArrayList<GameObjectData>>();
            getGameObjects(dp);
        }

        dp.animation = client.getLocalPlayer().getAnimation();
        post(dp);
        return dp;
    }
}
