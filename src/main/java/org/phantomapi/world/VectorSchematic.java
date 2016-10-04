package org.phantomapi.world;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.phantomapi.lang.GList;
import org.phantomapi.lang.GMap;

/**
 * Vector schematics
 * 
 * @author cyberpwn
 */
public class VectorSchematic
{
	private final GMap<Vector, VariableBlock> schematic;
	
	/**
	 * Create a vector schematic
	 */
	public VectorSchematic()
	{
		this.schematic = new GMap<Vector, VariableBlock>();
	}
	
	/**
	 * Does the vector schematic's variable blocks contain the given material
	 * block?
	 * 
	 * @param mb
	 *            the material block
	 * @return true if it does
	 */
	public boolean contains(MaterialBlock mb)
	{
		for(Vector i : schematic.k())
		{
			if(schematic.get(i).is(mb))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Does this vector schematic contain multiple of the given material blocks
	 * 
	 * @param mb
	 *            the materialblock
	 * @return true if it does
	 */
	public boolean containsMultiple(MaterialBlock mb)
	{
		return find(mb).size() > 1;
	}
	
	/**
	 * Find all vector references that match the given material block
	 * 
	 * @param mb
	 *            the materialblock
	 * @return the vector matches
	 */
	public GList<Vector> find(MaterialBlock mb)
	{
		GList<Vector> vectors = new GList<Vector>();
		
		for(Vector i : schematic.k())
		{
			if(schematic.get(i).is(mb))
			{
				vectors.add(i);
			}
		}
		
		return vectors;
	}
	
	/**
	 * Match the location as part of a multiblock structure
	 * 
	 * @param location
	 *            the location
	 * @return the mapping or null if no match
	 */
	public GMap<Vector, Location> match(Location location)
	{
		MaterialBlock mb = new MaterialBlock(location);
		
		for(Vector i : find(mb))
		{
			Vector shift = i;
			Location base = location.clone().subtract(shift);
			GMap<Vector, Location> map = new GMap<Vector, Location>();
			Boolean found = true;
			
			for(Vector j : schematic.k())
			{
				Location attempt = base.clone().add(j);
				MaterialBlock mbx = new MaterialBlock(attempt);
				
				if(schematic.get(j).is(mbx))
				{
					map.put(j, attempt);
				}
				
				else
				{
					found = false;
					break;
				}
			}
			
			if(found)
			{
				return map;
			}
		}
		
		return null;
	}
	
	/**
	 * Get the schematic
	 * 
	 * @return the schematic
	 */
	public GMap<Vector, VariableBlock> getSchematic()
	{
		return schematic;
	}
}