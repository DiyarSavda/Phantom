package org.phantomapi.hive;

import java.io.File;
import org.bukkit.Chunk;
import org.bukkit.block.Block;
import org.phantomapi.clust.Configurable;

/**
 * Represents a hyve
 * 
 * @author cyberpwn
 */
public interface Hyve extends Configurable
{
	public HyveType getType();
	
	public void save();
	
	public void load();
	
	public void drop();
	
	public static boolean exists(Block block)
	{
		File file = new File(block.getWorld().getWorldFolder(), "block");
		return new File(file, "hyve-block." + block.getX() + "." + block.getY() + "." + block.getZ() + ".hy").exists();
	}
	
	public static boolean exists(Chunk chunk)
	{
		File file = new File(chunk.getWorld().getWorldFolder(), "chunk");
		return new File(file, "hyve-chunk." + chunk.getX() + "." + chunk.getZ() + ".hy").exists();
	}
}