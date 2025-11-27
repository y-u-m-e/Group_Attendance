package com.group_attendance;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class Group_AttendanceTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(Group_AttendancePlugin.class);
		RuneLite.main(args);
	}
}