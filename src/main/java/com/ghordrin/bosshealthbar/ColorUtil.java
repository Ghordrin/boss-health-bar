package com.ghordrin.bosshealthbar;

import java.awt.Color;

final class ColorUtil
{
	private ColorUtil()
	{
	}

	/**
	 * Mixes two opaque colors: t = 0 gives a, t = 1 gives b.
	 */
	static Color lerp(Color a, Color b, float t)
	{
		return new Color(
			mix(a.getRed(), b.getRed(), t),
			mix(a.getGreen(), b.getGreen(), t),
			mix(a.getBlue(), b.getBlue(), t));
	}

	/**
	 * Mixes the color towards white by the given amount, from 0 to 1.
	 */
	static Color brighten(Color c, float amount)
	{
		return lerp(c, Color.WHITE, amount);
	}

	/**
	 * Mixes the color towards black by the given amount, from 0 to 1.
	 */
	static Color darken(Color c, float amount)
	{
		return lerp(c, Color.BLACK, amount);
	}

	/**
	 * The same color with the given alpha, from 0 to 255.
	 */
	static Color withAlpha(Color c, int alpha)
	{
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	private static int mix(int a, int b, float t)
	{
		return Math.max(0, Math.min(255, Math.round(a + (b - a) * t)));
	}
}
