package com.GroupAttendanceTracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class Group_AttendanceTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(GroupAttendanceTrackerPlugin.class);
		RuneLite.main(args);
	}
}