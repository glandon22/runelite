package net.runelite.client.plugins.autoserver;

import com.google.gson.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import org.apache.commons.compress.utils.IOUtils;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@PluginDescriptor(
        name = "AutoServer",
        description = "Exposes a server on port 56789 to get game data",
        tags = {"bot"},
        enabledByDefault = false
)
public class AutoServer extends Plugin {
    private HttpServer server = null;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Value
    public static class osrsData {
        List<Inventory.Slot> inv;
    }

    @Inject private PluginManager pluginManager;

    @Inject
    public Client client;

    @Inject
    private ClientThread clientThread;

    private class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            try {
                handleResponse(httpExchange,httpExchange.getRequestBody());
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        private void handleResponse(HttpExchange httpExchange, InputStream reqBody) throws IOException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            GameInfoPacket gip = new GameInfoPacket();
            Player playerUtil = new Player();
            OutputStream outputStream = httpExchange.getResponseBody();
            byte[] bytes = IOUtils.toByteArray(reqBody);
            String text = new String(bytes, CHARSET);
            try {
                JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
            } catch (Exception e) {
                String resText = "Exception while trying to parse request body.";
                httpExchange.sendResponseHeaders(403, resText.length());

                outputStream.write(resText.getBytes());
                outputStream.flush();
                outputStream.close();
                return;
            }
            final JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
            if (jsonObject.get("varBit") != null) {
                try {
                    clientThread.invoke(() -> {
                        gip.varBit = client.getVarbitValue(jsonObject.get("varBit").getAsInt());
                        return true;
                    });
                    // Varbit look ups are done on the client thread, and the call is run async. Wait at least 1 tick
                    // to assure that it is populated.
                    Thread.sleep(601);
                } catch (Exception e) {
                    System.out.println("parsing error");
                    System.out.println(e);
                }
            }

            if (
                    jsonObject.get("inv") != null &&
                    jsonObject.get("inv").getAsBoolean()
            ) {
                Inventory inventory = new Inventory();
                gip.inv = inventory.getInventory(client);
            }

            if (
                    jsonObject.get("equipmentInv") != null &&
                            (Boolean) jsonObject.get("equipmentInv").getAsBoolean()
            ) {
                Inventory inventory = new Inventory();
                gip.equipmentInv = inventory.getEquipmentInventory(client);
            }

            if (jsonObject.get("npcs") != null) {
                JsonArray test = jsonObject.get("npcs").getAsJsonArray();
                HashSet<String> npcsToFind = new HashSet<>();
                for (JsonElement elem : test) {
                    try {
                        String tileHash = elem.toString().replace("\"", "");
                        npcsToFind.add(tileHash);

                    } catch (Exception e) {
                        System.out.println("Failed to find tile data for npc: ");
                        System.out.println(elem);
                    }
                }
                NPCs npcUtil = new NPCs();
                gip.npcs = npcUtil.getNPCsByName(client, npcsToFind);
            }

            if (jsonObject.get("npcsID") != null) {
                JsonArray test = jsonObject.get("npcsID").getAsJsonArray();
                HashSet<String> npcsToFind = new HashSet<>();
                for (JsonElement elem : test) {
                    try {
                        String tileHash = elem.toString().replace("\"", "");
                        npcsToFind.add(tileHash);

                    } catch (Exception e) {
                        System.out.println("Failed to find tile data for npc: ");
                        System.out.println(elem);
                    }
                }
                NPCs npcUtil = new NPCs();
                gip.npcs = npcUtil.getNPCsByID(client, npcsToFind);
            }

            if (
                    jsonObject.get("bank") != null &&
                    jsonObject.get("bank").getAsBoolean()
            ) {
                Bank bankUtil = new Bank();
                gip.bankItems = bankUtil.getBankItems(client);
            }

            if (
                    jsonObject.get("dumpInvButton") != null && jsonObject.get("dumpInvButton").getAsBoolean()
            ) {
                Bank bankUtil = new Bank();
                gip.dumpInvButton = bankUtil.getDumpInventoryLoc(client);
            }

            if (jsonObject.get("skills") != null) {
                gip.skills = playerUtil.getSkillData(client, jsonObject.get("skills").getAsJsonArray());
            }

            if (
                    jsonObject.get("isMining") != null &&
                    jsonObject.get("isMining").getAsBoolean()
            ) {
                gip.isMining = playerUtil.isMining(client);
            }

            if (jsonObject.get("tiles") != null) {
                Tiles tileUtil = new Tiles();
                gip.tiles = tileUtil.getTileData(client, jsonObject.get("tiles").getAsJsonArray());
            }

            if (
                    jsonObject.get("clickToPlay") != null &&
                    jsonObject.get("clickToPlay").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                gip.clickToPlay = ifce.getClickToPlay(client);
            }

            if (jsonObject.get("gameObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("gameObjects").getAsJsonArray();
                gip.gameObjects = go.findGameObjects(client, s);
            }

            if (jsonObject.get("groundObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("groundObjects").getAsJsonArray();
                gip.groundObjects = go.findGroundObjects(client, s);
            }

            if (jsonObject.get("wallObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("wallObjects").getAsJsonArray();
                gip.wallObjects = go.findWallObjects(client, s);
            }

            if (jsonObject.get("multipleGameObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("multipleGameObjects").getAsJsonArray();
                gip.multipleGameObjects= go.findMultipleGameObjects(client, s);
            }

            if (
                    jsonObject.get("poseAnimation") != null && jsonObject.get("poseAnimation").getAsBoolean()
            ) {
                Player pu = new Player();
                gip.poseAnimation = pu.getPoseAnimation(client);
            }

            if (jsonObject.get("widget") != null) {
                Interfaces ifce = new Interfaces();
                gip.widget = ifce.getWidget(client, jsonObject.get("widget").getAsString());
            }

            if (jsonObject.get("setYaw") != null) {
                Utilities u = new Utilities();
                u.setYaw(client, jsonObject.get("setYaw").getAsInt());
            }

            if (
                    jsonObject.get("playerWorldPoint") != null && jsonObject.get("playerWorldPoint").getAsBoolean()
            ) {
                Utilities u = new Utilities();
                gip.playerWorldPoint = u.getPlayerWorldPoint(client);
            }

            if (
                    jsonObject.get("interactingWith") != null && jsonObject.get("interactingWith").getAsBoolean()
            ) {
                Player pu = new Player();
                gip.interactingWith = pu.getInteractingWith(client);
            }

            if (
                    jsonObject.get("isFishing") != null && jsonObject.get("isFishing").getAsBoolean()
            ) {
                Player pu = new Player();
                gip.isFishing = pu.isFishing(client);
            }

            if (
                    jsonObject.get("chatOptions") != null && jsonObject.get("chatOptions").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                gip.chatOptions = ifce.getChatOptions(client);
            }

            if (
                    jsonObject.get("playerAnimation") != null && jsonObject.get("playerAnimation").getAsBoolean()
            ) {
                gip.playerAnimation = client.getLocalPlayer().getAnimation();
            }

            if (
                    jsonObject.get("getMenuEntries") != null && jsonObject.get("getMenuEntries").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                gip.menuEntries = ifce.getMenuEntries(client);
            }

            if (jsonObject.get("decorativeObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("decorativeObjects").getAsJsonArray();
                gip.decorativeObjects = go.findDecorativeObjects(client, s);
            }

            if (jsonObject.get("npcsToKill") != null) {
                JsonArray test = jsonObject.get("npcsToKill").getAsJsonArray();
                HashSet<String> npcsToFind = new HashSet<>();
                for (JsonElement elem : test) {
                    try {
                        String tileHash = elem.toString().replace("\"", "");
                        npcsToFind.add(tileHash);

                    } catch (Exception e) {
                        System.out.println("Failed to find tile data for npc: ");
                        System.out.println(elem);
                    }
                }
                NPCs npcUtil = new NPCs();
                gip.npcs = npcUtil.getNPCsByToKill(client, npcsToFind);
            }

            if (jsonObject.get("groundItems") != null) {
                JsonArray s = jsonObject.get("groundItems").getAsJsonArray();
                ObjectUtil go = new ObjectUtil();
                try {
                    gip.groundItems = go.getGroundItems(client, s);
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("getTargetObj") != null && jsonObject.get("getTargetObj").getAsBoolean()) {
                Plugin qhp = pluginManager.getPlugins().stream()
                        .filter(e -> e.getName().equals("Interact Highlight"))
                        .findAny().orElse(null);
                if (qhp == null) return;

                Object qh = qhp.getClass().getMethod("interactedObjID").invoke(qhp);
                if (qh == null) return;
                gip.targetObj = ((Number) qh).intValue();
            }


            if (jsonObject.get("getTargetNPC") != null && jsonObject.get("getTargetNPC").getAsBoolean()) {
                Plugin qhp = pluginManager.getPlugins().stream()
                        .filter(e -> e.getName().equals("Interact Highlight"))
                        .findAny().orElse(null);
                if (qhp == null) return;

                Object qh = qhp.getClass().getMethod("interactedNPCID").invoke(qhp);
                if (qh == null) return;
                gip.targetNPC = ((Number) qh).intValue();
            }
            Headers headers = httpExchange.getResponseHeaders();
            // Tell my downstream consumer we are sending JSON back
            headers.set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));

            // Convert my game data object into a JSON string for down stream consumption
            Gson gson = new Gson();
            String responseBody = gson.toJson(gip);
            byte[] rawResponseBody = responseBody.getBytes(CHARSET);
            httpExchange.sendResponseHeaders(200, rawResponseBody.length);
            outputStream.write(rawResponseBody);
            outputStream.flush();
            outputStream.close();
        }
    }

    @Override
    protected void startUp() throws Exception
    {
        server = HttpServer.create(new InetSocketAddress("localhost", 56799), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/osrs", new  MyHttpHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
    }

    @Override
    protected void shutDown() throws Exception {
        server.stop(0);
    }

    @Subscribe public void onGameTick(GameTick tick) throws Exception
    {
        /*for (int i = 0; i < 150; i++) {
            Widget bankDumpContainer = client.getWidget(WidgetID.EQUIPMENT_GROUP_ID, i);
            if (bankDumpContainer != null) {
                System.out.println("found a widget");
                System.out.println(i);
                bankDumpContainer.setHidden(true);
                Thread.sleep(2000);
                bankDumpContainer.setHidden(false);
                System.out.println("----------------------------------------------");
            }
        }
        Widget bankDumpContainer = client.getWidget(WidgetID.EQUIPMENT_GROUP_ID, 24);
        Rectangle r = bankDumpContainer.getBounds();
        double x = r.getX();
        double y = r.getY();
        double w = r.getWidth();
        double h = r.getHeight();
        int cx = (int)(x + (w/2));
        int cy = (int)(y + 23 + (h /2));
        System.out.println(cx);
        System.out.println(cy);
        Thread.sleep(5000);*/
    }
}

/*
* for (int i = 0; i < 150; i++) {
            Widget bankDumpContainer = client.getWidget(WidgetID.DEPOSIT_BOX_GROUP_ID, i);
            if (bankDumpContainer != null) {
                System.out.println("found a widget");
                System.out.println(i);
                bankDumpContainer.setHidden(true);
                Thread.sleep(2000);
                bankDumpContainer.setHidden(false);
                System.out.println("----------------------------------------------");
            }
        }
*
* */

/*if (client.isMenuOpen()) {
            MenuEntry[] menuEntries = client.getMenuEntries();
            System.out.println("menu");
            for (MenuEntry entry : menuEntries)
            {
                entry.getOption();
            }
        }*/