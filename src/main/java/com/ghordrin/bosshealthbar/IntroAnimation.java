package com.ghordrin.bosshealthbar;

/**
 * How the bar appears when it starts showing an opponent.
 */
public enum IntroAnimation
{
	FADE("Fade", false, false),
	SLIDE("Slide in", true, false),
	EXPAND("Expand", false, true),
	SLIDE_AND_EXPAND("Slide in and expand", true, true);

	private final String label;
	// Whether the bar rises into place from below.
	final boolean slide;
	// Whether the bar widens from its center and the fill sweeps up to the current health.
	final boolean expand;

	IntroAnimation(String label, boolean slide, boolean expand)
	{
		this.label = label;
		this.slide = slide;
		this.expand = expand;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
