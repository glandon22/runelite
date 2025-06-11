package net.runelite.client.plugins.autoserver;

import com.google.gson.*;
import com.google.inject.Provides;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLiteProperties;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.ui.overlay.OverlayManager;
import org.apache.commons.compress.utils.IOUtils;

import javax.inject.Inject;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

@PluginDescriptor(
        name = "AutoServer",
        description = "Exposes a server on port 56789 to get game data",
        tags = {"bot", "goonlite"},
        enabledByDefault = false
)
public class AutoServer extends Plugin {
    @Getter(AccessLevel.PACKAGE)
    private String status = "test";

    @Getter(AccessLevel.PACKAGE)
    private String break_start = "pl";

    @Getter(AccessLevel.PACKAGE)
    private String break_end = "fsdf";

    @Getter(AccessLevel.PACKAGE)
    private HashMap<String, String> scriptStats;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    AutoServerConfig autoServerConfig;

    @Inject
    private ScriptOverlay overlay;
    private HttpServer server = null;
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    @Value
    public static class osrsData {
        List<Inventory.Slot> inv;
    }

    @Value
    public static class herbiboarData
    {
        HashMap<String, Integer> nextStop;
        boolean finished;
    }

    @Inject private PluginManager pluginManager;

    @Inject
    public Client client;

    @Inject
    private ClientThread clientThread;

    @Provides
    AutoServerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(AutoServerConfig.class);
    }

    private <T> T invokeAndWait(Callable<T> r)
    {
        try
        {
            AtomicReference<T> ref = new AtomicReference<>();
            Semaphore semaphore = new Semaphore(0);
            clientThread.invokeLater(() -> {
                try
                {

                    ref.set(r.call());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                finally
                {
                    semaphore.release();
                }
            });
            semaphore.acquire();
            return ref.get();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

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
            System.out.println("handling request!");
            final JsonObject jsonObject = new JsonParser().parse(text).getAsJsonObject();
            if (jsonObject.get("varBit") != null) {
                try {
                    invokeAndWait(() -> {
                        gip.varBit = client.getVarbitValue(jsonObject.get("varBit").getAsInt());
                        return null;
                    });
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
                invokeAndWait(() -> {
                    gip.inv = inventory.getInventory(client);
                    return null;
                });
            }

            // this is what is in your inventory when the bank screen is open
            if (
                    jsonObject.get("bankInv") != null &&
                            jsonObject.get("bankInv").getAsBoolean()
            ) {
                Inventory inventory = new Inventory();
                invokeAndWait(() -> {
                    gip.bankInv = inventory.getBankInventory(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("equipment") != null &&
                            (Boolean) jsonObject.get("equipment").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    gip.equipment = client.getLocalPlayer().getPlayerComposition().getEquipmentIds();
                    return null;
                });
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
                invokeAndWait(() -> {
                    gip.npcs = npcUtil.getNPCsByName(client, npcsToFind);
                    return null;
                });
            }

            if (jsonObject.get("varPlayer") != null) {
                JsonArray test = jsonObject.get("varPlayer").getAsJsonArray();
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
                Player p = new Player();
                invokeAndWait(() -> {
                    gip.varPlayer = p.varPlayer(client, npcsToFind);
                    return null;
                });
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
                invokeAndWait(() -> {
                    gip.npcs = npcUtil.getNPCsByID(client, npcsToFind);
                    return null;
                });
            }

            if (
                    jsonObject.get("bank") != null &&
                    jsonObject.get("bank").getAsBoolean()
            ) {
                Bank bankUtil = new Bank();
                invokeAndWait(() -> {
                    gip.bankItems = bankUtil.getBankItems(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("depositBox") != null &&
                            jsonObject.get("depositBox").getAsBoolean()
            ) {
                Bank bankUtil = new Bank();
                invokeAndWait(() -> {
                    gip.depositBox = bankUtil.getDepositItems(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("dumpInvButton") != null && jsonObject.get("dumpInvButton").getAsBoolean()
            ) {
                Bank bankUtil = new Bank();
                invokeAndWait(() -> {
                    gip.dumpInvButton = bankUtil.getDumpInventoryLoc(client);
                    return null;
                });
            }

            if (jsonObject.get("skills") != null) {
                gip.skills = playerUtil.getSkillData(client, jsonObject.get("skills").getAsJsonArray());
            }

            if (
                    jsonObject.get("isMining") != null &&
                    jsonObject.get("isMining").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    gip.isMining = playerUtil.isMining(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("orientation") != null &&
                            jsonObject.get("orientation").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    gip.orientation = client.getLocalPlayer().getCurrentOrientation();
                    return null;
                });
            }

            if (jsonObject.get("tiles") != null) {
                Tiles tileUtil = new Tiles();
                invokeAndWait(() -> {
                    gip.tiles = tileUtil.getTileData(client, jsonObject.get("tiles").getAsJsonArray());
                    return null;
                });
            }

            if (jsonObject.get("canvas") != null) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.canvas = ifce.getCanvasData(client);
                    return null;
                });
            }

            if (jsonObject.get("widgets") != null) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.widgets = ifce.getWidgets(client, jsonObject.get("widgets").getAsJsonArray());
                    return null;
                });
            }

            if (jsonObject.get("widgetsV2") != null) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    if (gip.widgets != null) {
                        gip.widgets.putAll(ifce.getWidgetsV2(client, jsonObject.get("widgetsV2").getAsJsonArray()));
                    }

                    else {
                        gip.widgets = ifce.getWidgetsV2(client, jsonObject.get("widgetsV2").getAsJsonArray());
                    }
                    return null;
                });
            }

            if (jsonObject.get("slayer") != null) {
                invokeAndWait(() -> {
                    HashMap<String, String> data = new HashMap<>();
                    int amount = client.getVarpValue(VarPlayer.SLAYER_TASK_SIZE);
                    data.put("amount", String.valueOf(amount));
                    data.put("monster", "");
                    data.put("area", "");
                    if (amount > 0)
                    {
                        int taskId = client.getVarpValue(VarPlayer.SLAYER_TASK_CREATURE);
                        String taskName;
                        if (taskId == 98 /* Bosses, from [proc,helper_slayer_current_assignment] */)
                        {
                            int structId = client.getEnum(EnumID.SLAYER_TASK)
                                    .getIntValue(client.getVarbitValue(Varbits.SLAYER_TASK_BOSS));
                            taskName = client.getStructComposition(structId)
                                    .getStringValue(ParamID.SLAYER_TASK_NAME);
                            data.put("monster", taskName);
                        }
                        else
                        {
                            taskName = client.getEnum(EnumID.SLAYER_TASK_CREATURE)
                                    .getStringValue(taskId);
                            data.put("monster", taskName);
                        }

                        int areaId = client.getVarpValue(VarPlayer.SLAYER_TASK_LOCATION);
                        String taskLocation = null;
                        if (areaId > 0)
                        {
                            taskLocation = client.getEnum(EnumID.SLAYER_TASK_LOCATION)
                                    .getStringValue(areaId);
                            data.put("area", taskLocation);
                        }
                        gip.slayer = data;
                    }
                    return null;
                });
            }

            if (
                    jsonObject.get("clickToPlay") != null &&
                    jsonObject.get("clickToPlay").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.clickToPlay = ifce.getClickToPlay(client);
                    return null;
                });
            }

            if (jsonObject.get("gameObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("gameObjects").getAsJsonArray();
                invokeAndWait(() -> {
                    gip.gameObjects = go.findGameObjects(client, s);
                    return null;
                });
            }

            if (jsonObject.get("allObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonObject s = jsonObject.get("allObjects").getAsJsonObject();
                invokeAndWait(() -> {
                    gip.allObjects = go.getAllObjects(client, s);
                    return null;
                });
            }

            if (jsonObject.get("scriptStats") != null) {
                JsonObject s = jsonObject.get("scriptStats").getAsJsonObject();
                invokeAndWait(() -> {
                    Gson gson = new Gson();
                    // Parse my input as SearchV2 class
                    HashMap<String, String> search = gson.fromJson(s, HashMap.class);
                    scriptStats = search;
                    return null;
                });
            }

            if (jsonObject.get("scriptStats1") != null) {
                JsonObject s = jsonObject.get("scriptStats1").getAsJsonObject();
                invokeAndWait(() -> {
                    try {
                        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, 3300, 555, 1, false, 1);
                        //event.setSource("Nigger");
                        client.getCanvas().dispatchEvent(event);
                        System.out.println("completed");
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
            }

            if (jsonObject.get("gameObjectsV2") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonObject s = jsonObject.get("gameObjectsV2").getAsJsonObject();
                invokeAndWait(() -> {
                    gip.gameObjectsV2 = go.gameObjects(client, s, ObjectUtil.SearchObjectType.Game);
                    return null;
                });
            }

            if (jsonObject.get("groundObjectsV2") != null) {
                System.out.println("in v2 ground");
                ObjectUtil go = new ObjectUtil();
                JsonObject s = jsonObject.get("groundObjectsV2").getAsJsonObject();
                invokeAndWait(() -> {
                    gip.groundObjectsV2 = go.gameObjects(client, s, ObjectUtil.SearchObjectType.GroundObject);
                    return null;
                });
            }

            if (jsonObject.get("spotAnims") != null) {
                invokeAndWait(() -> {
                    ArrayList<Integer> output = new ArrayList<>();
                    IterableHashTable<ActorSpotAnim> asa = client.getLocalPlayer().getSpotAnims();
                    for (ActorSpotAnim a : asa) {
                        output.add(a.getId());
                    }
                    gip.spotAnims = output;
                    return null;
                });
            }

            if (jsonObject.get("wallObjectsV2") != null) {
                System.out.println("in v2 wall");
                ObjectUtil go = new ObjectUtil();
                JsonObject s = jsonObject.get("wallObjectsV2").getAsJsonObject();
                invokeAndWait(() -> {
                    gip.wallObjectsV2 = go.gameObjects(client, s, ObjectUtil.SearchObjectType.Wall);
                    return null;
                });
            }

            if (jsonObject.get("decorativeObjectsV2") != null) {
                System.out.println("in v2 decorative");
                ObjectUtil go = new ObjectUtil();
                JsonObject s = jsonObject.get("decorativeObjectsV2").getAsJsonObject();
                invokeAndWait(() -> {
                    gip.decorativeObjectsV2 = go.gameObjects(client, s, ObjectUtil.SearchObjectType.Decorative);
                    return null;
                });
            }

            if (jsonObject.get("groundObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("groundObjects").getAsJsonArray();
                invokeAndWait(() -> {
                    gip.groundObjects = go.findGroundObjects(client, s);
                    return null;
                });
            }

            if (jsonObject.get("wallObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("wallObjects").getAsJsonArray();
                invokeAndWait(() -> {
                    gip.wallObjects = go.findWallObjects(client, s);
                    return null;
                });
            }

            if (jsonObject.get("multipleGameObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("multipleGameObjects").getAsJsonArray();
                invokeAndWait(() -> {
                    gip.multipleGameObjects = go.findMultipleGameObjects(client, s);
                    return null;
                });
            }

            if (
                    jsonObject.get("poseAnimation") != null && jsonObject.get("poseAnimation").getAsBoolean()
            ) {
                Player pu = new Player();
                invokeAndWait(() -> {
                    gip.poseAnimation = pu.getPoseAnimation(client);
                    return null;
                });
            }

            if (jsonObject.get("widget") != null) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.widget = ifce.getWidget(client, jsonObject.get("widget").getAsString());
                    return null;
                });
            }

            if (jsonObject.get("chatLines") != null) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.chatLines = ifce.getChatLines(client);
                    return null;
                });
            }

            if (jsonObject.get("setYaw") != null) {
                Utilities u = new Utilities();
                invokeAndWait(() -> {
                    u.setYaw(client, jsonObject.get("setYaw").getAsInt());
                    return null;
                });
            }

            if (
                    jsonObject.get("playerWorldPoint") != null && jsonObject.get("playerWorldPoint").getAsBoolean()
            ) {
                Utilities u = new Utilities();
                invokeAndWait(() -> {
                    gip.playerWorldPoint = u.getPlayerWorldPoint(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("interactingWith") != null && jsonObject.get("interactingWith").getAsBoolean()
            ) {
                Player pu = new Player();
                invokeAndWait(() -> {
                    gip.interactingWith = pu.getInteractingWith(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("isFishing") != null && jsonObject.get("isFishing").getAsBoolean()
            ) {
                Player pu = new Player();
                invokeAndWait(() -> {
                    gip.isFishing = pu.isFishing(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("chatOptions") != null && jsonObject.get("chatOptions").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.chatOptions = ifce.getChatOptions(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("playerAnimation") != null && jsonObject.get("playerAnimation").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    gip.playerAnimation = client.getLocalPlayer().getAnimation();
                    return null;
                });
            }

            if (
                    jsonObject.get("getMenuEntries") != null && jsonObject.get("getMenuEntries").getAsBoolean()
            ) {
                Interfaces ifce = new Interfaces();
                invokeAndWait(() -> {
                    gip.menuEntries = ifce.getMenuEntries(client);
                    return null;
                });
            }

            if (
                    jsonObject.get("players") != null && jsonObject.get("players").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    List<net.runelite.api.Player> p = client.getPlayers();
                    ArrayList<String> ps = new ArrayList<>();
                    for (net.runelite.api.Player pl : p) {
                        ps.add(pl.getName());
                    }
                    gip.players = ps;
                    return null;
                });
            }

            if (
                    jsonObject.get("world") != null && jsonObject.get("world").getAsBoolean()
            ) {
                invokeAndWait(() -> {
                    gip.world = client.getWorld();
                    return null;
                });
            }

            if (jsonObject.get("decorativeObjects") != null) {
                ObjectUtil go = new ObjectUtil();
                JsonArray s = jsonObject.get("decorativeObjects").getAsJsonArray();
                invokeAndWait(() -> {
                    gip.decorativeObjects = go.findDecorativeObjects(client, s);
                    return null;
                });
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
                invokeAndWait(() -> {
                    gip.npcs = npcUtil.getNPCsByToKill(client, npcsToFind);
                    return null;
                });
            }

            if (jsonObject.get("groundItems") != null) {
                JsonArray s = jsonObject.get("groundItems").getAsJsonArray();
                ObjectUtil go = new ObjectUtil();
                try {
                    invokeAndWait(() -> {
                        gip.groundItems = go.getGroundItems(client, s);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("groundItemsV2") != null) {
                JsonArray s = jsonObject.get("groundItemsV2").getAsJsonArray();
                ObjectUtil go = new ObjectUtil();
                try {

                    invokeAndWait(() -> {
                        gip.groundItemsV2 = go.getGroundItemsV2(client, s);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("herbiboar") != null && jsonObject.get("herbiboar").getAsBoolean()) {
                Plugin qhp = pluginManager.getPlugins().stream()
                        .filter(e -> e.getName().equals("Herbiboar"))
                        .findAny().orElse(null);
                if (qhp == null) {
                    gip.herbiboar = new herbiboarData(null, false);
                }

                else {
                    HashMap<String, Integer> nextStop = null;
                    boolean hasFinished = false;

                    Object qh = qhp.getClass().getMethod("getNextStop").invoke(qhp);
                    if (qh != null) {
                        nextStop = (HashMap<String, Integer>) qh;
                    }

                    Object qh2 = qhp.getClass().getMethod("hasFinished").invoke(qhp);
                    if (qh2 != null) {
                        hasFinished = (boolean) qh2;
                    }

                    gip.herbiboar = new herbiboarData(nextStop, hasFinished);
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

            if (jsonObject.get("projectiles") != null && jsonObject.get("projectiles").getAsBoolean()) {
                HashSet<Integer> projs = new HashSet<>();
                invokeAndWait(() -> {
                    for (Projectile p : client.getProjectiles()) {
                        projs.add(p.getId());
                    }

                    gip.projectiles = projs;
                    return null;
                });
            }

            if (jsonObject.get("projectilesV2") != null && jsonObject.get("projectilesV2").getAsBoolean()) {
                Projectiles p = new Projectiles();
                invokeAndWait(() -> {
                    gip.projectilesV2 = p.getProjectiles(client);
                    return null;
                });
            }

            if (jsonObject.get("activePrayers") != null && jsonObject.get("activePrayers").getAsBoolean()) {
                Player p = new Player();
                invokeAndWait(() -> {
                    gip.activePrayers = p.activePrayer(client);
                    return null;
                });
            }

            if (jsonObject.get("destinationTile") != null && jsonObject.get("destinationTile").getAsBoolean()) {
                invokeAndWait(() -> {
                    LocalPoint lp = client.getLocalDestinationLocation();
                    if (lp == null) {
                        gip.destinationTile = null;
                    }

                    else {
                        WorldPoint wp = WorldPoint.fromLocal(client, lp);
                        Utilities.PointData pd = new Utilities.PointData();
                        pd.x = wp.getX();
                        pd.y = wp.getY();
                        pd.z = wp.getPlane();
                        gip.destinationTile = pd;
                    }

                    return null;
                });
            }

            if (jsonObject.get("gameCycle") != null && jsonObject.get("gameCycle").getAsBoolean()) {
                Player p = new Player();
                invokeAndWait(() -> {
                    gip.gameCycle = client.getGameCycle();
                    return null;
                });
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

            if (jsonObject.get("rightClick") != null && jsonObject.get("rightClick").getAsBoolean()) {
                Interfaces i = new Interfaces();
                try {
                    invokeAndWait(() -> {
                        gip.rightClickMenu = i.getRightClickMenuEntries(client);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("rightClickV2") != null && jsonObject.get("rightClickV2").getAsBoolean()) {
                Interfaces i = new Interfaces();
                try {
                    invokeAndWait(() -> {
                        gip.rightClickV2 = i.getRightClickMenuEntriesV2(client);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("allGroundItems") != null) {
                JsonArray s = jsonObject.get("allGroundItems").getAsJsonArray();
                ObjectUtil go = new ObjectUtil();
                try {

                    invokeAndWait(() -> {
                        gip.allGroundItems = go.getGroundItemsAnyId(client, s);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            if (jsonObject.get("gameState") != null) {
                try {

                    invokeAndWait(() -> {
                        gip.gameState = client.getGameState();
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
            }

            /**
             * blocking areas -
             * minimap - 161,95
             * inv interace - 161,97
             * chat buttons on bottom 162,1
             */

            if (jsonObject.get("login") != null) {
                try {

                    invokeAndWait(() -> {
                        client.setGameState(GameState.LOGGING_IN);
                        return null;
                    });
                } catch (Exception e) {
                    System.out.println("eeee");
                    System.out.println(e);
                }
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
        int port = Integer.parseInt(RuneLiteProperties.getPort());
        System.out.println("starting server on port: " + port);
        server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/osrs", new  MyHttpHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() throws Exception {
        server.stop(0);
        overlayManager.remove(overlay);
    }
}