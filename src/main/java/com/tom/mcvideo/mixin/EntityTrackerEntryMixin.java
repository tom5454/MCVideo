package com.tom.mcvideo.mixin;

import java.util.Iterator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.network.Packet;
import net.minecraft.server.network.EntityTrackerEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

@Mixin(EntityTrackerEntry.class)
public class EntityTrackerEntryMixin {
	@Shadow Entity entity;
	@Shadow ServerWorld world;
	@Shadow int trackingTick;

	@Inject(at = @At("RETURN"), method = "tick()V")
	public void onTick(CallbackInfo cbi) {
		if (this.entity instanceof ItemFrameEntity && this.trackingTick % 10 != 0) {
			ItemFrameEntity itemFrameEntity = (ItemFrameEntity)this.entity;
			ItemStack itemStack = itemFrameEntity.getHeldItemStack();
			if (itemStack.getItem() instanceof FilledMapItem) {
				MapState mapState = FilledMapItem.getOrCreateMapState(itemStack, this.world);
				Iterator var5 = this.world.getPlayers().iterator();

				while(var5.hasNext()) {
					ServerPlayerEntity serverPlayerEntity = (ServerPlayerEntity)var5.next();
					//if(((SPA)serverPlayerEntity).incrementPacket())continue;
					//mapState.update(serverPlayerEntity, itemStack);
					Packet<?> packet = ((FilledMapItem)itemStack.getItem()).createSyncPacket(itemStack, this.world, serverPlayerEntity);
					if (packet != null) {
						serverPlayerEntity.networkHandler.sendPacket(packet);
					}
				}
			}
		}
	}
}
