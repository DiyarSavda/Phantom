package org.phantomapi.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.phantomapi.nms.NMSX;

/**
 * Represents an entity hologram
 * 
 * @author cyberpwn
 */
public class EntityHologram implements Hologram
{
	private Entity base;
	private Player exc;
	
	/**
	 * Create a new entity hologram holder out of an entity
	 * 
	 * @param base
	 *            the entity
	 */
	public EntityHologram(Entity base)
	{
		exc = null;
		this.base = base;
	}
	
	@Override
	public Entity getHandle()
	{
		return base;
	}
	
	@Override
	public String getDisplay()
	{
		return getHandle().getCustomName();
	}
	
	@Override
	public void setDisplay(String display)
	{
		if(display == null)
		{
			getHandle().setCustomNameVisible(false);
			return;
		}
		
		getHandle().setCustomName(display);
		getHandle().setCustomNameVisible(true);
	}
	
	@Override
	public void setLocation(Location location)
	{
		getHandle().teleport(location);
	}
	
	@Override
	public void setTextLocation(Location location)
	{
		setLocation(location.clone().add(0, -0.87, 0));
	}
	
	@Override
	public void destroy()
	{
		getHandle().remove();
	}
	
	@Override
	public Location getLocation()
	{
		return getHandle().getLocation();
	}
	
	@Override
	public void setExclusive(Player p)
	{
		exc = p;
		
		NMSX.hideEntity(base);
		NMSX.showEntity(exc, base);
	}
	
	@Override
	public Player getExclusive()
	{
		return exc;
	}
}
