package net.runelite.client.plugins.autoserver;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import lombok.Value;
import net.runelite.api.*;
import net.runelite.client.plugins.autolode.Pickaxe;

import java.util.*;

public class Player {
    private static final Set<Integer> FISHING_ANIMATIONS = ImmutableSet.of(
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_BARBED,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_BLANK,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_SHARK_2,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_SHARK_1,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_SWORDFISH_1,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_SWORDFISH_2,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_TUNA_1,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_END_TUNA_2,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_START,
            net.runelite.api.gameval.AnimationID.BRUT_PLAYER_HAND_FISHING_READY,
            net.runelite.api.gameval.AnimationID.HUMAN_LARGENET,
            net.runelite.api.gameval.AnimationID.HUMAN_LOBSTER,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_CRYSTAL,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_DRAGON,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_TRAILBLAZER_NO_INFERNAL,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_INFERNAL,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_TRAILBLAZER,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_TRAILBLAZER_RELOADED,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_LEAGUE_TRAILBLAZER,
            net.runelite.api.gameval.AnimationID.HUMAN_HARPOON_TRAILBLAZER_RELOADED_NO_INFERNAL,
            net.runelite.api.gameval.AnimationID.HUMAN_OCTOPUS_POT,
            net.runelite.api.gameval.AnimationID.HUMAN_SMALLNET,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_CASTING,
            net.runelite.api.gameval.AnimationID.HUMAN_FISH_ONSPOT,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_CASTING_PEARL,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_CASTING_PEARL_FLY,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_CASTING_PEARL_BRUT,
            net.runelite.api.gameval.AnimationID.HUMAN_FISH_ONSPOT_PEARL,
            net.runelite.api.gameval.AnimationID.HUMAN_FISH_ONSPOT_PEARL_FLY,
            net.runelite.api.gameval.AnimationID.HUMAN_FISH_ONSPOT_PEARL_BRUT,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_CASTING_PEARL_OILY,
            net.runelite.api.gameval.AnimationID.HUMAN_FISHING_ONSPOT_BRUT);
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

    public HashMap<String, String> varPlayer(Client client, HashSet<String> varps) {
        HashMap<String, String> output = new HashMap<>();

        for (String varp : varps) {

            Integer value = client.getVarpValue(Integer.parseInt(varp));
            output.put(varp, String.valueOf(value));
        }

        return output;
    }
}
