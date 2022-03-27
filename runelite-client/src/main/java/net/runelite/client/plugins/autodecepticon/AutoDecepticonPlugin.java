package net.runelite.client.plugins.autodecepticon;

import com.google.gson.Gson;
import com.google.inject.Provides;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.*;

import javax.inject.Inject;
import java.awt.*;
import java.io.IOException;
import java.util.*;

@PluginDescriptor(
        name = "AutoDecepticon",
        description = "The total bot package",
        tags = {"bot"},
        enabledByDefault = false
)
public class AutoDecepticonPlugin extends Plugin {
    public Hashtable<Integer, Tile> currentWallObjects = new Hashtable<Integer, Tile>();
    public HashSet<Tile> currentGameObjects = new HashSet<>();
    @Inject
    private AutoDecepticonConfig config;

    @Inject
    Client client;

    @Provides
    AutoDecepticonConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(AutoDecepticonConfig.class);
    }

    @Override
    protected void shutDown() throws Exception
    {
        currentGameObjects.clear();
        System.out.println("size");
        System.out.println(currentGameObjects.size());
        System.out.println(currentGameObjects);
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        Player p = new Player(config, client, currentWallObjects, currentGameObjects);
        p.build();
    }

    @Subscribe
    public void onWallObjectSpawned(WallObjectSpawned event)
    {
        final WallObject wall = event.getWallObject();
        currentWallObjects.put(wall.getId(), event.getTile());
    }

    @Subscribe
    public void onWallObjectDespawned(WallObjectDespawned event)
    {
        final WallObject wall = event.getWallObject();
        currentWallObjects.remove(wall.getId());
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event)
    {
        //||
        //                        event.getGameObject().getLocalLocation().distanceTo(client.getLocalPlayer().getLocalLocation()) > 15
        if (
                event.getGameObject().getId() == 0 ||
                event.getGameObject().getId() != config.gameObject()
        ) {
            return;
        }

        final Tile t = event.getTile();
        currentGameObjects.add(t);
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned event)
    {
        if (event.getGameObject().getId() == 0 || event.getGameObject().getId() != config.gameObject()) {
            return;
        }

        currentGameObjects.remove(event.getTile());

    }

    @Subscribe
    public void onGameObjectChanged(final GameObjectChanged event) {
        currentGameObjects.remove(event.getTile());
    }
}
