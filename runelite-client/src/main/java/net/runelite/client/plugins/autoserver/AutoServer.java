package net.runelite.client.plugins.autoserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.text.StringEscapeUtils;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.*;

import javax.inject.Inject;

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

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    private class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            handleResponse(httpExchange,httpExchange.getRequestBody());

        }

        private void handleResponse(HttpExchange httpExchange, InputStream reqBody)  throws  IOException {
            GameInfoPacket gip = new GameInfoPacket();
            Player playerUtil = new Player();
            OutputStream outputStream = httpExchange.getResponseBody();
            String text = new String(reqBody.readAllBytes(), CHARSET);
            System.out.println("req body");
            System.out.println(text);
            Object obj = null;
            try {
                obj = new JSONParser().parse(text);
            } catch (Exception e) {
                String resText = "Exception while trying to parse request body.";
                System.out.println(resText);
                httpExchange.sendResponseHeaders(403, resText.length());

                outputStream.write(resText.getBytes());
                outputStream.flush();
                outputStream.close();
                return;
            }

            JSONObject parsedRequestBody = (JSONObject) obj;
            if (parsedRequestBody.get("varBit") != null) {
                try {
                    clientThread.invoke(() -> {
                        gip.varBit = client.getVarbitValue(Integer.parseInt((String) parsedRequestBody.get("varBit")));
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
                    parsedRequestBody.get("inv") != null &&
                    (Boolean) parsedRequestBody.get("inv")
            ) {
                Inventory inventory = new Inventory();
                gip.inv = inventory.getInventory(client);
            }

            if (parsedRequestBody.get("npcs") != null) {
                JSONArray test = (JSONArray) parsedRequestBody.get("npcs");
                Object[] parse = test.toArray();
                HashSet<String> npcsToFind = new HashSet<>();
                for (Object o : parse) {
                    String npcName = (String) o;
                    npcsToFind.add(npcName);
                }
                NPCs npcUtil = new NPCs();
                gip.npcs = npcUtil.getNPCsByName(client, npcsToFind);
            }

            if (parsedRequestBody.get("npcsID") != null) {
                JSONArray test = (JSONArray) parsedRequestBody.get("npcsID");
                Object[] parse = test.toArray();
                HashSet<String> npcsToFind = new HashSet<>();
                for (Object o : parse) {
                    String npcName = (String) o;
                    npcsToFind.add(npcName);
                }
                NPCs npcUtil = new NPCs();
                gip.npcs = npcUtil.getNPCsByID(client, npcsToFind);
            }

            if (
                    parsedRequestBody.get("bank") != null &&
                    (Boolean) parsedRequestBody.get("bank")
            ) {
                Bank bankUtil = new Bank();
                gip.bankItems = bankUtil.getBankItems(client);
            }

            if (
                    parsedRequestBody.get("dumpInvButton") != null &&
                            (Boolean) parsedRequestBody.get("dumpInvButton")
            ) {
                Bank bankUtil = new Bank();
                gip.dumpInvButton = bankUtil.getDumpInventoryLoc(client);
            }

            if (parsedRequestBody.get("skills") != null) {
                gip.skills = playerUtil.getSkillData(client, parsedRequestBody.get("skills"));
            }

            if (
                    parsedRequestBody.get("isMining") != null &&
                    (Boolean) parsedRequestBody.get("isMining")
            ) {
                gip.isMining = playerUtil.isMining(client);
            }

            if (parsedRequestBody.get("tiles") != null) {
                Tiles tileUtil = new Tiles();
                gip.tiles = tileUtil.getTileData(client, parsedRequestBody.get("tiles"));
            }

            if (
                    parsedRequestBody.get("clickToPlay") != null &&
                    (Boolean) parsedRequestBody.get("clickToPlay")
            ) {
                Interfaces ifce = new Interfaces();
                gip.clickToPlay = ifce.getClickToPlay(client);
            }

            if (parsedRequestBody.get("gameObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                gip.gameObjects = go.findGameObjects(client, parsedRequestBody.get("gameObjects"));
            }

            if (parsedRequestBody.get("groundObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                gip.groundObjects = go.findGroundObjects(client, parsedRequestBody.get("groundObjects"));
            }

            if (parsedRequestBody.get("wallObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                gip.wallObjects = go.findWallObjects(client, parsedRequestBody.get("wallObjects"));
            }

            if (
                    parsedRequestBody.get("poseAnimation") != null &&
                            (Boolean) parsedRequestBody.get("poseAnimation")
            ) {
                Player pu = new Player();
                gip.poseAnimation = pu.getPoseAnimation(client);
            }

            if (parsedRequestBody.get("widget") != null) {
                Interfaces ifce = new Interfaces();
                gip.widget = ifce.getWidget(client, parsedRequestBody.get("widget"));
            }

            if (parsedRequestBody.get("setYaw") != null) {
                Utilities u = new Utilities();
                u.setYaw(client, parsedRequestBody.get("setYaw"));
            }

            if (
                    parsedRequestBody.get("playerWorldPoint") != null &&
                            (Boolean) parsedRequestBody.get("playerWorldPoint")
            ) {
                Utilities u = new Utilities();
                gip.playerWorldPoint = u.getPlayerWorldPoint(client);
            }

            if (
                    parsedRequestBody.get("interactingWith") != null &&
                            (Boolean) parsedRequestBody.get("interactingWith")
            ) {
                Player pu = new Player();
                gip.interactingWith = pu.getInteractingWith(client);
            }

            if (
                    parsedRequestBody.get("isFishing") != null &&
                    (Boolean) parsedRequestBody.get("isFishing")
            ) {
                Player pu = new Player();
                gip.isFishing = pu.isFishing(client);
            }

            if (
                    parsedRequestBody.get("chatOptions") != null && (Boolean) parsedRequestBody.get("chatOptions")
            ) {
                Interfaces ifce = new Interfaces();
                gip.chatOptions = ifce.getChatOptions(client);
            }

            if (parsedRequestBody.get("decorativeObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                gip.decorativeObjects = go.findDecorativeObjects(client, parsedRequestBody.get("decorativeObjects"));
            }

            Headers headers = httpExchange.getResponseHeaders();
            // Tell my downstream consumer we are sending JSON back
            headers.set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));

            // Convert my game data object into a JSON string for down stream consumption
            Gson gson = new Gson();
            String responseBody = gson.toJson(gip);
            System.out.println("resb");
            System.out.println(responseBody);
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
        System.out.println("herjk");
        if (client.getLocalPlayer() != null) {
            System.out.println("123");
            if (client.getLocalPlayer().getInteracting() != null) {
                System.out.println("567");
                System.out.println(client.getLocalPlayer().getInteracting().getName());
            }
        }
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