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
import lombok.Builder;
import lombok.Value;

/**
 * The colors the bar is drawn with, taken from a built-in theme or from the custom color settings.
 */
@Value
@Builder
class ThemeColors
{
	// The text colors all built-in themes use.
	static final Color DEFAULT_TEXT = new Color(0xE8E2D4);
	static final Color DEFAULT_HITPOINTS_TEXT = new Color(0xC8C0B0);

	/**
	 * The fill color at full health. The fill blends from this to {@link #fillLow} as health drops.
	 */
	Color fillHigh;

	/**
	 * The fill color at zero health.
	 */
	Color fillLow;

	/**
	 * The color of the damage trail.
	 */
	Color trail;

	/**
	 * The color of the bar's frame, end pieces, underline and phase markers.
	 */
	Color frame;

	/**
	 * The metal color of the ornaments.
	 */
	Color ornament;

	/**
	 * The color of the gems on the ornaments.
	 */
	Color gem;

	/**
	 * The color of the name and damage number.
	 */
	Color text;

	/**
	 * The color of the combat level next to the name.
	 */
	Color levelText;

	/**
	 * The color of the hitpoints text below the bar.
	 */
	Color hitpointsText;

	/**
	 * The color of the "Defeated" label.
	 */
	Color defeatedText;
}
