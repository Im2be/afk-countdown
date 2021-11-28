package im2be.afkcountdown;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AfkCountdownPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AfkCountdownPlugin.class);
		RuneLite.main(args);
	}
}