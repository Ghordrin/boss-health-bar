package com.ghordrin.bosshealthbar;

public enum OrnamentStyle
{
	NONE("None"),
	GODSWORD("Godsword"),
	STAFF("Staff"),
	BONE("Skull and bone"),
	SCROLL("Scroll"),
	BRACKET("Brackets");

	private final String label;

	OrnamentStyle(String label)
	{
		this.label = label;
	}

	@Override
	public String toString()
	{
		return label;
	}
}
