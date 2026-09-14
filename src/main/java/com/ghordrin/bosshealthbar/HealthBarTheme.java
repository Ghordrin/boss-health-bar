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
	DUSKROSE("Duskrose", 0xC23C6E, 0x7E1E48, 0x8C6A78, 0xF6C8D8);

	private final String label;
	private final Color highColor;
	private final Color lowColor;
	private final Color accentColor;
	private final Color trailColor;

	HealthBarTheme(String label, int highColor, int lowColor, int accentColor, int trailColor)
	{
		this.label = label;
		this.highColor = new Color(highColor);
		this.lowColor = new Color(lowColor);
		this.accentColor = new Color(accentColor);
		this.trailColor = new Color(trailColor);
	}

	/**
	 * The fill color at full health. Also used for the gems on the godsword ornament.
	 */
	public Color getHighColor()
	{
		return highColor;
	}

	/**
	 * The fill color at zero health. The fill blends from the high color to this as health drops.
	 */
	public Color getLowColor()
	{
		return lowColor;
	}

	/**
	 * The color of the bar's frame, end pieces, phase markers and ornament metal.
	 */
	public Color getAccentColor()
	{
		return accentColor;
	}

	/**
	 * The color of the damage trail.
	 */
	public Color getTrailColor()
	{
		return trailColor;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
