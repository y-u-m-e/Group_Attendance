package com.group_attendance;

import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.List;

@Singleton
public class Group_AttendanceOverlay extends OverlayPanel
{
    private final Group_AttendancePlugin plugin;
    private final Group_AttendanceConfig config;

    @Inject
    private Group_AttendanceOverlay(Group_AttendancePlugin plugin, Group_AttendanceConfig config)
    {
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);

        // ✅ allow Alt-drag & snapping
        setMovable(true);
        setSnappable(true);
        setDragTargetable(true);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        List<String> names = plugin.getVisibleNames();

        // Title
        String title = "Nearby Players";
        panelComponent.getChildren().add(
                TitleComponent.builder()
                        .text(title)
                        .color(Color.WHITE)
                        .build()
        );

        // Count
        if (config.showCount())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Count:")
                            .right(Integer.toString(names.size()))
                            .leftColor(Color.LIGHT_GRAY)
                            .rightColor(Color.CYAN)
                            .build()
            );
        }

        if (names.isEmpty())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("No players")
                            .right("")
                            .leftColor(Color.GRAY)
                            .build()
            );
        }
        else
        {
            int index = 1;
            for (String name : names)
            {
                panelComponent.getChildren().add(
                        LineComponent.builder()
                                .left(index + ".")
                                .right(name)
                                .leftColor(Color.LIGHT_GRAY)
                                .rightColor(Color.GREEN)
                                .build()
                );
                index++;
            }
        }

        return super.render(graphics);
    }
}
