package com.GroupAttendanceTracker;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;


@Slf4j
@PluginDescriptor(
        name = "Group Attendance Tracker",
        description = "Track Group/Event attendance using world views",
        tags = {"clan", "attendance", "group"}
)
public class GroupAttendanceTrackerPlugin extends Plugin
{
    private static final String CONFIG_GROUP_KEY = "GroupAttendanceTracker";
    private static final String DATA_KEY = "attendanceData";
    private static final int SAVE_INTERVAL_TICKS = 50;
    private static final BufferedImage ICON = createListIcon();

    @Inject
    private Client client;

    @Inject
    private GroupAttendanceTrackerConfig config;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private GroupAttendanceTrackerOverlay overlay;

    @Inject
    private ClientToolbar clientToolbar;

    @Inject
    private GroupAttendanceTrackerPanel panel;

    @Inject
    private ConfigManager configManager;

    private NavigationButton navButton;

    @Getter
    private List<String> visibleNames = Collections.emptyList();

    private final Map<String, Integer> attendanceTicks = new HashMap<>();
    private boolean trackingEnabled;
    private int ticksSinceLastSave = 0;

    @Provides
    GroupAttendanceTrackerConfig provideConfig(ConfigManager cm)
    {
        return cm.getConfig(GroupAttendanceTrackerConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("Group Attendance plugin started");
        visibleNames = Collections.emptyList();
        ticksSinceLastSave = 0;

        trackingEnabled = config.trackingEnabled();

        loadAttendance();

        if (config.showOverlay())
        {
            overlayManager.add(overlay);
        }

        panel.setPlugin(this);

        navButton = NavigationButton.builder()
                .tooltip("Group Attendance")
                .priority(5)
                .icon(ICON)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        final String text = attendanceTicks.isEmpty()
                ? "No attendance yet."
                : buildAttendanceText();
        SwingUtilities.invokeLater(() -> panel.updateAttendanceText(text));
    }

    @Override
    protected void shutDown()
    {
        log.info("Group Attendance plugin stopped");

        saveAttendance();

        overlayManager.remove(overlay);

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        visibleNames = Collections.emptyList();
        attendanceTicks.clear();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null)
        {
            log.info("Attendance: logged in as {}", client.getLocalPlayer().getName());
        }

        if (event.getGameState() == GameState.LOGIN_SCREEN
                || event.getGameState() == GameState.HOPPING)
        {
            saveAttendance();
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals(CONFIG_GROUP_KEY))
        {
            return;
        }

        if ("showOverlay".equals(event.getKey()))
        {
            if (config.showOverlay())
            {
                overlayManager.add(overlay);
            }
            else
            {
                overlayManager.remove(overlay);
            }
        }
        else if ("trackingEnabled".equals(event.getKey()))
        {
            trackingEnabled = config.trackingEnabled();

            if (!trackingEnabled)
            {
                visibleNames = Collections.emptyList();
                saveAttendance();
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        if (client.getGameState() != GameState.LOGGED_IN)
        {
            return;
        }

        if (!trackingEnabled)
        {
            return;
        }

        final Player local = client.getLocalPlayer();
        if (local == null)
        {
            visibleNames = Collections.emptyList();
            return;
        }

        final WorldView rootView = client.getTopLevelWorldView();
        if (rootView == null)
        {
            visibleNames = Collections.emptyList();
            return;
        }

        List<Player> allPlayers = new ArrayList<>();
        collectPlayersRecursive(rootView, allPlayers);

        List<String> filteredNames = new ArrayList<>();

        for (Player p : allPlayers)
        {
            if (p == null)
            {
                continue;
            }

            String name = p.getName();
            boolean isLocalPlayer = (p == local);
            boolean isNameInFilter = false;

            if (isLocalPlayer)
            {
                isNameInFilter = config.trackSelf();
            }
            else if (config.ClanChat() && p.isClanMember())
            {
                isNameInFilter = true;
            }
            else if (config.FriendsChat() && p.isFriendsChatMember())
            {
                isNameInFilter = true;
            }
            else if (config.GuestClanChat() && isGuestClanMember(name))
            {
                isNameInFilter = true;
            }
            else if (config.PublicChat())
            {
                isNameInFilter = true;
            }

            if (name != null && !name.isEmpty() && isNameInFilter)
            {
                filteredNames.add(name);
            }
        }

        if (config.sortAlphabetically())
        {
            filteredNames.sort(String.CASE_INSENSITIVE_ORDER);
        }

        Set<String> filteredSet = new HashSet<>(filteredNames);

        for (String name : filteredSet)
        {
            attendanceTicks.merge(name, 1, Integer::sum);
        }

        int max = Math.max(1, config.maxPlayers());
        List<String> overlayNames;
        if (filteredNames.size() > max)
        {
            overlayNames = new ArrayList<>(filteredNames.subList(0, max));
        }
        else
        {
            overlayNames = new ArrayList<>(filteredNames);
        }

        visibleNames = Collections.unmodifiableList(overlayNames);

        final String attendanceText = buildAttendanceText();
        SwingUtilities.invokeLater(() -> panel.updateAttendanceText(attendanceText));

        ticksSinceLastSave++;
        if (ticksSinceLastSave >= SAVE_INTERVAL_TICKS)
        {
            saveAttendance();
            ticksSinceLastSave = 0;
        }
    }

    private boolean isGuestClanMember(String name)
    {
        if (name == null)
        {
            return false;
        }
        ClanChannel guestClan = client.getGuestClanChannel();
        return guestClan != null && guestClan.findMember(name) != null;
    }

    private void collectPlayersRecursive(WorldView worldView, List<Player> out)
    {
        for (Player p : worldView.players())
        {
            if (p != null)
            {
                out.add(p);
            }
        }

        for (WorldView child : worldView.worldViews())
        {
            collectPlayersRecursive(child, out);
        }
    }

    private String buildAttendanceText()
    {
        if (attendanceTicks.isEmpty())
        {
            return "Group attendance (0)\nNo players currently tracked.";
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(attendanceTicks.entrySet());
        entries.sort(Comparator
                .comparingInt((Map.Entry<String, Integer> e) -> e.getValue()).reversed()
                .thenComparing(Map.Entry::getKey, String.CASE_INSENSITIVE_ORDER));

        StringBuilder sb = new StringBuilder();
        sb.append("Group attendance (").append(entries.size()).append(")\n");

        for (Map.Entry<String, Integer> e : entries)
        {
            String name = e.getKey();
            int ticks = e.getValue();
            String duration = formatDurationTicks(ticks);
            sb.append(name).append(" - ").append(duration).append('\n');
        }

        return sb.toString();
    }

    private static String formatDurationTicks(int ticks)
    {
        long totalMillis = ticks * 600L;
        long totalSeconds = totalMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // --- Persistence ---

    private void saveAttendance()
    {
        if (attendanceTicks.isEmpty())
        {
            configManager.unsetConfiguration(CONFIG_GROUP_KEY, DATA_KEY);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : attendanceTicks.entrySet())
        {
            if (sb.length() > 0)
            {
                sb.append(';');
            }
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }

        configManager.setConfiguration(CONFIG_GROUP_KEY, DATA_KEY, sb.toString());
        log.debug("Saved attendance for {} players", attendanceTicks.size());
    }

    private void loadAttendance()
    {
        attendanceTicks.clear();

        String data = configManager.getConfiguration(CONFIG_GROUP_KEY, DATA_KEY);
        if (data == null || data.isEmpty())
        {
            return;
        }

        for (String entry : data.split(";"))
        {
            int eq = entry.lastIndexOf('=');
            if (eq < 1)
            {
                continue;
            }

            String name = entry.substring(0, eq);
            try
            {
                int ticks = Integer.parseInt(entry.substring(eq + 1));
                attendanceTicks.put(name, ticks);
            }
            catch (NumberFormatException e)
            {
                log.warn("Skipping malformed attendance entry: {}", entry);
            }
        }

        log.info("Loaded attendance for {} players", attendanceTicks.size());
    }

    // --- Icon ---

    private static BufferedImage createListIcon()
    {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(new Color(0x2a1800));
        g.fillRect(1, 3, 13, 11);

        g.setColor(new Color(0x5a0000));
        g.fillRect(1, 3, 13, 4);

        g.setColor(new Color(0xc03020));
        g.fillRect(2, 3, 11, 1);

        g.setColor(new Color(0xc8a030));
        g.fillRect(4, 1, 2, 4);
        g.fillRect(10, 1, 2, 4);

        g.setColor(new Color(0xffe060));
        g.fillRect(4, 1, 1, 1);
        g.fillRect(10, 1, 1, 1);

        g.setColor(new Color(0xc8a030));
        g.fillRect(2, 9, 2, 2);
        g.fillRect(6, 9, 2, 2);
        g.fillRect(10, 9, 2, 2);

        g.setColor(new Color(0x4a3010));
        g.fillRect(2, 12, 2, 1);
        g.fillRect(6, 12, 2, 1);
        g.fillRect(10, 12, 2, 1);

        g.setColor(new Color(0xffe060));
        g.fillRect(2, 9, 1, 1);
        g.fillRect(6, 9, 1, 1);
        g.fillRect(10, 9, 1, 1);

        g.setColor(new Color(0xc8a030));
        g.drawRect(1, 3, 12, 10);

        g.dispose();
        return img;
    }

    void resetAttendance()
    {
        attendanceTicks.clear();
        visibleNames = Collections.emptyList();

        configManager.unsetConfiguration(CONFIG_GROUP_KEY, DATA_KEY);

        SwingUtilities.invokeLater(() -> panel.updateAttendanceText("No attendance yet."));
    }
}
