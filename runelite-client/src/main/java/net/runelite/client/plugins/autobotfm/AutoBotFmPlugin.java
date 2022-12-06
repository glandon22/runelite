package net.runelite.client.plugins.autobotfm;

import com.google.gson.Gson;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.autobotquest.AutoBotQuestPlugin;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
        name = "AutoBotFm",
        description = "lights fires at varr west",
        tags = {"firemaking", "fm", "bot"},
        enabledByDefault = false
)
public class AutoBotFmPlugin extends Plugin {
    private OkHttpClient okClient = new OkHttpClient();

    @Value
    private static class targetToClick
    {
        double x;
        double y;
        String name;
        int Id;
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

    @Value
    private static class NpcPacket
    {
        double x;
        double y;
        String name;
        Integer id;
        Integer dist;
    }

    @Value
    private static class TilePacket
    {
        double x;
        double y;
        String name;
    }

    @Value
    private static class GameInfoPacket
    {
        ArrayList<InvSlot> inv;
        ArrayList<NpcPacket> npcs;
        ArrayList<TilePacket> tiles;
        ArrayList<InvSlot> bank;
        int fmLevel;
        int fmXp;
        WorldPoint worldPoint;
    }

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
        Gson gson = new Gson();
        String json = gson.toJson(obj);


        HttpUrl url = HttpUrl.parse("http://localhost:6890/");
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

    @Subscribe
    public void onGameTick(GameTick event) {
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
                    int cx = (int)(r.getX() + (r.getWidth()/2));
                    int cy = (int)(r.getY() + 23 + (r.getHeight() /2));
                    InvSlot slot = new InvSlot(cx, cy, i, items[i].getId(), items[i].getQuantity());
                    drop.add(slot);
                }
            }
        }
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<NpcPacket>();
        for (NPC npc : npcs) {
            String n = npc.getName();
            if (n != null && n.contains("Banker")) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                int cx = (int)(x + (w/2));
                int cy = (int)(y + 23 + (h /2));
                NpcPacket np = new NpcPacket(cx ,cy, npc.getName(), npc.getId(), npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()));
                alnp.add(np);
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
        ArrayList<TilePacket> altp = new ArrayList<>();
        /*
        WorldPoint wp = new WorldPoint(3200, 3431, 0);
        final LocalPoint lp = LocalPoint.fromWorld(client, wp);
        Polygon p = Perspective.getCanvasTilePoly(client, lp);
        Rectangle r = p.getBounds();
        double x = r.getX();
        double y = r.getY();
        double w = r.getWidth();
        double h = r.getHeight();
        int cx = (int)(x + (w/2));
        int cy = (int)(y + 23 + (h /2));
        altp.add(new TilePacket(cx, cy, "s1"));

        WorldPoint wp1 = new WorldPoint(3200, 3432, 0);
        final LocalPoint lp1 = LocalPoint.fromWorld(client, wp1);
        Polygon p1 = Perspective.getCanvasTilePoly(client, lp1);
        Rectangle r1 = p1.getBounds();
        double x1 = r1.getX();
        double y1 = r1.getY();
        double w1 = r1.getWidth();
        double h1 = r1.getHeight();
        int cx1 = (int)(x1 + (w1/2));
        int cy1 = (int)(y1 + 23 + (h1 /2));
        altp.add(new TilePacket(cx1, cy1, "s2"));*/

        WorldPoint wp2 = new WorldPoint(3200, 3432, 0);
        final LocalPoint lp1 = LocalPoint.fromWorld(client, wp2);
        Polygon p1 = Perspective.getCanvasTilePoly(client, lp1);
        Rectangle r1 = p1.getBounds();
        double x1 = r1.getX();
        double y1 = r1.getY();
        double w1 = r1.getWidth();
        double h1 = r1.getHeight();
        int cx1 = (int)(x1 + (w1/2));
        int cy1 = (int)(y1 + 23 + (h1 /2));
        altp.add(new TilePacket(cx1, cy1, "fountain"));

        int fmxp = client.getSkillExperience(Skill.FIREMAKING);
        WorldPoint playerWorldPoint = client.getLocalPlayer().getWorldLocation();
        //(wp.getX(), wp. getY(), wp.getPlane(), "world")
        GameInfoPacket gip = new GameInfoPacket(drop, alnp, altp, bankStuff, client.getRealSkillLevel(Skill.FIREMAKING), fmxp, playerWorldPoint);
        post(gip);
    }
}
