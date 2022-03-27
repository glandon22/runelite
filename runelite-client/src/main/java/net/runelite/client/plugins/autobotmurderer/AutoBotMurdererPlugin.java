package net.runelite.client.plugins.autobotmurderer;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@PluginDescriptor(
        name = "AutoBotMurderer",
        description = "Gets relevant combat info",
        tags = {"combat", "bot"},
        enabledByDefault = false
)
public class AutoBotMurdererPlugin extends Plugin {
    @Inject
    AutoBotMurdererConfig config;

    @Provides
    AutoBotMurdererConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoBotMurdererConfig.class);
    }

    private OkHttpClient okClient = new OkHttpClient();

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
    private static class GameInfoPacket
    {
        ArrayList<InvSlot> inv;
        ArrayList<NpcPacket> npcs;
        int hpLevel;
        int unboostedHpLevel;
        String interactingWith;
        boolean canAttack;
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


        HttpUrl url = HttpUrl.parse("http://localhost:" + (config.exposedPort() != null ? config.exposedPort() : "7777") + "/");
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

        boolean canAttack = false;
        if (client.getMenuEntries() != null) {
            MenuEntry[] mn = client.getMenuEntries();
            for (MenuEntry entry : mn) {
                if (entry.getOption().contains("Attack")) {
                    canAttack = true;
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
        List<NPC> npcs = client.getNpcs();
        ArrayList<NpcPacket> alnp = new ArrayList<NpcPacket>();
        for (NPC npc : npcs) {
            // this should filter out npcs who are being attacked by other players
            if (npc.getInteracting() != null && npc.getInteracting() != client.getLocalPlayer()) {
                continue;
            }
            String n = npc.getName();
            if (n != null && n.contains(config.npcTokill())) {
                Polygon poly = npc.getCanvasTilePoly();
                if (poly == null) {continue;}
                Rectangle r = poly.getBounds();
                double x = r.getX();
                double y = r.getY();
                double w = r.getWidth();
                double h = r.getHeight();
                double cx = (int)(x + (w/2));
                double cy = (int)(y + 23 + (h /2));
                NpcPacket np = new NpcPacket(cx ,cy, npc.getName(), npc.getId(), npc.getWorldLocation().distanceTo2D(client.getLocalPlayer().getWorldLocation()));
                alnp.add(np);
            }
        }
        String interactingWith = "not interacting";
        Actor p = client.getLocalPlayer().getInteracting();
        if (p != null) {
           interactingWith = p.getName();
        }
        GameInfoPacket gip = new GameInfoPacket(
                drop,
                alnp,
                client.getBoostedSkillLevel(Skill.HITPOINTS),
                client.getRealSkillLevel(Skill.HITPOINTS),
                interactingWith,
                canAttack
        );
        post(gip);
    }
}
