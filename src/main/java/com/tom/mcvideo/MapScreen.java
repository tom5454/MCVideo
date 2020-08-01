package com.tom.mcvideo;

import java.util.Arrays;

import net.minecraft.item.FilledMapItem;
import net.minecraft.item.map.MapState;
import net.minecraft.item.map.MapState.PlayerUpdateTracker;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

public class MapScreen implements Screen {
	private World world;
	private MapState[] maps;
	private int w, h, startID;

	public MapScreen(World world, CompoundTag tag) {
		this(world, tag.getInt("w"), tag.getInt("h"), tag.getInt("startID"));
	}

	public MapScreen(World world, int w, int h, int ids) {
		this.world = world;
		this.w = w;
		this.h = h;
		this.startID = ids;
		maps = new MapState[w*h];
		for (int i = 0; i < maps.length; i++) {
			String mid = FilledMapItem.getMapName(ids + i);
			MapState mapState = world.getMapState(mid);
			if (mapState == null && world instanceof ServerWorld) {
				world.getNextMapId();
				mapState = new MapState(mid);
				mapState.init(0, 0, 3, false, false, world.getRegistryKey());
				mapState.locked = true;
				world.putMapState(mapState);
			} else {
				mapState.init(0, 0, 3, false, false, world.getRegistryKey());
				mapState.locked = true;
				mapState.showIcons = false;
				mapState.frames.clear();
				mapState.banners.clear();
				mapState.icons.clear();
				markDirty(mapState);
			}
			maps[i] = mapState;
		}
		clear();
	}

	/*public MapScreen(World world, int w, int h, String screenID) {
		this.world = world;
		this.w = w;
		this.h = h;
		maps = new MapState[w*h];
		for (int i = 0; i < maps.length; i++) {
			String mid = "screen_" + screenID + i;
			MapState mapState = world.getMapState(mid);
			if (mapState == null && world instanceof ServerWorld) {
				mapState = new MapState(mid);
				mapState.init(0, 0, 3, false, false, world.getRegistryKey());
				mapState.locked = true;
				Arrays.fill(mapState.colors, (byte) (21*4));
				world.putMapState(mapState);
			} else {
				mapState.init(0, 0, 3, false, false, world.getRegistryKey());
				mapState.locked = true;
				mapState.showIcons = false;
				Arrays.fill(mapState.colors, (byte) (21*4));
				mapState.frames.clear();
				mapState.banners.clear();
				mapState.icons.clear();
				markDirty(mapState);
			}
			maps[i] = mapState;
		}
	}*/

	@Override
	public void data(byte[] pixels, int w) {
		int x, y, h = pixels.length / w;
		for (y = 0; y < h; y++) {
			int my = y / 128;
			int dy = y % 128;
			for (x = 0; x < w; x++) {
				maps[x / 128 + my * this.w].colors[x % 128 + dy * 128] = pixels[x + y * w];
			}
		}
		for (int i = 0; i < maps.length; i++) {
			markDirty(maps[i]);
		}
	}

	private static void markDirty(MapState st) {
		st.markDirty();
		for (PlayerUpdateTracker playerUpdateTracker : st.updateTrackers) {
			playerUpdateTracker.startX = 0;
			playerUpdateTracker.startZ = 0;
			playerUpdateTracker.endX = 127;
			playerUpdateTracker.endZ = 127;
			playerUpdateTracker.dirty = true;
		}
	}

	@Override
	public void writeTo(CompoundTag tag) {
		tag.putInt("w", w);
		tag.putInt("h", h);
		tag.putInt("startID", startID);
	}

	@Override
	public String getID() {
		return "map";
	}

	@Override
	public void clear() {
		for (int i = 0; i < maps.length; i++) {
			Arrays.fill(maps[i].colors, (byte) (21*4));
		}
	}

	/*static public void data(byte[] pixels, int w) {
		int h = pixels.length / w;
		for (int y = 0; y < h; y++) {
			int my = y / 64;
			for (int x = 0; x < w; x++) {
				int mx = x / 64;
				maps[mx + my * this.w].colors[x % 64 + (y % 64) * 64] = pixels[x + y * w];
			}
		}
	}*/
}
