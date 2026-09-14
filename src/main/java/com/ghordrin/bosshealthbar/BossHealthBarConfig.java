/*
 * Copyright (c) 2026, Ghordrin
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
package com.ghordrin.bosshealthbar;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;
import net.runelite.client.config.Units;

@ConfigGroup("bosshealthbar")
public interface BossHealthBarConfig extends Config
{
	@ConfigSection(
		name = "Appearance",
		description = "How the bar looks and animates.",
		position = 0
	)
	String appearanceSection = "appearance";

	@ConfigSection(
		name = "Text",
		description = "The name, numbers and labels around the bar.",
		position = 1
	)
	String textSection = "text";

	@ConfigSection(
		name = "Behaviour",
		description = "Which opponents get a bar, and how it works alongside other health bars.",
		position = 2
	)
	String behaviourSection = "behaviour";

	@ConfigItem(
		keyName = "theme",
		name = "Theme",
		description = "The colors of the health bar.",
		position = 0,
		section = appearanceSection
	)
	default HealthBarTheme theme()
	{
		return HealthBarTheme.ASHEN_CRIMSON;
	}

	@ConfigItem(
		keyName = "ornamentStyle",
		name = "Ornaments",
		description = "Decorations on both ends of the bar, colored to match the theme. Godsword draws a hilt on the left and a blade tip on the right.",
		position = 1,
		section = appearanceSection
	)
	default OrnamentStyle ornamentStyle()
	{
		return OrnamentStyle.GODSWORD;
	}

	@Range(min = 200, max = 1400)
	@ConfigItem(
		keyName = "barWidth",
		name = "Bar width",
		description = "The width of the health bar in pixels.",
		position = 2,
		section = appearanceSection
	)
	default int barWidth()
	{
		return 600;
	}

	@Range(min = 4, max = 24)
	@ConfigItem(
		keyName = "barHeight",
		name = "Bar height",
		description = "The height of the health bar itself in pixels, not counting the text around it.",
		position = 3,
		section = appearanceSection
	)
	default int barHeight()
	{
		return 7;
	}

	@ConfigItem(
		keyName = "showDamageTrail",
		name = "Show damage trail",
		description = "After a hit, keep the lost health visible as a lighter section for a moment before it drains away.",
		position = 4,
		section = appearanceSection
	)
	default boolean showDamageTrail()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showPhaseMarkers",
		name = "Show phase markers",
		description = "When this bar replaces the game's own boss health bar, show the same phase markers the game's bar shows.",
		position = 5,
		section = appearanceSection
	)
	default boolean showPhaseMarkers()
	{
		return true;
	}

	@ConfigItem(
		keyName = "flashOnBigHits",
		name = "Flash on big hits",
		description = "Briefly flash the bar's border when a hit removes a large part of the opponent's health.",
		position = 6,
		section = appearanceSection
	)
	default boolean flashOnBigHits()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDefeatAnimation",
		name = "Defeat animation",
		description = "When the opponent dies, hold the empty bar with a \"Defeated\" label for a moment before fading it out.",
		position = 7,
		section = appearanceSection
	)
	default boolean showDefeatAnimation()
	{
		return true;
	}

	@ConfigItem(
		keyName = "lowHealthEffect",
		name = "Low health effect",
		description = "Make the fill pulse and glow while the opponent's health is at or below the low health threshold.",
		position = 8,
		section = appearanceSection
	)
	default boolean lowHealthEffect()
	{
		return true;
	}

	@Range(min = 5, max = 50)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "lowHealthThreshold",
		name = "Low health threshold",
		description = "The health percentage at or below which the low health effect starts.",
		position = 9,
		section = appearanceSection
	)
	default int lowHealthThreshold()
	{
		return 25;
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "animationSpeed",
		name = "Heal animation speed",
		description = "How quickly the bar refills when the opponent heals. Higher is faster. Damage always lowers the bar immediately.",
		position = 10,
		section = appearanceSection
	)
	default int animationSpeed()
	{
		return 6;
	}

	@ConfigItem(
		keyName = "fontStyle",
		name = "Font",
		description = "The font used for all text around the bar. RuneScape uses the game's interface font.",
		position = 0,
		section = textSection
	)
	default FontStyle fontStyle()
	{
		return FontStyle.SERIF;
	}

	@Range(min = 75, max = 150)
	@Units(Units.PERCENT)
	@ConfigItem(
		keyName = "textSize",
		name = "Text size",
		description = "The size of all text around the bar, relative to its default size.",
		position = 1,
		section = textSection
	)
	default int textSize()
	{
		return 100;
	}

	@ConfigItem(
		keyName = "showBossName",
		name = "Show name",
		description = "Show the opponent's name above the health bar.",
		position = 2,
		section = textSection
	)
	default boolean showBossName()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showCombatLevel",
		name = "Show combat level",
		description = "Show the opponent's combat level next to its name.",
		position = 3,
		section = textSection
	)
	default boolean showCombatLevel()
	{
		return false;
	}

	@ConfigItem(
		keyName = "showDamageNumber",
		name = "Show damage number",
		description = "Show the total damage of your recent hits above the right end of the bar. It resets a few seconds after your last hit.",
		position = 4,
		section = textSection
	)
	default boolean showDamageNumber()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hitpointsTextMode",
		name = "Hitpoints text",
		description = "Show the opponent's hitpoints below the bar as a percentage, a value (when the max hitpoints are known), or both. Bosses the game only shows as a percentage always show a percentage.",
		position = 5,
		section = textSection
	)
	default HitpointsTextMode hitpointsTextMode()
	{
		return HitpointsTextMode.NONE;
	}

	@ConfigItem(
		keyName = "bossOnly",
		name = "Only show for bosses",
		description = "Only show the bar for opponents at or above the minimum combat level, or shown by the game's own boss health bar. Turn off to show it for any opponent.",
		position = 0,
		section = behaviourSection
	)
	default boolean bossOnly()
	{
		return true;
	}

	@Range(min = 1, max = 1000)
	@ConfigItem(
		keyName = "minimumCombatLevel",
		name = "Minimum combat level",
		description = "When \"Only show for bosses\" is on, the lowest combat level an opponent needs for the bar to show.",
		position = 1,
		section = behaviourSection
	)
	default int minimumCombatLevel()
	{
		return 150;
	}

	@Range(min = 1, max = 60)
	@Units(Units.SECONDS)
	@ConfigItem(
		keyName = "hideDelay",
		name = "Hide after",
		description = "How long the bar stays after you stop attacking. While the game's own boss health bar shows the opponent, the bar stays regardless.",
		position = 2,
		section = behaviourSection
	)
	default int hideDelay()
	{
		return 5;
	}

	@ConfigItem(
		keyName = "replaceNativeBossBar",
		name = "Replace game's boss health bar",
		description = "Some bosses show the game's own health bar at the top of the screen. When on, that bar is hidden while you fight the boss and this bar is shown instead. When off, this bar is hidden for those bosses.",
		position = 3,
		section = behaviourSection
	)
	default boolean replaceNativeBossBar()
	{
		return true;
	}

	@ConfigItem(
		keyName = "hideVanillaOverlay",
		name = "Hide vanilla opponent overlay",
		description = "Turn off the health bar of RuneLite's \"Opponent Information\" plugin while this plugin is on, so two health bars aren't shown at once. Takes effect when this plugin starts.",
		position = 4,
		section = behaviourSection
	)
	default boolean hideVanillaOverlay()
	{
		return true;
	}
}
