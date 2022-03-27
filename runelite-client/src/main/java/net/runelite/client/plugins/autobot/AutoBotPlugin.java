package net.runelite.client.plugins.autobot;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.Value;
import net.runelite.api.*;
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

import okhttp3.*;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
        name = "AutoBotFishing",
        description = "Does some stuff with fishing data",
        tags = {"fishing", "bot"}
)

public class AutoBotPlugin extends Plugin {
    @Inject
    private AutoBotFishingConfig config;

    private OkHttpClient okClient = new OkHttpClient();

    public AutoBotPlugin() {
    }


    @Provides
    AutoBotFishingConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoBotFishingConfig.class);
    }

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:1488/");
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
    // Wait a few game ticks as I go to a new fishing spot
    private Integer ignoreTick = 0;
    private String status = "initing";
    private static final Set<Integer> FISHING_ANIMATIONS = ImmutableSet.of(
            AnimationID.FISHING_BARBTAIL_HARPOON,
            AnimationID.FISHING_BAREHAND,
            AnimationID.FISHING_BAREHAND_CAUGHT_SHARK_1,
            AnimationID.FISHING_BAREHAND_CAUGHT_SHARK_2,
            AnimationID.FISHING_BAREHAND_CAUGHT_SWORDFISH_1,
            AnimationID.FISHING_BAREHAND_CAUGHT_SWORDFISH_2,
            AnimationID.FISHING_BAREHAND_CAUGHT_TUNA_1,
            AnimationID.FISHING_BAREHAND_CAUGHT_TUNA_2,
            AnimationID.FISHING_BAREHAND_WINDUP_1,
            AnimationID.FISHING_BAREHAND_WINDUP_2,
            AnimationID.FISHING_BIG_NET,
            AnimationID.FISHING_CAGE,
            AnimationID.FISHING_CRYSTAL_HARPOON,
            AnimationID.FISHING_DRAGON_HARPOON,
            AnimationID.FISHING_DRAGON_HARPOON_OR,
            AnimationID.FISHING_HARPOON,
            AnimationID.FISHING_INFERNAL_HARPOON,
            AnimationID.FISHING_TRAILBLAZER_HARPOON,
            AnimationID.FISHING_KARAMBWAN,
            AnimationID.FISHING_NET,
            AnimationID.FISHING_OILY_ROD,
            AnimationID.FISHING_POLE_CAST,
            AnimationID.FISHING_PEARL_ROD,
            AnimationID.FISHING_PEARL_FLY_ROD,
            AnimationID.FISHING_PEARL_BARBARIAN_ROD,
            AnimationID.FISHING_PEARL_ROD_2,
            AnimationID.FISHING_PEARL_FLY_ROD_2,
            AnimationID.FISHING_PEARL_BARBARIAN_ROD_2,
            AnimationID.FISHING_PEARL_OILY_ROD);

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
    private static class fishingPacket
    {
        double x;
        double y;
        String type;
        Integer dist;
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
        Boolean amFishing;
        List<fishingPacket> fishingSpotData;
        List<SlotToDrop> inv;
        List<randomEventPacket> randomEvent;
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Boolean amFishing = client.getLocalPlayer().getInteracting() != null
                && client.getLocalPlayer().getInteracting().getName().contains("Fishing spot")
                && client.getLocalPlayer().getInteracting().getGraphic() != GraphicID.FLYING_FISH
                && FISHING_ANIMATIONS.contains(client.getLocalPlayer().getAnimation());

        List<NPC> nips = client.getNpcs();
        List<fishingPacket> fishingSpotData = new ArrayList<fishingPacket>();
        List<randomEventPacket> randomEventData = new ArrayList<randomEventPacket>();
        for (NPC pchar : nips) {
            //this NPC is a fishing spot
            FishingSpot spot = FishingSpot.findSpot(pchar.getId());
            if (spot != null){
                Polygon poly = pchar.getCanvasTilePoly();
                Rectangle r = poly.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = x + (w/2);
                double cy = y + 23 + (h /2);
                fishingPacket fp = new fishingPacket(cx, cy, spot.getName(), pchar.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()));
                fishingSpotData.add(fp);
            }

            else if (EVENT_NPCS.contains(pchar.getId())) {
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
                double cx = x + (w/2);
                double cy = y + 23 + (h /2);
                randomEventData.add(new randomEventPacket(cx, cy));
            }
        }

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


        post(new FishingInfo(amFishing, fishingSpotData, drop, randomEventData));
    }
}
