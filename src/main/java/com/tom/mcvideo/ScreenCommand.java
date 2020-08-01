package com.tom.mcvideo;

import static net.minecraft.server.command.CommandManager.*;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

public class ScreenCommand {
	private static final DynamicCommandExceptionType EX = new DynamicCommandExceptionType(e -> new LiteralMessage(String.valueOf(e)));
	public static void register(CommandDispatcher<ServerCommandSource> mngr) {
		mngr.register(literal("screen").
				requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(3)).
				then(literal("create").
						then(argument("name", StringArgumentType.word()).
								then(literal("frame").
										executes(ScreenCommand::createFrameScreen)
										)/*.
								then(literal("block").
										executes(ScreenCommand::createBlockScreen)
										)*/
								)
						).
				then(literal("markers").
						executes(ScreenCommand::giveMarkers)
						)
				);
	}
	private static int createFrameScreen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ScreenPos p = detectScreen(context, false);
		if(p.facing.getAxis() == Axis.Y)throw EX.create("WIP");
		int xl = (int) Math.sqrt(new BlockPos(p.p1.getX(), 0, p.p1.getZ()).getSquaredDistance(new BlockPos(p.p2.getX(), 0, p.p2.getZ())));
		int yl = Math.abs(p.p1.getY() - p.p2.getY());
		ServerPlayerEntity user = context.getSource().getPlayer();
		int id = ((ServerWorld)user.world).getNextMapId();
		MapScreen screen = new MapScreen(user.world, xl+1, yl+1, id);
		String name = context.getArgument("name", String.class);
		MCVideo.manager.addScreen(name, screen);
		for(int x = 0;x <= xl;x++) {
			for(int y = 0;y <= yl;y++) {
				BlockPos pos = new BlockPos(p.facing.getAxis() == Axis.X ? p.p1.getX() : p.p1.getX() + x * (p.facing.getDirection().offset()),
						p.p1.getY() - y,
						p.facing.getAxis() == Axis.Z ? p.p1.getZ() : p.p1.getZ() + x * (-p.facing.getDirection().offset()));
				//user.world.setBlockState(pos, Blocks.FURNACE.getDefaultState().with(Properties.HORIZONTAL_FACING, p.facing));

				List<ItemFrameEntity> ents = user.world.getEntities(ItemFrameEntity.class, new Box(pos), null);
				ItemFrameEntity e;
				if(!ents.isEmpty()) {
					e = ents.get(0);
					e.setRotation(0);
				} else {
					e = new ItemFrameEntity(user.world, pos, p.facing);
					user.world.spawnEntity(e);
				}
				ItemStack value = new ItemStack(Items.FILLED_MAP);
				value.setTag(new CompoundTag());
				value.getTag().putInt("map", id + x + y * xl + y);
				e.setHeldItemStack(value, true);
			}
		}
		String size = ((xl+1) * 128) + "x" + ((yl+1) * 128);
		context.getSource().sendFeedback(new LiteralText("Created screen '" + name + "' with a size of ").append(new LiteralText("[" + size + "]").formatted(Formatting.GREEN).styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, size)))).append(new LiteralText(".")), true);
		/*Iterator<BlockPos> itr = BlockPos.iterate(p.p1, p.p2).iterator();
		while(itr.hasNext()) {
			BlockPos pos = itr.next();
			List<ItemFrameEntity> ents = user.world.getEntities(ItemFrameEntity.class, new Box(pos), null);
			if(!ents.isEmpty()) {
				ItemFrameEntity e = ents.get(0);
				e.setRotation(0);
				ItemStack value = new ItemStack(Items.FILLED_MAP);
				value.setTag(new CompoundTag());
				//value.getTag().putInt("map", id);
				e.setHeldItemStack(value, true);
			}
		}*/
		return 0;
	}
	private static int createBlockScreen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		throw EX.create("WIP");
	}
	private static int giveMarkers(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerPlayerEntity user = context.getSource().getPlayer();
		ItemStack is = new ItemStack(Items.ITEM_FRAME);
		is.setTag(new CompoundTag());
		is.setCustomName(new LiteralText("Screen Marker 1"));
		ListTag tag = new ListTag();
		is.getTag().put("Lore", tag);
		tag.add(StringTag.of("Top left corner Marker"));
		is.getTag().putInt("screen_marker_id", 1);
		user.inventory.insertStack(is);

		is = new ItemStack(Items.ITEM_FRAME);
		is.setTag(new CompoundTag());
		is.setCustomName(new LiteralText("Screen Marker 2"));
		tag = new ListTag();
		is.getTag().put("Lore", tag);
		tag.add(StringTag.of("Bottom right corner Marker"));
		is.getTag().putInt("screen_marker_id", 2);
		user.inventory.insertStack(is);

		return 1;
	}
	private static ScreenPos detectScreen(CommandContext<ServerCommandSource> context, boolean onBlock) throws CommandSyntaxException {
		ServerPlayerEntity user = context.getSource().getPlayer();
		List<ItemFrameEntity> ents = user.world.getEntities(ItemFrameEntity.class, new Box(user.getBlockPos().add(-32, -32, -32), user.getBlockPos().add(32, 32, 32)), e -> e.getHeldItemStack().getItem() == Items.ITEM_FRAME);
		List<ItemFrameEntity> id1 = new ArrayList<>();
		List<ItemFrameEntity> id2 = new ArrayList<>();
		for (ItemFrameEntity e : ents) {
			ItemStack st = e.getHeldItemStack();
			if(st.hasTag()) {
				int id = st.getTag().getInt("screen_marker_id");
				switch (id) {
				case 1:
					id1.add(e);
					break;

				case 2:
					id2.add(e);
					break;

				default:
					break;
				}
			}
		}
		if(id1.isEmpty())throw EX.create("Marker 1 not found");
		if(id2.isEmpty())throw EX.create("Marker 2 not found");
		if(id1.size() > 1)throw EX.create("Multipile Marker 1 found");
		if(id2.size() > 1)throw EX.create("Multipile Marker 2 found");
		ItemFrameEntity m1 = id1.get(0);
		ItemFrameEntity m2 = id2.get(0);
		Direction dir = m1.getHorizontalFacing();
		if(dir != m2.getHorizontalFacing())throw EX.create("Markers facing in different directions");
		ScreenPos ret = new ScreenPos();
		ret.p1 = m1.getDecorationBlockPos();
		ret.p2 = m2.getDecorationBlockPos();
		ret.facing = dir;
		if(onBlock) {
			ret.p1 = ret.p1.offset(dir.getOpposite());
			ret.p2 = ret.p2.offset(dir.getOpposite());
		}
		if(!(ret.p1.getX() == ret.p2.getX() || ret.p1.getY() == ret.p2.getY() || ret.p1.getZ() == ret.p2.getZ()))
			throw EX.create("Markers not in same axis");
		return ret;
	}
	/*.
				executes(e -> {
					MCVideo.manager.addScreen("test", new MapScreen(e.getSource().getWorld(), 5, 4, 0));
					return 1;
				})*/
}
