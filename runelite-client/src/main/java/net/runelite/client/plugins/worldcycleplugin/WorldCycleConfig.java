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

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

@ConfigGroup(WorldCycleConfig.GROUP)
public interface WorldCycleConfig extends Config
{
	String GROUP = "worldcycle";
	String CONFIG_WORLDSET = "worldset";

	@ConfigItem(
		keyName = "previousKey",
		name = "Quick-hop previous",
		description = "When you press this key you'll hop to the previous world",
		position = 0
	)
	default Keybind previousKey()
	{
		return new Keybind(KeyEvent.VK_LEFT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "nextKey",
		name = "Quick-hop next",
		description = "When you press this key you'll hop to the next world",
		position = 1
	)
	default Keybind nextKey()
	{
		return new Keybind(KeyEvent.VK_RIGHT, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
	}

	@ConfigItem(
		keyName = "showMessage",
		name = "Show world hop message in chat",
		description = "Shows what world is being hopped to in the chat",
		position = 2
	)
	default boolean showWorldHopMessage()
	{
		return true;
	}

	@ConfigItem(
			keyName = "acceptPartyCycle",
			name = "Accept Party Cycle",
			description = "Accept changes to the world cycle made by party members, PVP/HIGH RISK ARE OMITTED",
			position = 3
	)
	default boolean acceptPartyCycle()
	{
		return false;
	}

}
