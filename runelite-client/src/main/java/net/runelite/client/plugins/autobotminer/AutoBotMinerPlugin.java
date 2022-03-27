package net.runelite.client.plugins.autobotminer;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.FishingSpot;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.api.InventoryID;
import net.runelite.client.config.ConfigManager;

import net.runelite.client.plugins.autobot.AutoBotPlugin;
import net.runelite.client.plugins.autobotmurderer.AutoBotMurdererConfig;
import net.runelite.client.plugins.autobotsmithy.AutoBotSmithyPlugin;
import okhttp3.*;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
        name = "AutoBotMiner",
        description = "mines and drops shit",
        tags = {"mining", "bot"}
)
public class AutoBotMinerPlugin extends Plugin {
    @Inject
    AutoBotMinerConfig config;

    @Provides
    AutoBotMinerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoBotMinerConfig.class);
    }

    private static final Set<Integer> EVENT_NPCS = ImmutableSet.of(
            NpcID.BEE_KEEPER_6747,
            NpcID.CAPT_ARNAV,
            NpcID.DR_JEKYLL, NpcID.DR_JEKYLL_314,
            NpcID.DRUNKEN_DWARF,
            NpcID.DUNCE_6749,
            NpcID.EVIL_BOB, NpcID.EVIL_BOB_6754,
            NpcID.FLIPPA_6744,
            NpcID.FREAKY_FORESTER_6748,
            NpcID.FROG_5429,
            NpcID.GENIE, NpcID.GENIE_327,
            NpcID.GILES, NpcID.GILES_5441,
            NpcID.LEO_6746,
            NpcID.MILES, NpcID.MILES_5440,
            NpcID.MYSTERIOUS_OLD_MAN_6750, NpcID.MYSTERIOUS_OLD_MAN_6751,
            NpcID.MYSTERIOUS_OLD_MAN_6752, NpcID.MYSTERIOUS_OLD_MAN_6753,
            NpcID.NILES, NpcID.NILES_5439,
            NpcID.PILLORY_GUARD,
            NpcID.POSTIE_PETE_6738,
            NpcID.QUIZ_MASTER_6755,
            NpcID.RICK_TURPENTINE, NpcID.RICK_TURPENTINE_376,
            NpcID.SANDWICH_LADY,
            NpcID.SERGEANT_DAMIEN_6743,
            NpcID.STRANGE_PLANT
    );

    private static final Set<Integer> RELEVANT_OBJECTS = ImmutableSet.of(
            ObjectID.ROCKS_11364
    );

    private OkHttpClient okClient = new OkHttpClient();

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:" + (config.exposedPort() != null ? config.exposedPort() : "6578") + "/");
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
    private static class miningPacket
    {
        double x;
        double y;
        String type;
        Integer dist;
    }

    @Value
    private static class rockData
    {
        double x;
        double y;
        Integer dist;
        int num;
    }

    @Value
    private static class randomEventPacket
    {
        double x;
        double y;
    }

    @Value
    private static class FishingInfo
    {
        List<SlotToDrop> inv;
        List<randomEventPacket> randomEvent;
        ArrayList<rockData> rockData;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        List<NPC> nips = client.getNpcs();
        List<miningPacket> fishingSpotData = new ArrayList<miningPacket>();
        List<randomEventPacket> randomEventData = new ArrayList<randomEventPacket>();
        for (NPC pchar : nips) {
            //this NPC is a fishing spot
             if (EVENT_NPCS.contains(pchar.getId())) {
                // Check that the npc is interacting with the player
                if (pchar.getInteracting() != client.getLocalPlayer()) {
                    // This isn't my random
                    continue;
                }
                Polygon poly = pchar.getCanvasTilePoly();
                Rectangle r = poly.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = (int)(x + (w / 2));
                double cy = (int)(y + 23 + (h / 2));
                randomEventData.add(new randomEventPacket(cx, cy));
            }
        }

        Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
        List<SlotToDrop> drop = new ArrayList<SlotToDrop>();
        for (int i = 0; i < items.length; ++i) {
            ;
            if (items[i] != null && items[i].getId() > 0) {
                final WidgetItem targetWidgetItem = getWidgetItem(client.getWidget(WidgetInfo.INVENTORY), i);
                final Rectangle r = targetWidgetItem.getCanvasBounds(false);
                double cx = r.getX() + (r.getWidth() / 2);
                double cy = r.getY() + 23 + (r.getHeight() / 2);
                SlotToDrop slot = new SlotToDrop(cx, cy, i, items[i].getId(), items[i].getQuantity());
                drop.add(slot);
            }
        }

        ArrayList<WorldPoint> wps = new ArrayList<>();
        ArrayList<rockData> mps = new ArrayList<>();
        Tile[][][] tiles = client.getScene().getTiles();
        wps.add(new WorldPoint(2195, 2792, 0));
        wps.add(new WorldPoint(2196, 2793, 0));
        wps.add(new WorldPoint(2197, 2792, 0));
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            Tile tile = tiles[client.getPlane()][localLocation.getSceneX()][localLocation.getSceneY()];
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
                    if (cx > 0 && cx < 1900 && cy > 0 && cy < 1000) {
                        mps.add(new rockData(cx, cy, g.getId(), wp.getX() - 2195));
                    }
                }
            }
        }

        post(new FishingInfo(drop, randomEventData, mps));
    }
}
