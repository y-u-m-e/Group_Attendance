package com.group_attendance;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("group_attendance")
public interface Group_AttendanceConfig extends Config
{
    @ConfigItem(
            keyName = "showOverlay",
            name = "Show Overlay",
            description = "Show the on-screen attendance overlay box"
    )
    default boolean showOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "trackingEnabled",
            name = "Tracking enabled",
            description = "If disabled, attendance timers will not update"
    )
    default boolean trackingEnabled()
    {
        return true;
    }

    @ConfigItem(
            keyName = "ClanChat",
            name = "Track Clan Chat",
            description = "Allow members of Clan chat to be tracked"
    )
    default boolean ClanChat()
    {
        return true;
    }

    @ConfigItem(
            keyName = "FriendsChat",
            name = "Track Friends Chat",
            description = "Allow members of Friends chat to be tracked"
    )
    default boolean FriendsChat()
    {
        return true;
    }


    @ConfigItem(
            keyName = "PublicChat",
            name = "Track All Players",
            description = "Allow All visible players to be tracked"
    )
    default boolean PublicChat()
    {
        return true;
    }

    @Range(min = 1, max = 50)
    @ConfigItem(
            keyName = "maxPlayers",
            name = "Max players in overlay",
            description = "Maximum number of players to show in the overlay list"
    )
    default int maxPlayers()
    {
        return 25;
    }

    @ConfigItem(
            keyName = "showCount",
            name = "Show player count",
            description = "Show the total number of visible players in the overlay header"
    )
    default boolean showCount()
    {
        return true;
    }

    @ConfigItem(
            keyName = "sortAlphabetically",
            name = "Sort alphabetically",
            description = "Sort player names alphabetically in the overlay"
    )
    default boolean sortAlphabetically()
    {
        return true;
    }

}
