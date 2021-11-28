package im2be.afkcountdown;

import javax.inject.Inject;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;
import java.time.Duration;

@Slf4j
@PluginDescriptor(
	name = "Afk Countdown",
	description = "Shows you exactly how many seconds it will take before you log out due to inactivity",
	tags = {"afk", "timer", "countdown", "log", "logout"}
)
public class AfkCountdownPlugin extends Plugin
{
	public static long AFK_LOG_TICKS = 15000;
	public static Duration AFK_LOG_TIME = Duration.ofMinutes(5);


	@Inject
	private Client client;

	@Getter
	private AfkCountdownTimer currentTimer;

	@Inject
	private InfoBoxManager infoBoxManager;

	private static final BufferedImage LOGOUT_IMAGE;
	private boolean active = false;

	private long lastIdleTicks = -1;

	static
	{
		LOGOUT_IMAGE = ImageUtil.loadImageResource(AfkCountdownPlugin.class, "logout_icon.png");
	}

	@Override
	protected void startUp() throws Exception
	{
		active = true;
	}

	@Override
	protected void shutDown() throws Exception
	{
		active = false;
		lastIdleTicks = -1;
		removeTimer();
	}


	private int getIdleTicks()
	{
		return Math.min(client.getKeyboardIdleTicks(), client.getMouseIdleTicks());
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (!active)
		{
			return;
		}

		if (client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}

		final long idleTicks = getIdleTicks();
		final long diff = AFK_LOG_TICKS - idleTicks;

		if (diff < 0)
		{
			return;
		}

		if (lastIdleTicks == -1 || idleTicks < lastIdleTicks)
		{
			final long afkLogMillis = AFK_LOG_TIME.toMillis();
			final long durationMillis = diff * afkLogMillis / AFK_LOG_TICKS + 999;

			if (durationMillis >= 0)
			{
				createTimer(Duration.ofMillis(durationMillis));
			}
			else
			{
				removeTimer();
			}
		}

		lastIdleTicks = idleTicks;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			removeTimer();
			lastIdleTicks = -1;
		}
	}


	private void removeTimer()
	{
		infoBoxManager.removeInfoBox(currentTimer);
		currentTimer = null;
	}

	private void createTimer(Duration duration)
	{
		long ms = duration.toMillis();
		long seconds = ms / 1000;
		long SS = seconds % 60;
		long HH = seconds / 3600;
		long MM = (seconds % 3600) / 60;
		String timeInHHMMSS = String.format("%02d:%02d:%02d.%03d", HH, MM, SS, ms % 1000);
		log.info("Timer start: " + timeInHHMMSS);

		removeTimer();
		currentTimer = new AfkCountdownTimer(duration, LOGOUT_IMAGE, this, true);
		infoBoxManager.addInfoBox(currentTimer);
	}
}