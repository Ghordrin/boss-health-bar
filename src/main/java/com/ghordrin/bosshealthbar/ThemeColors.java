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
	Color fillLow;
	Color trail;

	/**
	 * The frame, end pieces, underline and phase markers.
	 */
	Color frame;

	/**
	 * The metal of the ornaments. Pieces made of another material mix it with that material's color.
	 */
	Color ornament;
	Color gem;

	/**
	 * The opponent's name and the damage number.
	 */
	Color text;
	Color levelText;
	Color hitpointsText;
	Color defeatedText;
}
