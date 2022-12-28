package net.runelite.client.plugins.autoserver;

import com.google.gson.Gson;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.Skill;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.apache.commons.text.StringEscapeUtils;

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

    private class MyHttpHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            String requestParamValue = null;
            handleResponse(httpExchange,httpExchange.getRequestBody());

        }

        private void handleResponse(HttpExchange httpExchange, InputStream reqBody)  throws  IOException {
            //System.out.println(client.getRealSkillLevel(Skill.valueOf("Fletching")));
            GameInfoPacket gip = new GameInfoPacket();
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
            /*
            // Test shit, this is how to parse an array of arrays
            JSONArray test = (JSONArray) parsedRequestBody.get("tiles");
            Object[] parse = test.toArray();
            for (Object o : parse) {
                JSONArray jsonTile = (JSONArray) o;
                Object[] tile = jsonTile.toArray();
                System.out.println(tile[0]);
            }*/

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
        server = HttpServer.create(new InetSocketAddress("localhost", 56799), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/osrs", new  MyHttpHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
    }

    @Override
    protected void shutDown() throws Exception
    {
        server.stop(0);
    }
}
