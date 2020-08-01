package com.tom.mcvideo;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;

public class MCVideo implements DedicatedServerModInitializer {
	public static ScreenManager manager;

	@Override
	public void onInitializeServer() {
		System.out.println("Hello Fabric world!");
		CommandRegistrationCallback.EVENT.register((c, d) -> {
			ScreenCommand.register(c);
		});
	}
}
