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
