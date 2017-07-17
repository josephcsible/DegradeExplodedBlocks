/*
DegradeExplodedBlocks Minecraft Mod
Copyright (C) 2017 Joseph C. Sible

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License along
with this program; if not, write to the Free Software Foundation, Inc.,
51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package josephcsible.degradeexplodedblocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import com.google.common.base.Predicate;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.InvalidBlockStateException;
import net.minecraft.command.NumberInvalidException;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.event.world.ExplosionEvent;
import net.minecraftforge.fml.client.event.ConfigChangedEvent.OnConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod(modid = DegradeExplodedBlocks.MODID, version = DegradeExplodedBlocks.VERSION, acceptedMinecraftVersions = "[1.11,)", guiFactory = "josephcsible.degradeexplodedblocks.DegradeExplodedBlocksGuiFactory")
public class DegradeExplodedBlocks {
	// XXX duplication with mcmod.info and build.gradle
	public static final String MODID = "degradeexplodedblocks";
	public static final String VERSION = "1.0.0";

	protected static final String[] DEFAULT_REPLACEMENTS = {
		"minecraft:cobblestone * minecraft:gravel",
		"minecraft:gravel * minecraft:sand",
		"minecraft:stone variant=granite minecraft:sand variant=red_sand",
		"minecraft:stone variant=smooth_granite minecraft:sand variant=red_sand",
		"minecraft:glass * minecraft:quartz_ore",
		"minecraft:glass_pane * minecraft:quartz_ore",
		"minecraft:stained_glass * minecraft:quartz_ore",
		"minecraft:stained_glass_pane * minecraft:quartz_ore",
	};

	public static class Replacement {
		public final Predicate<IBlockState> predicate;
		public final IBlockState newState;

		public Replacement(Predicate<IBlockState> predicate, IBlockState newState) {
			this.predicate = predicate;
			this.newState = newState;
		}
	}

	public static Configuration config;
	public Map<Block, List<Replacement>> replacements;
	public Logger log;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		log = event.getModLog();
		config = new Configuration(event.getSuggestedConfigurationFile());
		syncConfig();
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@SubscribeEvent
	public void onConfigChanged(OnConfigChangedEvent eventArgs) {
		if (eventArgs.getModID().equals(MODID))
			syncConfig();
	}

	protected void syncConfig() {
		replacements = new HashMap<Block, List<Replacement>>();
		config.setCategoryComment(Configuration.CATEGORY_GENERAL, "Blocks, predicates, and states are specified exactly as they are with the /testforblock and /setblock commands.\nIf multiple replacements with different predicates are specified for the same block, the earliest matching one wins.");
		// Not using getStringList to avoid overly-long list of default values in the comment
		// XXX it still appears in the GUI; that needs to be fixed too
		Property prop = config.get(Configuration.CATEGORY_GENERAL, "replacements", DEFAULT_REPLACEMENTS);
		prop.setLanguageKey("replacements");
		prop.setComment("A list of entries in the following format: <block> <dataValue|-1|state|*> <block> [dataValue|state]");
		for(String str : prop.getStringList()) {
			String[] pieces = str.split(" ");
			if(pieces.length != 3 && pieces.length != 4) {
				log.warn("Ignoring replacement with %d terms (expected 3 or 4): {}", pieces.length, str);
				continue;
			}

			ResourceLocation blockRL = new ResourceLocation(pieces[0]);
			if (!Block.REGISTRY.containsKey(blockRL)) {
				log.warn("Ignoring replacement with unknown old block: {}", str);
				continue;
			}
			Block oldBlock = Block.REGISTRY.getObject(blockRL);

			Predicate<IBlockState> predicate;
			try {
				predicate = CommandBase.convertArgToBlockStatePredicate(oldBlock, pieces[1]);
			} catch (InvalidBlockStateException e) {
				log.warn("Ignoring replacement with invalid predicate: {}", str);
				continue;
			}

			blockRL = new ResourceLocation(pieces[2]);
			if (!Block.REGISTRY.containsKey(blockRL)) {
				log.warn("Ignoring replacement with unknown new block: {}", str);
				continue;
			}
			Block newBlock = Block.REGISTRY.getObject(blockRL);

			IBlockState newState;
			if(pieces.length == 4) {
				try {
					newState = CommandBase.convertArgToBlockState(newBlock, pieces[3]);
				} catch (NumberInvalidException e) {
					log.warn("Ignoring replacement with out-of-range new block data value: {}", str);
					continue;
				} catch (InvalidBlockStateException e) {
					log.warn("Ignoring replacement with invalid new block state: {}", str);
					continue;
				}
			} else {
				newState = newBlock.getDefaultState();
			}

			if(!replacements.containsKey(oldBlock)) {
				replacements.put(oldBlock, new ArrayList<Replacement>());
			}
			replacements.get(oldBlock).add(new Replacement(predicate, newState));
		}
		if (config.hasChanged())
			config.save();
	}

	@SubscribeEvent
	public void onExplosionDetonate(ExplosionEvent.Detonate event) {
		World world = event.getWorld();
		for(BlockPos pos : event.getAffectedBlocks()) {
			IBlockState state = world.getBlockState(pos);
			Block block = state.getBlock();
			if(!replacements.containsKey(block)) {
				continue;
			}
			for(Replacement replacement : replacements.get(block)) {
				if(!replacement.predicate.apply(state)) {
					continue;
				}
				/*
				 * Meaning of flag 24:
				 * 1 off: no block update
				 * 2 off: no client notification
				 * 4 off: irrelevant since 2 is off
				 * 8 on: no re-render
				 * 16 on: no observer notification
				 * This is for performance, since all of these things will happen in doExplosionB when the block is replaced again with air.
				 */
				world.setBlockState(pos, replacement.newState, 24);
				break;
			}
		}
	}
}
