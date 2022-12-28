package net.runelite.client.plugins.autotodt;

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
import net.runelite.client.plugins.autobotminer.AutoBotMinerPlugin;
import net.runelite.client.plugins.autobotmurderer.AutoBotMurdererPlugin;
import net.runelite.client.plugins.autobotsmithy.AutoBotSmithyPlugin;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

@PluginDescriptor(
        name = "AutoTodt",
        description = "fucks the wintertodt to death",
        tags = {"firemaking", "fm", "bot", "wintertodt", "todt"},
        enabledByDefault = false
)
public class AutoTodtPlugin extends Plugin {
    private static final Set<Integer> RELEVANT_OBJECTS = ImmutableSet.of(
            ObjectID.DOORS_OF_DINH,
            ObjectID.BRAZIER_29312,
            ObjectID.BRUMA_ROOTS,
            ObjectID.BANK_CHEST_29321,
            ObjectID.BURNING_BRAZIER_29314,
            ObjectID.BRAZIER_29313,
            26690,
            29324
    );

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

    @Value
    private static class objData
    {
        double x;
        double y;
    }

    @Value
    private static class TilePacket
    {
        double x;
        double y;
        HashMap<String, objData> gameObjects;
    }

    @Value
    private static class InvSlot
    {
        double x;
        double y;
        int index;
        int id;
        int quantity;
    }

    private OkHttpClient okClient = new OkHttpClient();

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:4200/");
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
    Client client;

    @Value
    private static class GameInfoPacket
    {
        HashMap<String, TilePacket> tiles;
        String wtHealth;
        int wtTimer;
        int animation;
        int fmXp;
        int hp;
        ArrayList<InvSlot> inv;
        ArrayList<InvSlot> bankStuff;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        // i need to also look up what game objects are on my current tile to see if im about to get hit with snow
        // need my wintertodt score amount
        Widget wintertodtEnergyWidget = client.getWidget(25952277);
        int fmxp = client.getSkillExperience(Skill.FIREMAKING);
        int hp = client.getBoostedSkillLevel(Skill.HITPOINTS);
        int animation = client.getLocalPlayer().getAnimation();
        int timer = client.getVarbitValue(Varbits.WINTERTODT_TIMER) * 30 / 50;
        WorldPoint me = client.getLocalPlayer().getWorldLocation();
        String t = null;
        if (wintertodtEnergyWidget != null) {
            t = wintertodtEnergyWidget.getText();
        }

        int[][] tilesToFind = new int[8][];
        tilesToFind[0] = new int[] {1630, 3965, 0}; // dinhs door
        tilesToFind[1] = new int[] {1621, 3998, 0}; // brazier
        tilesToFind[2] = new int[] {1621, 3988, 0}; // bruma root
        tilesToFind[3] = new int[] {1641, 3944, 0}; //bank chest
        tilesToFind[4] = new int[] {me.getX(), me.getY(), me.getPlane()};
        tilesToFind[5] = new int[] {1632, 3950, 0}; // intermediate tile by bank
        tilesToFind[6] = new int[] {1627, 3991, 0}; // intermediate tile in wintertodt
        tilesToFind[7] = new int[] {1622, 3988, 0}; // safe cuttin tile

        String[] tileNames = {"dinhs", "brazier", "bruma", "bank", "me", "i1", "i2", "safe"};

        HashMap<String, TilePacket> altp = new HashMap<>();
        Tile[][][] tiles = client.getScene().getTiles();
        for (int i = 0; i < tilesToFind.length; i++) {
            HashMap<String, objData> gameObjects = new HashMap<>();
            int[] myTile = tilesToFind[i];
            WorldPoint wp = new WorldPoint(myTile[0], myTile[1], myTile[2]);
            final LocalPoint lp1 = LocalPoint.fromWorld(client, wp);
            if (lp1 != null) {
                Polygon p1 = Perspective.getCanvasTilePoly(client, lp1);
                if (p1 != null) {
                    Rectangle r1 = p1.getBounds();
                    if (r1 != null) {
                        double x1 = r1.getX();
                        double y1 = r1.getY();
                        double w1 = r1.getWidth();
                        double h1 = r1.getHeight();
                        int cx1 = (int) (x1 + (w1 / 2));
                        int cy1 = (int) (y1 + 23 + (h1 / 2));
                        //only add things that are on screen
                        if (cx1 > 0 && cx1 < 1920 && cy1 > 0 && cy1 < 1080) {
                            Tile tile = null;
                            try {
                                tile = tiles[client.getPlane()][lp1.getSceneX()][lp1.getSceneY()];
                            } catch(Exception e) {}
                            if (tile != null) {
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
                                        if (cx > 0 && cx < 1920 && cy > 0 && cy < 1080) {
                                            gameObjects.put(String.valueOf(g.getId()), new objData(cx, cy));
                                        }
                                    }
                                }
                            }
                            altp.put(tileNames[i], new TilePacket(cx1, cy1, gameObjects));
                        }
                    }
                }
            }
        }

        ItemContainer ic = client.getItemContainer(InventoryID.INVENTORY);
        ArrayList<InvSlot> drop = new ArrayList<InvSlot>();
        if (ic == null) {
            drop = null;
        }

        else {
            Item[] items = ic.getItems();
            for (int i = 0; i < items.length; ++i) {
                if (items[i] != null && items[i].getId() > 0) {
                    final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                    final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                    double cx = (int)(r.getX() + (r.getWidth()/2));
                    double cy = (int)(r.getY() + 23 + (r.getHeight() /2));
                    InvSlot slot = new InvSlot(cx, cy, i, items[i].getId(), items[i].getQuantity());
                    drop.add(slot);
                }
            }
        }

        // Compute bank prices using only the shown items so that we can show bank value during searches
        final Widget bankItemContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
        ItemContainer bankContainer = null;
        Widget[] children = null;
        if (bankItemContainer != null) {
            bankContainer = client.getItemContainer(InventoryID.BANK);
            children = bankItemContainer.getChildren();
        }

        ArrayList<InvSlot> bankStuff = new ArrayList<>();
        if (bankContainer != null && children != null) {

            // The first components are the bank items, followed by tabs etc. There are always 816 components regardless
            // of bank size, but we only need to check up to the bank size.
            for (int i = 0; i < bankContainer.size(); ++i) {
                Widget child = children[i];
                if (child != null && !child.isSelfHidden() && child.getItemId() > -1) {
                    Rectangle r = child.getBounds();
                    double x = r.getX();
                    double y = r.getY();
                    double w = r.getWidth();
                    double h = r.getHeight();
                    int cx = (int)(x + (w/2));
                    int cy = (int)(y + 23 + (h /2));
                    InvSlot is = new InvSlot(cx, cy, 0, child.getItemId(), child.getItemQuantity());
                    bankStuff.add(is);
                }
            }
        }

        GameInfoPacket gip = new GameInfoPacket(altp, t, timer, animation, fmxp, hp, drop,  bankStuff);
        post(gip);
    }
}
