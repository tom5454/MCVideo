package com.tom.mcvideo;

import net.minecraft.nbt.CompoundTag;

public interface Screen {
	void data(byte[] pixels, int w);
	void writeTo(CompoundTag tag);
	void clear();
	String getID();
}
