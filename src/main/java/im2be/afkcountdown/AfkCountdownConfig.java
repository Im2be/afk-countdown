package im2be.afkcountdown;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(AfkCountdownConfig.GROUP)
public interface AfkCountdownConfig  extends Config {

    String GROUP = "AfkCountdown";

    @ConfigItem(
            keyName = "renderOnLogoutStone",
            name = "Render on logout stone",
            description = "Disable for infobox, enable for text overlay on logout tab stone"
    )
    default boolean renderOnLogoutStone() {
        return false;
    }

}
