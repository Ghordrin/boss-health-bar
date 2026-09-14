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

import static com.ghordrin.bosshealthbar.ColorUtil.brighten;
import java.awt.Color;

public enum HealthBarTheme
{
	ASHEN_CRIMSON("Ashen Crimson", 0x96161A, 0x96161A, 0x7A6C54, 0xD6B254),
	EMBERFALL("Emberfall", 0xC41E1E, 0xE8841C, 0x8C2814, 0xFFE2A8),
	GILDED_BLOOD("Gilded Blood", 0xA8182A, 0x6E0E1A, 0xC9A54A, 0xF2E6C8),
	FROSTBOUND("Frostbound", 0x3C8CD2, 0x2A5C9C, 0xA8B8C8, 0xEAF6FF),
	ABYSSAL("Abyssal", 0x7A2FC0, 0x4A1A80, 0x6E6A86, 0xE070D8),
	OBSIDIAN("Obsidian", 0xE4DFD4, 0xB8B0A2, 0x4A4A4E, 0xC8303A),
	VERDANT("Verdant", 0x2E9E5A, 0xB8B02A, 0x5A8A5E, 0xF4ECB0),
	VENOM("Venom", 0x8CC81E, 0x4E8A14, 0x3E4A32, 0xE8F070),
	SUNFORGED("Sunforged", 0xE0A42A, 0xC0681A, 0xB08A4E, 0xFFF4D6),
	DUSKROSE("Duskrose", 0xC23C6E, 0x7E1E48, 0x8C6A78, 0xF6C8D8),
	/**
	 * Uses the colors from the "Custom colors" config section instead of fixed colors.
	 */
	CUSTOM("Custom");

	private final String label;
	// Null for CUSTOM, whose colors come from the config.
	private final ThemeColors colors;

	/**
	 * A theme without fixed colors, whose colors the overlay reads from the config.
	 */
	HealthBarTheme(String label)
	{
		this.label = label;
		this.colors = null;
	}

	/**
	 * A built-in theme. The frame color is also used for the ornament metal, and a lighter shade of it
	 * for the combat level and "Defeated" label. The full health fill color is used for the gems.
	 */
	HealthBarTheme(String label, int fillHigh, int fillLow, int frame, int trail)
	{
		this.label = label;
		final Color frameColor = new Color(frame);
		this.colors = ThemeColors.builder()
			.fillHigh(new Color(fillHigh))
			.fillLow(new Color(fillLow))
			.trail(new Color(trail))
			.frame(frameColor)
			.ornament(frameColor)
			.gem(new Color(fillHigh))
			.text(ThemeColors.DEFAULT_TEXT)
			.levelText(brighten(frameColor, 0.35f))
			.hitpointsText(ThemeColors.DEFAULT_HITPOINTS_TEXT)
			.defeatedText(brighten(frameColor, 0.45f))
			.build();
	}

	/**
	 * The theme's colors, or null for {@link #CUSTOM}.
	 */
	ThemeColors getColors()
	{
		return colors;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
