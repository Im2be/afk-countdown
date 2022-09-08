package im2be.afkcountdown;

import javax.inject.Inject;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.GameState;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@PluginDescriptor(
	name = "AFK Countdown",
	description = "Shows you exactly how many seconds it will take before you log out due to inactivity",
	tags = {"afk", "timer", "countdown", "log", "logout"}
)
public class AfkCountdownPlugin extends Plugin
{
	@Inject
	private Client client;

	@Getter
	private AfkCountdownTimer currentTimer;

	@Getter Instant timerStartTime;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private AfkCountdownOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private AfkCountdownConfig config;

	private static final BufferedImage LOGOUT_IMAGE;
	private boolean active = false;

	private long lastIdleDuration = -1;

	static
	{
		LOGOUT_IMAGE = ImageUtil.loadImageResource(AfkCountdownPlugin.class, "logout_icon.png");
	}

	@Override
	protected void startUp() throws Exception
	{
		active = true;
		if (config.renderOnLogoutStone())
			overlayManager.add(overlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		active = false;
		lastIdleDuration = -1;
		removeTimer();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event) {
		if (!AfkCountdownConfig.GROUP.equals(event.getGroup())) return;

		if ("renderOnLogoutStone".equals(event.getKey())) {
			if (config.renderOnLogoutStone()) {
				overlayManager.add(overlay);
				removeTimer();
			} else {
				overlayManager.remove(overlay);

				// emulate infobox timer creation similar to onClientTick does
				setTimer(Duration.ofMillis(getDurationMillis()));
			}
		}
	}

	private long getDurationMillis()
	{
		return Constants.CLIENT_TICK_LENGTH * (client.getIdleTimeout() - getIdleTicks()) + 999;
	}

	@Provides
	AfkCountdownConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(AfkCountdownConfig.class);
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

		final long durationMillis = getDurationMillis();

		if (durationMillis < 0)
		{
			return;
		}

		if (lastIdleDuration == -1 || durationMillis < lastIdleDuration)
		{

			if (durationMillis >= 0)
			{
				setTimer(Duration.ofMillis(durationMillis));
				overlay.setTimer(durationMillis);
			}
			else
			{
				removeTimer();
			}
		}

		lastIdleDuration = durationMillis;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			removeTimer();
			lastIdleDuration = -1;
		}
	}


	private void removeTimer()
	{
		infoBoxManager.removeInfoBox(currentTimer);
		currentTimer = null;
		timerStartTime = null;
	}

	private void setTimer(Duration duration)
	{
		if (config.renderOnLogoutStone()) return;

		final Instant now = Instant.now();
		if (currentTimer == null)
		{
			currentTimer = new AfkCountdownTimer(duration, LOGOUT_IMAGE, this);
			timerStartTime = now;
			infoBoxManager.addInfoBox(currentTimer);
		}
		else
		{
			final Duration newDuration = duration.plus(Duration.between(timerStartTime, now));
			currentTimer.setDuration(newDuration);
		}
	}
}
