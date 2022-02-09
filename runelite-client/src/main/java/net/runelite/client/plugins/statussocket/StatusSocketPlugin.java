package net.runelite.client.plugins.statussocket;

import com.google.inject.Provides;

import java.awt.*;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import okhttp3.OkHttpClient;
import javax.inject.Inject;
import net.runelite.client.game.FishingSpot;
import net.runelite.api.NPC;
import net.runelite.api.NpcID;
@Slf4j
@PluginDescriptor(
	name = "Status Socket",
	description = "Actively logs the player status to a remote server.",
	tags = {"status", "socket"},
	enabledByDefault = false
)
public class StatusSocketPlugin extends Plugin
{

	@Inject
	@Getter(AccessLevel.PUBLIC)
	private Client client;

	@Inject
	@Getter(AccessLevel.PUBLIC)
	private EventBus eventBus;

	@Inject
	private StatusSocketConfig config;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ItemManager itemManager;

	private OkHttpClient okClient = new OkHttpClient();

	private StatusSocketClient slc;
	private int lastTickAttacked; // last tick the client player attacked

	@Provides
	StatusSocketConfig provideConfig(final ConfigManager configManager)
	{
		return configManager.getConfig(StatusSocketConfig.class);
	}

	@Override
	protected void startUp()
	{
		slc = new StatusSocketClient(client, itemManager, config, okClient);
	}


	@Override
	protected void shutDown()
	{

	}

	// the onGameTick event is used to:
	// - send inventory updates (which includes most relevant data) every tick
	// - send attack packets while the client player is attacking (not when being attacked)
	@Subscribe
	public void onGameTick(GameTick event)
	{
		slc.sendInventoryChangeLog();
	}
}
