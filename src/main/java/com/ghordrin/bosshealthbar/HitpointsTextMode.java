package com.ghordrin.bosshealthbar;

public enum HitpointsTextMode
{
	NONE("None"),
	PERCENTAGE("Percentage"),
	HITPOINTS("Hitpoints"),
	BOTH("Both");

	private final String label;

	HitpointsTextMode(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
