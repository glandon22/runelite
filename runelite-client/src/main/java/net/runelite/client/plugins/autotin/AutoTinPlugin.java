package net.runelite.client.plugins.autotin;

import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import lombok.Getter;
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
import net.runelite.api.AnimationID;
import javax.annotation.Nullable;
import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@PluginDescriptor(
        name = "AutoTin",
        description = "mines tin and drops ",
        tags = {"mining", "bot", "tin"}
)
public class AutoTinPlugin extends Plugin {
    private static final Set<Integer> RELEVANT_OBJECTS = ImmutableSet.of(
            ObjectID.ROCKS_11361,
            ObjectID.ROCKS_11364
    );

    private OkHttpClient okClient = new OkHttpClient();

    private void post(Object obj)
    {
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:9087/");
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
        int id;
    }

    @Value
    private static class MiningInfo
    {
        List<SlotToDrop> inv;
        ArrayList<rockData> rockData;
        boolean isMining;
    }

    @Getter
    @Nullable
    private Pickaxe pickaxe;

    @Subscribe
    public void onGameTick(GameTick event) {
        Player local = client.getLocalPlayer();
        int animId = local.getAnimation();

        Pickaxe pickaxe = Pickaxe.fromAnimation(animId);
        boolean isMining = pickaxe != null && (pickaxe.matchesMiningAnimation(client.getLocalPlayer()) || client.getLocalPlayer().getAnimation() == AnimationID.DENSE_ESSENCE_CHIPPING);

        Item[] items = client.getItemContainer(InventoryID.INVENTORY).getItems();
        List<SlotToDrop> drop = new ArrayList<SlotToDrop>();
        for (int i = 0; i < items.length; ++i) {
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
        //tin
        wps.add(new WorldPoint(3172, 3366, 0));
        wps.add(new WorldPoint(3173, 3365, 0));
        //iron
        wps.add(new WorldPoint(2195, 2792, 0));
        wps.add(new WorldPoint(2196, 2793, 0));
        wps.add(new WorldPoint(2197, 2792, 0));
        for (WorldPoint wp: wps) {
            final LocalPoint localLocation = LocalPoint.fromWorld(client, wp);
            if (localLocation != null) {
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
                            mps.add(new rockData(cx, cy, g.getId()));
                        }
                    }
                }
            }
        }

        post(new MiningInfo(drop, mps, isMining));
    }
}
