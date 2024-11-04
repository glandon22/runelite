package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonObject;
import net.runelite.api.*;
import net.runelite.client.game.NpcUtil;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

public class ReqHandler {
    @Inject
    private PluginManager pluginManager;

    public GameInfoPacket processFields(Client client, JsonObject jsonObject, NpcUtil npcUtil) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        Inventory inventory = new Inventory();
        Projectiles pj = new Projectiles();
        Tiles tileUtil = new Tiles();
        Interfaces ifce = new Interfaces();
        Player p = new Player();
        NPCs npcHelper = new NPCs(npcUtil);
        Bank bankUtil = new Bank();
        ObjectUtil go = new ObjectUtil();
        Utilities u = new Utilities();

        GameInfoPacket gip = new GameInfoPacket();
        if (jsonObject.get("varBits") != null) gip.varBits = ifce.getVarbits(client, jsonObject.get("varBits").getAsJsonArray());
        if (jsonObject.get("inv") != null) gip.inv = inventory.getInventory(client);
        // this is what is in your inventory when the bank screen is open
        if (jsonObject.get("bankInv") != null ) gip.bankInv = inventory.getBankInventory(client);
        if (jsonObject.get("equipment") != null) gip.equipment = inventory.getEquipment(client);
        if (jsonObject.get("npcs") != null) gip.npcs = npcHelper.getNPCsByName(client, jsonObject.get("npcs").getAsJsonArray());
        if (jsonObject.get("bank") != null) gip.bankItems = bankUtil.getBankItems(client);
        if (jsonObject.get("skills") != null) gip.skills = p.getSkillData(client, jsonObject.get("skills").getAsJsonArray());
        if (jsonObject.get("isMining") != null) gip.isMining = p.isMining(client);
        if (jsonObject.get("orientation") != null) gip.orientation = client.getLocalPlayer().getCurrentOrientation();
        if (jsonObject.get("tiles") != null) gip.tiles = tileUtil.getTileData(client, jsonObject.get("tiles").getAsJsonArray());
        if (jsonObject.get("canvas") != null) gip.canvas = ifce.getCanvasData(client);
        if (jsonObject.get("widgets") != null) gip.widgets = ifce.getWidgets(client, jsonObject.get("widgets").getAsJsonArray());
        if (jsonObject.get("allObjects") != null) gip.allObjects = go.getAllObjects(client, jsonObject);
        if (jsonObject.get("poseAnimation") != null) gip.poseAnimation = p.getPoseAnimation(client);
        if (jsonObject.get("setYaw") != null) u.setYaw(client, jsonObject.get("setYaw").getAsInt());
        if (jsonObject.get("playerWorldPoint") != null) gip.playerWorldPoint = u.getPlayerWorldPoint(client);
        if (jsonObject.get("interactingWith") != null) gip.interactingWith = p.getInteractingWith(client);
        if (jsonObject.get("detailedInteracting") != null) gip.detailedInteracting = p.getInteractingWithDetailed(client);
        if (jsonObject.get("isFishing") != null) gip.isFishing = p.isFishing(client);
        if (jsonObject.get("chatOptions") != null) gip.chatOptions = ifce.getChatOptions(client);
        if (jsonObject.get("playerAnimation") != null) gip.playerAnimation = client.getLocalPlayer().getAnimation();
        if (jsonObject.get("players") != null) gip.players = p.otherPlayers(client);
        if (jsonObject.get("world") != null) gip.world = client.getWorld();
        if (jsonObject.get("projectilesV2") != null) gip.projectilesV2 = pj.getProjectiles(client);
        if (jsonObject.get("activePrayers") != null) gip.activePrayers = p.activePrayer(client);
        if (jsonObject.get("destinationTile") != null) gip.destinationTile = p.getDest(client);
        if (jsonObject.get("rightClickV2") != null) gip.rightClickV2 = ifce.getRightClickMenuEntriesV2(client);
        if (jsonObject.get("gameState") != null) gip.gameState = client.getGameState();
        if (jsonObject.get("varPlayer") != null) gip.varPlayer = p.varPlayer(client, jsonObject.get("varPlayer").getAsJsonArray());

        if (jsonObject.get("herbiboar") != null) {
            Plugin qhp = pluginManager.getPlugins().stream()
                    .filter(e -> e.getName().equals("Herbiboar"))
                    .findAny().orElse(null);
            if (qhp == null) gip.herbiboar = new AutoServer.herbiboarData(null, false);
            else {
                HashMap<String, Integer> nextStop = null;
                boolean hasFinished = false;

                Object qh = qhp.getClass().getMethod("getNextStop").invoke(qhp);
                if (qh != null) nextStop = (HashMap<String, Integer>) qh;
                Object qh2 = qhp.getClass().getMethod("hasFinished").invoke(qhp);
                if (qh2 != null)  hasFinished = (boolean) qh2;
                gip.herbiboar = new AutoServer.herbiboarData(nextStop, hasFinished);
            }
        }

        if (jsonObject.get("slayer") != null) {
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
        }

        if (jsonObject.get("mta") != null) {
            gip.mta = new HashMap<>();
            Plugin qhp = pluginManager.getPlugins().stream()
                    .filter(e -> e.getName().equals("Mage Training Arena"))
                    .findAny().orElse(null);
            if (qhp == null) return null;
            Object qh = qhp.getClass().getMethod("getTeleTile").invoke(qhp);
            if (qh != null) gip.mta.put("teleTile", qh);
            Object qh1 = qhp.getClass().getMethod("getAlchItem").invoke(qhp);
            if (qh1 != null)  gip.mta.put("getAlchItem", qh1);
        }

        return gip;
    }
}
