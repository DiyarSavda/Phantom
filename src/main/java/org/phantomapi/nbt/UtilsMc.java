package org.phantomapi.nbt;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class UtilsMc
{
	
	private static HashSet<Material> NON_SOLID_BLOCKS = new HashSet<Material>();
	
	static
	{
		NON_SOLID_BLOCKS.add(Material.AIR);
		for(Material mat : Material.values())
		{
			if(mat.isBlock() && !mat.isSolid())
			{
				NON_SOLID_BLOCKS.add(mat);
			}
		}
	}
	
	private UtilsMc()
	{
	}
	
	public static String parseColors(String str)
	{
		return ChatColor.translateAlternateColorCodes('&', str);
	}
	
	public static int parseTickDuration(String str)
	{
		int duration;
		try
		{
			duration = Integer.parseInt(str);
		}
		catch(NumberFormatException e)
		{
			duration = Utils.parseTimeDuration(str) * 20;
		}
		if(duration < 0)
		{
			return -1;
		}
		return duration;
	}
	
	public static Location airLocation(Location loc)
	{
		World world = loc.getWorld();
		int x = loc.getBlockX();
		int y = loc.getBlockY();
		int z = loc.getBlockZ();
		int maxY = world.getMaxHeight();
		while(y < maxY && !NON_SOLID_BLOCKS.contains(world.getBlockAt(x, y, z).getType()))
		{
			y++;
		}
		return new Location(world, x + 0.5, y + 0.2, z + 0.5);
	}
	
	public static Block getTargetBlock(Player player)
	{
		return getTargetBlock(player, 50);
	}
	
	public static Block getTargetBlock(Player player, int distance)
	{
		List<Block> blocks = player.getLastTwoTargetBlocks(NON_SOLID_BLOCKS, distance);
		return blocks.get(blocks.size() - 1);
	}
	
	public static ItemStack newWrittenBook(String title, String author)
	{
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();
		meta.setTitle(title);
		meta.setAuthor(author);
		book.setItemMeta(meta);
		return book;
	}
	
	public static void broadcastToWorld(World world, String message)
	{
		for(Player player : world.getPlayers())
		{
			player.sendMessage(message);
		}
	}
	
	public static Vector faceToDelta(BlockFace face)
	{
		return new Vector(1, 1, 1).add(new Vector(face.getModX(), face.getModY(), face.getModZ())).multiply(0.5);
	}
	
	public static Vector faceToDelta(BlockFace face, double distance)
	{
		Vector delta = faceToDelta(face);
		return new Vector(-0.5, -0.5, -0.5).add(delta).normalize().multiply(distance).add(delta);
	}
	
	public static ItemStack newSingleItemStack(Material material, String name)
	{
		return newSingleItemStack(material, name, (String[]) null);
	}
	
	public static ItemStack newSingleItemStack(Material material, String name, String... lore)
	{
		return newSingleItemStack(material, name, (lore == null ? null : Arrays.asList(lore)));
	}
	
	public static ItemStack newSingleItemStack(Material material, String name, List<String> lore)
	{
		ItemStack item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(name);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}
	
	public static Permission getRootPermission(Plugin plugin)
	{
		String permName = plugin.getName().toLowerCase() + ".*";
		Permission perm = Bukkit.getPluginManager().getPermission(permName);
		if(perm == null)
		{
			perm = new Permission(permName, PermissionDefault.OP);
			Bukkit.getPluginManager().addPermission(perm);
		}
		return perm;
	}
	
}
