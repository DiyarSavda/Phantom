package org.phantomapi.physics;

import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * Vector utilities
 * 
 * @author cyberpwn
 *
 */
public class VectorMath
{
	/**
	 * Get a normalized vector going from a location to another
	 * 
	 * @param from
	 *            from here
	 * @param to
	 *            to here
	 * @return the normalized vector direction
	 */
	public static Vector direction(Location from, Location to)
	{
		return to.subtract(from).toVector().normalize();
	}
	
	/**
	 * Reverse a direction
	 * 
	 * @param v
	 *            the direction
	 * @return the reversed direction
	 */
	public static Vector reverse(Vector v)
	{
		v.setX(-v.getX());
		v.setY(-v.getY());
		v.setZ(-v.getZ());
		return v;
	}
	
	/**
	 * Get a speed value from a vector (velocity)
	 * 
	 * @param v
	 *            the vector
	 * @return the speed
	 */
	public static double getSpeed(Vector v)
	{
		Vector vi = new Vector(0, 0, 0);
		Vector vt = new Vector(0, 0, 0).add(v);
		
		return vi.distance(vt);
	}
}