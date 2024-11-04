package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.autolode.Pickaxe;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

import static net.runelite.client.plugins.fishing.FishingOverlay.FISHING_ANIMATIONS;

public class Player {
    Interfaces i = new Interfaces();
    @Value
    public static class SkillData {
        int level;
        int xp;
        int boostedLevel;
    }

    @Value
    public static class DetailedInteractingPacket {
        String name;
        int health;
    }

    @Value
    public static class OtherPlayerData {
        String name;
        WorldPoint worldPoint;
        int x;
        int y;
        int orientation;
    }

    public HashMap<String, SkillData> getSkillData(Client client, JsonArray skills) {
        HashMap<String, SkillData> skillData = new HashMap<>();
        for (JsonElement elem : skills) {
            try {
                String skillName = elem.toString().replace("\"", "");
                skillName = skillName.toUpperCase(Locale.ROOT);
                SkillData skd = new SkillData(
                        client.getRealSkillLevel(Skill.valueOf(skillName)),
                        client.getSkillExperience(Skill.valueOf(skillName)),
                        client.getBoostedSkillLevel(Skill.valueOf(skillName))
                );
                skillData.put(skillName.toLowerCase(Locale.ROOT), skd);
            } catch (Exception ignored) {
            }
        }
        return skillData;
    }

    public boolean isMining(Client client) {
        net.runelite.api.Player local = client.getLocalPlayer();
        int animId = local.getAnimation();

        Pickaxe pickaxe = Pickaxe.fromAnimation(animId);
        return pickaxe != null && (pickaxe.matchesMiningAnimation(client.getLocalPlayer()) || client.getLocalPlayer().getAnimation() == AnimationID.DENSE_ESSENCE_CHIPPING);
    }

    public int getPoseAnimation(Client client) {
        return client.getLocalPlayer().getPoseAnimation();
    }

    public String getInteractingWith(Client client) {
        if (client.getLocalPlayer() != null) {
            if (client.getLocalPlayer().getInteracting() != null) {
                return client.getLocalPlayer().getInteracting().getName();
            }
        }
        return null;
    }

    public DetailedInteractingPacket getInteractingWithDetailed(Client client) {
        if (client.getLocalPlayer() != null) {
            if (client.getLocalPlayer().getInteracting() != null) {
                Actor targ = client.getLocalPlayer().getInteracting();
                return new DetailedInteractingPacket(
                  targ.getName(),
                  targ.getHealthRatio()
                );
            }
        }
        return null;
    }


    public boolean isFishing(Client client) {
        return client.getLocalPlayer().getInteracting() != null
                && client.getLocalPlayer().getInteracting().getName().contains("Fishing spot")
                && client.getLocalPlayer().getInteracting().getGraphic() != GraphicID.FLYING_FISH
                && FISHING_ANIMATIONS.contains(client.getLocalPlayer().getAnimation());
    }

    public ArrayList<Integer> activePrayer(Client client) {
        ArrayList<Integer> activePrayers = new ArrayList<>();
        for (Prayer prayer : Prayer.values())
        {
            if (client.isPrayerActive(prayer))
            {
                activePrayers.add(prayer.getVarbit());
            }
        }
        return activePrayers;
    }

    public HashMap<String, String> varPlayer(Client client, JsonArray varps) {
        HashSet<String> varpsToFind = new HashSet<>();
        for (JsonElement elem : varps) {
            String varp = elem.toString().replace("\"", "");
            varpsToFind.add(varp);
        }
        HashMap<String, String> output = new HashMap<>();

        for (String varp : varpsToFind) {

            Integer value = client.getVarpValue(Integer.parseInt(varp));
            output.put(varp, String.valueOf(value));
        }

        return output;
    }

    public List<OtherPlayerData> otherPlayers(Client client) {
        Interfaces.CanvasData canvasData = i.getCanvasData(client);
        List<OtherPlayerData> OtherPlayerData = new ArrayList<>();
        WorldView wv = client.getTopLevelWorldView();
        List<net.runelite.api.Player> players = wv == null ? Collections.emptyList() : wv.players()
                .stream()
                .collect(Collectors.toCollection(ArrayList::new));
        for (net.runelite.api.Player player : players) {
            Shape poly = player.getConvexHull();
            if (poly == null) {continue;}
            Rectangle r = poly.getBounds();
            Utilities u = new Utilities();
            HashMap<Character, Integer> center = u.getCenter(r, canvasData.getXOffset(), canvasData.getYOffset());
            OtherPlayerData.add(new OtherPlayerData(
                player.getName(),
                player.getWorldLocation(),
                center.get('x'),
                center.get('y'),
                player.getOrientation()
            ));
        }

        return OtherPlayerData;
    }

    public Utilities.PointData getDest(Client client) {
        LocalPoint lp = client.getLocalDestinationLocation();
        if (lp == null) {
            return null;
        }

        else {
            WorldPoint wp = WorldPoint.fromLocal(client, lp);
            Utilities.PointData pd = new Utilities.PointData();
            pd.x = wp.getX();
            pd.y = wp.getY();
            pd.z = wp.getPlane();
            return pd;
        }
    }
}
