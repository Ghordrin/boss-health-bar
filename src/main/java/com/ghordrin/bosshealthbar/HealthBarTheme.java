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
	// Blue with gold and white.
	SARADOMIN("Saradomin", 0x2F62C8, 0x1C3C84, 0xC9A54A, 0xF4F0E0),
	// Red with black and flame orange.
	ZAMORAK("Zamorak", 0xB01818, 0x6A0C0C, 0x5A4E4E, 0xF0A030),
	// Green with brown and cream.
	GUTHIX("Guthix", 0x3E9A3A, 0x28602A, 0x8A7450, 0xE8DDB0),
	// White with silver and sky blue.
	ARMADYL("Armadyl", 0xE2DED0, 0xA4A090, 0x6E8E9A, 0x7FD6E6),
	// Olive and brown with bronze.
	BANDOS("Bandos", 0x7A8A3A, 0x5A4A26, 0x7A6448, 0xD0A870),
	// Purple with black.
	ZAROS("Zaros", 0x6E2AB0, 0x3E1670, 0x5A5068, 0xC080F0),
	// Pale crystal blue with silver.
	SEREN("Seren", 0x7ED8E6, 0x3E9CB8, 0xB8D4DA, 0xF2FCFF),
	// Gold with lapis blue.
	TUMEKEN("Tumeken", 0xE0A82E, 0xB0701C, 0x3E5E9A, 0xFFF0C0),
	// Teal water with gold.
	ELIDINIS("Elidinis", 0x2E9EA8, 0x1E6078, 0xB89A5A, 0xC8F0F0),
	// Sun orange with gold.
	RALOS("Ralos", 0xF08A1C, 0xC04818, 0xC09040, 0xFFE890),
	// Moon silver and blue.
	RANUL("Ranul", 0x8898C8, 0x4A5488, 0x9AA0B0, 0xE8ECF8),
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
