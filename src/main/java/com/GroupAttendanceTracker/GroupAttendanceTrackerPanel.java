package com.GroupAttendanceTracker;

import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

@Singleton
public class GroupAttendanceTrackerPanel extends PluginPanel
{
    private static final String CONFIG_GROUP = "GroupAttendanceTracker";

    private final GroupAttendanceTrackerConfig config;
    private final ConfigManager configManager;

    private final JTextArea attendanceArea = new JTextArea();
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton resetButton = new JButton("Reset");
    private final JButton copyButton = new JButton("Copy to Clipboard");

    private final JScrollPane scrollPane;

    // Plugin reference so we can ask it to reset internal data
    private GroupAttendanceTrackerPlugin plugin;

    @Inject
    GroupAttendanceTrackerPanel(GroupAttendanceTrackerConfig config, ConfigManager configManager)
    {
        this.config = config;
        this.configManager = configManager;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        // Header at the top (title + subtitle only)
        add(buildHeader(), BorderLayout.NORTH);

        // Attendance list takes the rest of the panel
        attendanceArea.setEditable(false);
        attendanceArea.setLineWrap(true);
        attendanceArea.setWrapStyleWord(true);
        attendanceArea.setFont(FontManager.getRunescapeSmallFont());
        attendanceArea.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        attendanceArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        attendanceArea.setMargin(new Insets(5, 5, 5, 5));

        scrollPane = new JScrollPane(attendanceArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // Footer with all control buttons (Copy below Start/Stop/Reset)
        add(buildFooter(), BorderLayout.SOUTH);

        // Initialise button states from config
        updateButtonStates(config.trackingEnabled());
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 4, 0));

        JLabel title = new JLabel("Group Attendance");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(ColorScheme.BRAND_ORANGE);

        JLabel subtitle = new JLabel("Track and copy attendance for your group.");
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);

        return header;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 0, 0, 0));

        // We’ll stack controls vertically on the right:
        // Row 1: Start | Stop | Reset
        // Row 2: Copy
        JPanel controlsColumn = new JPanel();
        controlsColumn.setOpaque(false);
        controlsColumn.setLayout(new BoxLayout(controlsColumn, BoxLayout.Y_AXIS));

        // Row 1: Start / Stop / Reset in a horizontal row
        JPanel row1 = new JPanel();
        row1.setOpaque(false);
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS));

        startButton.setFont(FontManager.getRunescapeSmallFont());
        startButton.setFocusable(false);
        startButton.addActionListener(e -> setTracking(true));

        stopButton.setFont(FontManager.getRunescapeSmallFont());
        stopButton.setFocusable(false);
        stopButton.addActionListener(e -> setTracking(false));

        resetButton.setFont(FontManager.getRunescapeSmallFont());
        resetButton.setFocusable(false);
        resetButton.addActionListener(e -> resetAttendance());

        row1.add(startButton);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(stopButton);
        row1.add(Box.createHorizontalStrut(4));
        row1.add(resetButton);

        // Row 2: Copy button below
        JPanel row2 = new JPanel();
        row2.setOpaque(false);
        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS));

        copyButton.setFont(FontManager.getRunescapeSmallFont());
        copyButton.setFocusable(false);
        copyButton.addActionListener(e -> copyAttendanceToClipboard());

        // Add a small vertical gap then align copy to the right
        row2.add(Box.createHorizontalGlue());
        row2.add(copyButton);

        controlsColumn.add(row1);
        controlsColumn.add(Box.createVerticalStrut(4));
        controlsColumn.add(row2);

        footer.add(controlsColumn, BorderLayout.EAST);
        return footer;
    }

    private void setTracking(boolean enabled)
    {
        // Flip the config value – plugin listens via ConfigChanged
        configManager.setConfiguration(CONFIG_GROUP, "trackingEnabled", enabled);
        updateButtonStates(enabled);
    }

    private void resetAttendance()
    {
        // Ask plugin to clear its internal state
        if (plugin != null)
        {
            plugin.resetAttendance();
        }
        else
        {
            // Fallback: just clear text if plugin ref isn't set yet
            attendanceArea.setText("");
        }

        // Ensure layout updates after reset as well
        attendanceArea.invalidate();
        scrollPane.invalidate();
        revalidate();
        repaint();
    }

    private void updateButtonStates(boolean trackingEnabled)
    {
        startButton.setEnabled(!trackingEnabled);
        stopButton.setEnabled(trackingEnabled);
        // Reset and Copy always enabled
    }

    private void copyAttendanceToClipboard()
    {
        String text = attendanceArea.getText();
        if (text == null)
        {
            text = "";
        }

        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    /**
     * Called by the plugin to update the rendered attendance text.
     * Also forces the scroll area to re-layout so its size matches the new content.
     */
    void updateAttendanceText(String text)
    {
        attendanceArea.setText(text == null ? "" : text);
        attendanceArea.setCaretPosition(0);

        // Force layout/scrollbox size refresh
        attendanceArea.invalidate();
        scrollPane.invalidate();
        revalidate();
        repaint();
    }

    // Backwards compat alias if you used this name anywhere else
    void setAttendanceText(String text)
    {
        updateAttendanceText(text);
    }

    // Called from the plugin's startUp() so the panel can invoke reset on it
    void setPlugin(GroupAttendanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
    }
}
