package org.cyberpwn.phantom.sfx;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.cyberpwn.phantom.util.M;

public class DopplerAudibleEntity extends AudibleEntity
{
	public DopplerAudibleEntity(Entity entity, Audible audible, Integer interval)
	{
		super(entity, audible, interval);
	}
	
	protected void onPlay(Player i, Audible audible, Entity entity)
	{
		Vector pv = i.getVelocity();
		Vector ev = entity.getVelocity();
		Double speedDifference = M.getSpeed(pv.subtract(ev)) * (entity.getLocation().distance(i.getLocation()));
		audible.setPitch((float) ((float) (audible.getPitch() / speedDifference) * 10));
		audible.play(i, entity.getLocation());
		System.out.println("Speed: " + speedDifference + " Pitch: " + audible.getPitch());
	}
}
