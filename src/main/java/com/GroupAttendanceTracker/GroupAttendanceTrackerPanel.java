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

    private static final Color BUTTON_COLOR = new Color(60, 60, 60);
    private static final Color START_HOVER = new Color(45, 120, 45);
    private static final Color STOP_HOVER = new Color(140, 45, 45);
    private static final Color RESET_HOVER = new Color(140, 110, 30);
    private static final Color COPY_HOVER = new Color(45, 90, 140);
    private static final Color SEPARATOR_COLOR = new Color(70, 70, 70);

    private final GroupAttendanceTrackerConfig config;
    private final ConfigManager configManager;

    private final JPanel listContainer = new JPanel();
    private final JLabel countLabel = new JLabel("0 players tracked");
    private final JButton startButton = new JButton("Start");
    private final JButton stopButton = new JButton("Stop");
    private final JButton resetButton = new JButton("Reset");
    private final JButton copyButton = new JButton("Copy to Clipboard");

    private final JScrollPane scrollPane;

    private GroupAttendanceTrackerPlugin plugin;
    private String lastRawText = "";

    @Inject
    GroupAttendanceTrackerPanel(GroupAttendanceTrackerConfig config, ConfigManager configManager)
    {
        super(false);

        this.config = config;
        this.configManager = configManager;

        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel topSection = new JPanel();
        topSection.setLayout(new BoxLayout(topSection, BoxLayout.Y_AXIS));
        topSection.setOpaque(false);

        JPanel header = buildHeader();
        header.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel controls = buildControls();
        controls.setAlignmentX(Component.LEFT_ALIGNMENT);
        JPanel countBar = buildCountBar();
        countBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        topSection.add(header);
        topSection.add(Box.createVerticalStrut(8));
        topSection.add(controls);
        topSection.add(Box.createVerticalStrut(8));
        topSection.add(countBar);
        topSection.add(Box.createVerticalStrut(4));

        add(topSection, BorderLayout.NORTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        scrollPane = new JScrollPane(listContainer);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        add(buildFooter(), BorderLayout.SOUTH);

        updateButtonStates(config.trackingEnabled());
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("Group Attendance");
        title.setFont(FontManager.getRunescapeBoldFont());
        title.setForeground(ColorScheme.BRAND_ORANGE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("Track attendance for your group.");
        subtitle.setFont(FontManager.getRunescapeSmallFont());
        subtitle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        header.add(title);
        header.add(Box.createVerticalStrut(2));
        header.add(subtitle);

        return header;
    }

    private JPanel buildControls()
    {
        JPanel controls = new JPanel(new GridLayout(1, 3, 4, 0));
        controls.setOpaque(false);
        controls.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        styleButton(startButton, START_HOVER);
        styleButton(stopButton, STOP_HOVER);
        styleButton(resetButton, RESET_HOVER);

        startButton.addActionListener(e -> setTracking(true));
        stopButton.addActionListener(e -> setTracking(false));
        resetButton.addActionListener(e -> resetAttendance());

        controls.add(startButton);
        controls.add(stopButton);
        controls.add(resetButton);

        return controls;
    }

    private JPanel buildCountBar()
    {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        bar.setBorder(new EmptyBorder(4, 6, 4, 6));
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        countLabel.setFont(FontManager.getRunescapeSmallFont());
        countLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);

        bar.add(countLabel, BorderLayout.WEST);
        return bar;
    }

    private JPanel buildFooter()
    {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(6, 0, 150, 0));

        styleButton(copyButton, COPY_HOVER);
        copyButton.addActionListener(e -> copyAttendanceToClipboard());

        footer.add(copyButton, BorderLayout.CENTER);
        return footer;
    }

    private void styleButton(JButton button, Color hoverColor)
    {
        button.setFont(FontManager.getRunescapeSmallFont());
        button.setFocusable(false);
        button.setForeground(Color.WHITE);
        button.setBackground(BUTTON_COLOR);
        button.setBorder(new EmptyBorder(5, 8, 5, 8));
        button.setOpaque(true);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e)
            {
                if (button.isEnabled())
                {
                    button.setBackground(hoverColor);
                }
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e)
            {
                button.setBackground(button.isEnabled() ? BUTTON_COLOR : BUTTON_COLOR.darker());
            }
        });
    }

    private void setTracking(boolean enabled)
    {
        configManager.setConfiguration(CONFIG_GROUP, "trackingEnabled", enabled);
        updateButtonStates(enabled);
    }

    private void resetAttendance()
    {
        if (plugin != null)
        {
            plugin.resetAttendance();
        }
        else
        {
            listContainer.removeAll();
            listContainer.revalidate();
            listContainer.repaint();
        }
    }

    private void updateButtonStates(boolean trackingEnabled)
    {
        startButton.setEnabled(!trackingEnabled);
        stopButton.setEnabled(trackingEnabled);
        startButton.setBackground(trackingEnabled ? BUTTON_COLOR.darker() : BUTTON_COLOR);
        stopButton.setBackground(trackingEnabled ? BUTTON_COLOR : BUTTON_COLOR.darker());
    }

    private void copyAttendanceToClipboard()
    {
        String text = lastRawText;
        if (text == null || text.isEmpty())
        {
            text = "";
        }
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
    }

    void updateAttendanceText(String text)
    {
        lastRawText = (text == null) ? "" : text;

        listContainer.removeAll();

        if (text == null || text.isEmpty() || text.startsWith("No attendance"))
        {
            countLabel.setText("0 players tracked");
            JLabel empty = new JLabel("No players tracked yet.");
            empty.setFont(FontManager.getRunescapeSmallFont());
            empty.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
            empty.setBorder(new EmptyBorder(12, 8, 12, 8));
            empty.setHorizontalAlignment(SwingConstants.CENTER);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            listContainer.add(empty);
        }
        else
        {
            String[] lines = text.split("\n");
            int playerCount = 0;

            for (String line : lines)
            {
                if (line.startsWith("Group attendance"))
                {
                    continue;
                }

                String trimmed = line.trim();
                if (trimmed.isEmpty())
                {
                    continue;
                }

                int dashIdx = trimmed.lastIndexOf(" - ");
                if (dashIdx < 0)
                {
                    continue;
                }

                String name = trimmed.substring(0, dashIdx).trim();
                String duration = trimmed.substring(dashIdx + 3).trim();
                playerCount++;

                if (playerCount > 1)
                {
                    JSeparator sep = new JSeparator(SwingConstants.HORIZONTAL);
                    sep.setForeground(SEPARATOR_COLOR);
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    listContainer.add(sep);
                }

                listContainer.add(buildPlayerRow(playerCount, name, duration));
            }

            countLabel.setText(playerCount + (playerCount == 1 ? " player" : " players") + " tracked");
        }

        listContainer.revalidate();
        listContainer.repaint();
        scrollPane.revalidate();
    }

    private JPanel buildPlayerRow(int rank, String name, String duration)
    {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setBorder(new EmptyBorder(4, 6, 4, 6));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        JLabel rankLabel = new JLabel(rank + ". ");
        rankLabel.setFont(FontManager.getRunescapeSmallFont());
        rankLabel.setForeground(new Color(140, 140, 140));
        rankLabel.setPreferredSize(new Dimension(22, 16));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(FontManager.getRunescapeSmallFont());
        nameLabel.setForeground(Color.WHITE);

        JLabel durationLabel = new JLabel(duration);
        durationLabel.setFont(FontManager.getRunescapeSmallFont());
        durationLabel.setForeground(ColorScheme.BRAND_ORANGE);

        JPanel leftSide = new JPanel(new BorderLayout());
        leftSide.setOpaque(false);
        leftSide.add(rankLabel, BorderLayout.WEST);
        leftSide.add(nameLabel, BorderLayout.CENTER);

        row.add(leftSide, BorderLayout.CENTER);
        row.add(durationLabel, BorderLayout.EAST);

        return row;
    }

    void setAttendanceText(String text)
    {
        updateAttendanceText(text);
    }

    void setPlugin(GroupAttendanceTrackerPlugin plugin)
    {
        this.plugin = plugin;
    }
}
