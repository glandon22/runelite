package net.runelite.client.plugins.autoserver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.AnimationID;
import net.runelite.api.Client;
import net.runelite.api.GraphicID;
import net.runelite.api.Skill;
import net.runelite.client.plugins.autolode.Pickaxe;

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
            } catch (Exception e) {
                System.out.println("Failed to find data for skill: ");
                System.out.println(elem);
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
        System.out.println("herjk");
        if (client.getLocalPlayer() != null) {
            System.out.println("123");
            if (client.getLocalPlayer().getInteracting() != null) {
                System.out.println("567");
                System.out.println(client.getLocalPlayer().getInteracting().getName());
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
}
