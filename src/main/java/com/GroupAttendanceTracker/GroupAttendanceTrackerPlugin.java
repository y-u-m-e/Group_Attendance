package com.GroupAttendanceTracker;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
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
        name = "Group Attendance",
        description = "Track Group/Event attendance using world views",
        tags = {"clan", "attendance", "group"},
        enabledByDefault = false
)
public class GroupAttendanceTrackerPlugin extends Plugin
{
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

    private NavigationButton navButton;

    // Names shown in the overlay (sorted/limited subset)
    @Getter
    private List<String> visibleNames = Collections.emptyList();

    // Total time present per player (ticks), for *all* filtered players
    private final Map<String, Integer> attendanceTicks = new HashMap<>();

    // For logging: last set of visible names
    private Set<String> lastLoggedSet = new HashSet<>();

    // Whether we’re currently tracking attendance
    private boolean trackingEnabled;

    // Icon for nav button
    private static final BufferedImage ICON = createListIcon();

    @Provides
    GroupAttendanceTrackerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(GroupAttendanceTrackerConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("Group Attendance plugin started");
        visibleNames = Collections.emptyList();
        attendanceTicks.clear();
        lastLoggedSet.clear();

        trackingEnabled = config.trackingEnabled();

        if (config.showOverlay())
        {
            overlayManager.add(overlay);
        }

        // Make panel aware of this plugin (for reset button)
        panel.setPlugin(this);

        navButton = NavigationButton.builder()
                .tooltip("Group Attendance")
                .priority(5)
                .icon(ICON)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);

        SwingUtilities.invokeLater(() -> panel.updateAttendanceText("No attendance yet."));
    }



    @Override
    protected void shutDown()
    {
        log.info("Group Attendance plugin stopped");

        overlayManager.remove(overlay);

        if (navButton != null)
        {
            clientToolbar.removeNavigation(navButton);
            navButton = null;
        }

        visibleNames = Collections.emptyList();
        attendanceTicks.clear();
        lastLoggedSet.clear();

        // Clear panel text
        SwingUtilities.invokeLater(() -> panel.updateAttendanceText("No attendance yet."));
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN && client.getLocalPlayer() != null)
        {
            log.info("Attendance: logged in as {}", client.getLocalPlayer().getName());
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("GroupAttendanceTracker"))
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
                // Stop showing current players in the overlay when tracking stops.
                // (We do NOT clear attendanceTicks so times remain in the panel.)
                visibleNames = Collections.emptyList();
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

        // If tracking is disabled, do nothing.
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

        // Collect all players from world view tree
        List<Player> allPlayers = new ArrayList<>();
        collectPlayersRecursive(rootView, allPlayers);

        // Names after filtering (clanOnly etc.), BEFORE overlay limiting
        List<String> filteredNames = new ArrayList<>();

        for (Player p : allPlayers)
        {
            if (p == null || p == local)
            {
                continue;
            }

            String name = p.getName();
            boolean isNameInFilter = false;

            // Clan-only filter (clan OR friends chat) if enabled
            if (config.ClanChat() && p.isClanMember())
            {

                    isNameInFilter = true;
            }

            // Friends Chat-only filter (OR friends chat) if enabled
            else if (config.FriendsChat() && p.isFriendsChatMember())
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

        // Sort (for overlay & panel output)
        if (config.sortAlphabetically())
        {
            filteredNames.sort(String.CASE_INSENSITIVE_ORDER);
        }

        // ---- Track time present: 1 tick per game tick while they are in filteredNames ----
        Set<String> filteredSet = new HashSet<>(filteredNames);

        for (String name : filteredSet)
        {
            attendanceTicks.merge(name, 1, Integer::sum);
        }

        // IMPORTANT:
        // Do NOT remove players who are no longer in filteredSet.
        // This keeps their names and final times in the panel.
        // attendanceTicks.keySet().removeIf(n -> !filteredSet.contains(n));

        // ---- Build overlay-visible list (limited) ----
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

        // ---- Build attendance text for panel & clipboard ----
        final String attendanceText = buildAttendanceText();
        SwingUtilities.invokeLater(() -> panel.updateAttendanceText(attendanceText));
    }



    /**
     * Recursively collect players from this worldview and all child worldviews.
     */
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

    /**
     * Build a text block like:
     *
     *   Group attendance (3)
     *   Alice - 03:24
     *   Bob   - 01:18
     */
    private String buildAttendanceText()
    {
        if (attendanceTicks.isEmpty())
        {
            return "Group attendance (0)\nNo players currently tracked.";
        }

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(attendanceTicks.entrySet());
        // Sort by longest time present desc, then name
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

    /**
     * Convert game ticks (~0.6s) to mm:ss.
     */
    private static String formatDurationTicks(int ticks)
    {
        long totalMillis = ticks * 600L; // 600 ms per tick
        long totalSeconds = totalMillis / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format("%02d:%02d", minutes, seconds);
    }

    /**
     * Simple list-style icon: three rows of [■ ███].
     */
    private static BufferedImage createListIcon()
    {
        int size = 16;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();

        g.setColor(Color.WHITE);

        // Row 1
        g.fillRect(2, 3, 3, 3);   // bullet
        g.fillRect(7, 3, 7, 3);   // line

        // Row 2
        g.fillRect(2, 7, 3, 3);
        g.fillRect(7, 7, 7, 3);

        // Row 3
        g.fillRect(2, 11, 3, 3);
        g.fillRect(7, 11, 7, 3);

        g.dispose();
        return img;
    }

    /**
     * Snapshot for tests / external use if needed.
     */
    public Map<String, Integer> getAttendanceTicks()
    {
        return Collections.unmodifiableMap(attendanceTicks);
    }

    // Small DTO if you ever want structured access elsewhere
    public static class AttendanceRecord
    {
        private final String name;
        private final int ticks;

        public AttendanceRecord(String name, int ticks)
        {
            this.name = name;
            this.ticks = ticks;
        }

        public String getName()
        {
            return name;
        }

        public int getTicks()
        {
            return ticks;
        }
    }

    void resetAttendance()
    {
        attendanceTicks.clear();
        visibleNames = Collections.emptyList();
        lastLoggedSet.clear();

        SwingUtilities.invokeLater(() -> panel.updateAttendanceText("No attendance yet."));
    }

}
