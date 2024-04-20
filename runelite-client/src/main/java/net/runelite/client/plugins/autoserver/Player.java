package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.client.plugins.autolode.Pickaxe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import static net.runelite.client.plugins.fishing.FishingOverlay.FISHING_ANIMATIONS;

public class Player {
    @Value
    public static class SkillData {
        int level;
        int xp;
        int boostedLevel;
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
}
