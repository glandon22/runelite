/*
 * Copyright (c) 2022, Jamal <http://github.com/1Defence>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.worldcycleplugin;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.*;
import javax.inject.Inject;


import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.input.KeyManager;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.ClientUI;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.*;
import net.runelite.http.api.worlds.World;
import net.runelite.http.api.worlds.WorldResult;
import net.runelite.http.api.worlds.WorldType;

@PluginDescriptor(
	name = "World Cycle",
	description = "Lighter, custom version of world hopper used to hop between a set of worlds circularly",
	tags = {"switcher","cycle","world hopper"}
)
@Slf4j
public class WorldCyclePlugin extends Plugin
{
	private static final int MAX_PLAYER_COUNT = 1950;

	private static final int DISPLAY_SWITCHER_MAX_ATTEMPTS = 3;


	@Inject
	private Client client;

	@Inject
	private ClientUI clientUI;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private KeyManager keyManager;

	@Inject
	private ChatMessageManager chatMessageManager;

	@Inject
	private WorldCycleConfig config;

	@Inject
	private WorldService worldService;

	@Inject
	private WSClient wsClient;

	@Inject
	private PartyService partyService;

	private NavigationButton navButton_cycle;
	public WorldCyclePanel panel_cycle;

	private net.runelite.api.World quickHopTargetWorld;
	private int displaySwitcherAttempts = 0;

	private boolean lastFocusStatus = false;

	private final CustomHotkeyListener previousKeyListener = new CustomHotkeyListener(() -> config.previousKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> hop(true));
		}
	};
	private final CustomHotkeyListener nextKeyListener = new CustomHotkeyListener(() -> config.nextKey())
	{
		@Override
		public void hotkeyPressed()
		{
			clientThread.invoke(() -> hop(false));
		}
	};

	@Provides
	WorldCycleConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(WorldCycleConfig.class);
	}

	@Override
	protected void startUp() throws Exception
	{

		wsClient.registerMessage(WorldCycleUpdate.class);
		keyManager.registerKeyListener(previousKeyListener);
		keyManager.registerKeyListener(nextKeyListener);

		panel_cycle = new WorldCyclePanel(client, configManager,this);

		final BufferedImage icon_cycle = ImageUtil.getResourceStreamFromClass(getClass(), "icon_cycle.png");
		navButton_cycle = NavigationButton.builder()
				.tooltip("World Cycle")
				.icon(icon_cycle)
				.priority(1)
				.panel(panel_cycle)
				.build();

		clientToolbar.addNavigation(navButton_cycle);

	}

	@Override
	protected void shutDown() throws Exception
	{
		wsClient.unregisterMessage(WorldCycleUpdate.class);
		keyManager.unregisterKeyListener(previousKeyListener);
		keyManager.unregisterKeyListener(nextKeyListener);

		clientToolbar.removeNavigation(navButton_cycle);
	}

	private void hop(boolean previous)
	{
		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null || client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		World currentWorld = worldResult.findWorld(client.getWorld());

		if (currentWorld == null)
		{
			return;
		}

		EnumSet<WorldType> currentWorldTypes = currentWorld.getTypes().clone();
		// Make it so you always hop out of PVP and high risk worlds
		currentWorldTypes.remove(WorldType.PVP);
		currentWorldTypes.remove(WorldType.HIGH_RISK);

		// Don't regard these worlds as a type that must be hopped between
		currentWorldTypes.remove(WorldType.BOUNTY);
		currentWorldTypes.remove(WorldType.SKILL_TOTAL);
		currentWorldTypes.remove(WorldType.LAST_MAN_STANDING);

		List<World> customWorldCycle = getCustomWorldCycle();
		boolean cyclePresent = !customWorldCycle.isEmpty();
		List<World> worlds = cyclePresent ? customWorldCycle : worldResult.getWorlds();
		int attemptedWorlds = 0;

		//don't limit the subscription type if the user is purposely attempting to hop between members and f2p
		if(cyclePresent){
			currentWorldTypes.remove(WorldType.MEMBERS);
		}

		int worldIdx = worlds.indexOf(currentWorld);
		int totalLevel = client.getTotalLevel();

		World world;
		do
		{
			/*
				Get the previous or next world in the list,
				starting over at the other end of the list
				if there are no more elements in the
				current direction of iteration.
			 */
			if (previous)
			{
				worldIdx--;

				if (worldIdx < 0)
				{
					worldIdx = worlds.size() - 1;
				}
			}
			else
			{
				worldIdx++;

				if (worldIdx >= worlds.size())
				{
					worldIdx = 0;
				}
			}

			world = worlds.get(worldIdx);

			EnumSet<WorldType> types = world.getTypes().clone();

			types.remove(WorldType.BOUNTY);
			// Treat LMS world like casual world
			types.remove(WorldType.LAST_MAN_STANDING);

			//don't limit the subscription type if the user is purposely attempting to hop between members and f2p
			if(cyclePresent){
				types.remove(WorldType.MEMBERS);
			}

			if (types.contains(WorldType.SKILL_TOTAL))
			{
				try
				{
					int totalRequirement = Integer.parseInt(world.getActivity().substring(0, world.getActivity().indexOf(" ")));

					if (totalLevel >= totalRequirement)
					{
						types.remove(WorldType.SKILL_TOTAL);
					}
				}
				catch (NumberFormatException ex)
				{
					log.warn("Failed to parse total level requirement for target world", ex);
				}
			}

			// Avoid switching to near-max population worlds, as it will refuse to allow the hop if the world is full
			if (world.getPlayers() >= MAX_PLAYER_COUNT)
			{
				//We've searched every world and not found a suitable world, break from loop
				if(++attemptedWorlds == worlds.size()){
					world = currentWorld;
					break;
				}
				continue;
			}

			// Break out if we've found a good world to hop to
			if (currentWorldTypes.equals(types))
			{
				break;
			}else{
				//We've searched every world and not found a suitable world, break from loop
				if(++attemptedWorlds == worlds.size()){
					world = currentWorld;
					break;
				}
			}
		}
		while (world != currentWorld);

		if (world == currentWorld)
		{
			String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Couldn't find a world to quick-hop to.")
				.build();

			chatMessageManager.queue(QueuedMessage.builder()
				.type(ChatMessageType.CONSOLE)
				.runeLiteFormattedMessage(chatMessage)
				.build());
		}
		else
		{
			hop(world.getId());
		}
	}

	private void hop(int worldId)
	{
		assert client.isClientThread();

		WorldResult worldResult = worldService.getWorlds();
		// Don't try to hop if the world doesn't exist
		World world = worldResult.findWorld(worldId);
		if (world == null)
		{
			return;
		}

		final net.runelite.api.World rsWorld = client.createWorld();
		rsWorld.setActivity(world.getActivity());
		rsWorld.setAddress(world.getAddress());
		rsWorld.setId(world.getId());
		rsWorld.setPlayerCount(world.getPlayers());
		rsWorld.setLocation(world.getLocation());
		rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

		if (client.getGameState() == GameState.LOGIN_SCREEN)
		{
			// on the login screen we can just change the world by ourselves
			client.changeWorld(rsWorld);
			return;
		}

		if (config.showWorldHopMessage())
		{
			String chatMessage = new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append("Quick-hopping to World ")
				.append(ChatColorType.HIGHLIGHT)
				.append(Integer.toString(world.getId()))
				.append(ChatColorType.NORMAL)
				.append("..")
				.build();

			chatMessageManager
				.queue(QueuedMessage.builder()
					.type(ChatMessageType.CONSOLE)
					.runeLiteFormattedMessage(chatMessage)
					.build());
		}

		quickHopTargetWorld = rsWorld;
		displaySwitcherAttempts = 0;
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		//Fix chat being locked & hotkeys from being unusable if user loses focus
		if(lastFocusStatus != clientUI.isFocused()){
			if(!clientUI.isFocused()){
				if(nextKeyListener.isPressed()){
					nextKeyListener.ReleaseHotkey();
				}
				if(previousKeyListener.isPressed()){
					previousKeyListener.ReleaseHotkey();
				}
			}
		}
		lastFocusStatus = clientUI.isFocused();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (quickHopTargetWorld == null)
		{
			return;
		}

		if (client.getWidget(WidgetInfo.WORLD_SWITCHER_LIST) == null)
		{
			client.openWorldHopper();

			if (++displaySwitcherAttempts >= DISPLAY_SWITCHER_MAX_ATTEMPTS)
			{
				String chatMessage = new ChatMessageBuilder()
					.append(ChatColorType.NORMAL)
					.append("Failed to quick-hop after ")
					.append(ChatColorType.HIGHLIGHT)
					.append(Integer.toString(displaySwitcherAttempts))
					.append(ChatColorType.NORMAL)
					.append(" attempts.")
					.build();

				chatMessageManager
					.queue(QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(chatMessage)
						.build());

				resetQuickHopper();
			}
		}
		else
		{
			client.hopToWorld(quickHopTargetWorld);
			resetQuickHopper();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}

		if (event.getMessage().equals("Please finish what you're doing before using the World Switcher."))
		{
			resetQuickHopper();
		}
	}

	private void resetQuickHopper()
	{
		displaySwitcherAttempts = 0;
		quickHopTargetWorld = null;
	}

	public void ChangeWorldSet(String worldset,boolean fromServer){

		//request is local, send to server
		if(!fromServer){
			if(LocalMemberIsValid()){
				partyService.send(new WorldCycleUpdate(worldset));
			}
		}

		//always handle from self, only handle from server if accept cycle is on
		if(!fromServer || config.acceptPartyCycle()){
			panel_cycle.pendingRequest = true;
			panel_cycle.uiInput.setWorldSetInput(worldset);
		}

	}
	public boolean LocalMemberIsValid(){
		PartyMember localMember = partyService.getLocalMember();
		return (localMember != null);
	}

	@Subscribe
	public void onWorldCycleUpdate(WorldCycleUpdate message)
	{
		//don't allow self updates, handle locally
		if(partyService.getLocalMember().getMemberId() == message.getMemberId()){
			return;
		}

		if(LocalMemberIsValid()){
			ChangeWorldSet(message.getWorldSet(),true);
		}
	}
	//Validate the world exists and isn't pvp before returning as a valid cycle world
	World ValidateWorld(int worldNum){

		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null) {
			return null;
		}

		World world = worldResult.findWorld(worldNum);
		if(world == null){
			return null;
		}

		//ensure there are zero instances of pvp worlds in the world cycle
		EnumSet<WorldType> currentWorldTypes = world.getTypes().clone();
		if(currentWorldTypes.contains(WorldType.PVP) || currentWorldTypes.contains(WorldType.HIGH_RISK)){
			return null;
		}

		return world;
	}

	List<World> getCustomWorldCycle(){

		List<World> worldCycleList = new ArrayList<>();

		String worldList = GetWorldSet();
		if (worldList.isEmpty())
		{
			return worldCycleList;
		}

		WorldResult worldResult = worldService.getWorlds();
		if (worldResult == null)
		{
			return worldCycleList;
		}

		Text.fromCSV(worldList).stream()
				.mapToInt(s ->
				{
					try
					{
						return Integer.parseInt(s);
					}
					catch (NumberFormatException e)
					{
						return 0;
					}
				})
				.filter(world -> ValidateWorld(world) != null)
				.forEach(world -> worldCycleList.add(worldResult.findWorld(world)));
		return worldCycleList;
	}

	public String GetWorldSet(){
		return panel_cycle.uiInput.getWorldSetInput();
	}
}
