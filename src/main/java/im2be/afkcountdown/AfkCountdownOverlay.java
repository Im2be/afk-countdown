package im2be.afkcountdown;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.config.FontType;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.*;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
public class AfkCountdownOverlay extends Overlay {

    private final Client client;
    private final AfkCountdownConfig config;
    private final ConfigManager configManager;

    private Instant endTime;

    @Inject
    public AfkCountdownOverlay(Client client, AfkCountdownPlugin plugin, AfkCountdownConfig config, ConfigManager configManager) {
        super(plugin);
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);
        this.client = client;
        this.config = config;
        this.configManager = configManager;
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.renderOnLogoutStone()) return null;

        Widget toDrawOn;
        if (client.isResized()) {
            toDrawOn = client.getWidget(WidgetInfo.RESIZABLE_VIEWPORT_LOGOUT_TAB);
            if (toDrawOn == null || toDrawOn.isHidden())
                toDrawOn = client.getWidget(WidgetInfo.RESIZABLE_MINIMAP_LOGOUT_BUTTON);
        } else {
            toDrawOn = client.getWidget(WidgetInfo.FIXED_VIEWPORT_LOGOUT_TAB);
        }
        if (toDrawOn == null || toDrawOn.isHidden()) return null;

        Duration timeLeft = Duration.between(Instant.now(), endTime);
        if (timeLeft.isNegative()) return null;

        String textToDraw = textFrom(timeLeft);
        FontType infoboxFontType = configManager.getConfiguration("runelite", "infoboxFontType", FontType.class);
        graphics.setFont(infoboxFontType.getFont()); // make sure we do this before calculating drawLocation

        Rectangle bounds = toDrawOn.getBounds();
        Point drawLocation = new Point((int) bounds.getCenterX() - (graphics.getFontMetrics().stringWidth(textToDraw) / 2), (int) bounds.getMaxY());
        OverlayUtil.renderTextLocation(graphics, drawLocation, textToDraw, textColor(timeLeft));

        return null;
    }

    public void setTimer(long period) {
        endTime = Instant.now().plus(Duration.of(period, ChronoUnit.MILLIS));
    }

    private Color textColor(Duration timeLeft) {
        if (timeLeft.getSeconds() < 60)
            return Color.RED.brighter();
        return Color.WHITE;
    }

    private String textFrom(Duration duration) {
        int seconds = (int) (duration.toMillis() / 1000L);

        int minutes = (seconds % 3600) / 60;
        int secs = seconds % 60;

        return String.format("%d:%02d", minutes, secs);
    }
}
