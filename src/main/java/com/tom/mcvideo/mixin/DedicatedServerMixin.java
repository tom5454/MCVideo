package com.tom.mcvideo.mixin;

import java.net.Proxy;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resource.ResourcePackManager;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ServerResourceManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.dedicated.MinecraftDedicatedServer;
import net.minecraft.util.UserCache;
import net.minecraft.util.registry.RegistryTracker.Modifiable;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;

import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.datafixers.DataFixer;

import com.tom.mcvideo.MCVideo;
import com.tom.mcvideo.ScreenManager;

@Mixin(MinecraftDedicatedServer.class)
public abstract class DedicatedServerMixin extends MinecraftServer {
	public DedicatedServerMixin(Thread thread, Modifiable modifiable, Session session, SaveProperties saveProperties,
			ResourcePackManager<ResourcePackProfile> resourcePackManager, Proxy proxy, DataFixer dataFixer,
			ServerResourceManager serverResourceManager, MinecraftSessionService minecraftSessionService,
			GameProfileRepository gameProfileRepository, UserCache userCache,
			WorldGenerationProgressListenerFactory worldGenerationProgressListenerFactory) {
		super(thread, modifiable, session, saveProperties, resourcePackManager, proxy, dataFixer, serverResourceManager,
				minecraftSessionService, gameProfileRepository, userCache, worldGenerationProgressListenerFactory);
	}

	@Inject(at = @At("RETURN"), method = "setupServer()Z")
	private void init(CallbackInfoReturnable<Boolean> info) {
		if(!info.getReturnValueZ())return;
		MCVideo.manager = new ScreenManager(9050, this::execute);
		getOverworld().getPersistentStateManager().getOrCreate(() -> MCVideo.manager.new State(getOverworld()), ScreenManager.ID);
		MCVideo.manager.start();
	}

	@Inject(at = @At("HEAD"), method = "shutdown()V")
	private void exit(CallbackInfo info) {
		if(MCVideo.manager != null) {
			MCVideo.manager.close();
			MCVideo.manager = null;
		}
	}
}
