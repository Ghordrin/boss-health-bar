package com.ghordrin.bosshealthbar;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BossHealthBarPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BossHealthBarPlugin.class);
		RuneLite.main(args);
	}
}
