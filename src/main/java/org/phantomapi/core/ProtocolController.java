package org.phantomapi.core;

import java.lang.reflect.InvocationTargetException;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.phantomapi.async.Callback;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;
import org.phantomapi.event.EquipmentUpdateEvent;
import org.phantomapi.event.PacketCustomSoundEvent;
import org.phantomapi.event.PacketNamedSoundEvent;
import org.phantomapi.ext.Protocol;
import org.phantomapi.lang.GList;
import org.phantomapi.lang.GMap;
import org.phantomapi.nms.EntityHider;
import org.phantomapi.nms.EntityHider.Policy;
import org.phantomapi.nms.FakeEquipment;
import org.phantomapi.nms.NMSX;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.util.C;
import org.phantomapi.util.Depend;
import org.phantomapi.util.PRO;
import org.phantomapi.util.Timer;
import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;

public class ProtocolController extends Controller
{
	private GMap<Player, Callback<GList<String>>> signListen;
	private EntityHider entityHider;
	private FakeEquipment fakeEquipment;
	private GMap<Player, Double> realPing;
	private GMap<Player, Timer> timers;
	private GMap<Integer, Player> waiting;
	private GMap<Player, GList<Double>> pingHistory;
	private GMap<Sound, String> id;
	
	public ProtocolController(Controllable parentController)
	{
		super(parentController);
		
		realPing = new GMap<Player, Double>();
		timers = new GMap<Player, Timer>();
		waiting = new GMap<Integer, Player>();
		pingHistory = new GMap<Player, GList<Double>>();
		id = new GMap<Sound, String>();
		signListen = new GMap<Player, Callback<GList<String>>>();
	}
	
	public void listenSign(Player player, Callback<GList<String>> callback, GList<String> initialText, String guide)
	{
		PacketContainer p2 = PRO.getLibrary().createPacket(PacketType.Play.Server.OPEN_SIGN_EDITOR);
		p2.getBlockPositionModifier().write(0, new BlockPosition(player.getLocation().toVector()));
		
		try
		{
			PRO.getLibrary().sendServerPacket(player, p2);
			signListen.put(player, callback);
			NMSX.sendActionBar(player, C.GREEN + guide);
		}
		
		catch(InvocationTargetException e)
		{
			e.printStackTrace();
		}
	}
	
	@Override
	public void onStart()
	{
		if(!Depend.PROTOLIB.exists())
		{
			return;
		}
		
		entityHider = new EntityHider(getPlugin(), Policy.BLACKLIST);
		
		fakeEquipment = new FakeEquipment(getPlugin())
		{
			@Override
			protected boolean onEquipmentSending(EquipmentSendingEvent e)
			{
				EquipmentUpdateEvent ee = new EquipmentUpdateEvent(e.getVisibleEntity(), e.getEquipment(), e.getClient(), e.getSlot());
				callEvent(ee);
				
				if(ee.isCancelled())
				{
					return false;
				}
				
				e.setEquipment(ee.getItem());
				e.setSlot(ee.getSlot());
				
				return true;
			}
		};
		
		ProtocolManager mgr = ProtocolLibrary.getProtocolManager();
		
		mgr.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Client.UPDATE_SIGN)
		{
			@Override
			public void onPacketReceiving(PacketEvent event)
			{
				if(event.getPacketType().equals(PacketType.Play.Client.UPDATE_SIGN))
				{
					if(signListen.containsKey(event.getPlayer()))
					{
						PacketContainer p = event.getPacket();
						GList<String> text = new GList<String>();
						text.add(p.getStringArrays().read(0));
						signListen.get(event.getPlayer()).run(text.copy());
						signListen.remove(event.getPlayer());
					}
				}
			}
		});
		
		mgr.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Server.NAMED_SOUND_EFFECT)
		{
			@Override
			public void onPacketSending(PacketEvent event)
			{
				if(event.getPacketType().equals(PacketType.Play.Server.NAMED_SOUND_EFFECT))
				{
					try
					{
						PacketContainer p = event.getPacket();
						
						Sound sound = p.getSoundEffects().read(0);
						Player player = event.getPlayer();
						World world = player.getWorld();
						Double x = (double) p.getIntegers().read(0) / 8.0;
						Double y = (double) p.getIntegers().read(1) / 8.0;
						Double z = (double) p.getIntegers().read(2) / 8.0;
						Float volume = p.getFloat().read(0);
						Float pitch = (float) p.getIntegers().read(3) / 63f;
						Location l = new Location(world, x, y, z);
						
						PacketNamedSoundEvent e = new PacketNamedSoundEvent(player, sound, l, volume, pitch);
						callEvent(e);
						
						p.getSoundEffects().write(0, e.getSound());
						p.getIntegers().write(0, (int) e.getLocation().getX() * 8);
						p.getIntegers().write(1, (int) e.getLocation().getY() * 8);
						p.getIntegers().write(2, (int) e.getLocation().getZ() * 8);
						p.getFloat().write(0, e.getVolume());
						p.getIntegers().write(3, (int) (e.getPitch() * 63f));
						
						if(e.isCancelled())
						{
							event.setCancelled(true);
						}
					}
					
					catch(Exception e)
					{
						
					}
				}
			}
		});
		
		mgr.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Server.CUSTOM_SOUND_EFFECT)
		{
			@Override
			public void onPacketSending(PacketEvent event)
			{
				if(event.getPacketType().equals(PacketType.Play.Server.CUSTOM_SOUND_EFFECT))
				{
					try
					{
						PacketContainer p = event.getPacket();
						
						String soundstr = p.getStrings().read(0);
						Player player = event.getPlayer();
						World world = player.getWorld();
						Double x = (double) p.getIntegers().read(0) / 8.0;
						Double y = (double) p.getIntegers().read(1) / 8.0;
						Double z = (double) p.getIntegers().read(2) / 8.0;
						Float volume = p.getFloat().read(0);
						Float pitch = (float) p.getIntegers().read(3) / 63f;
						Location l = new Location(world, x, y, z);
						Sound sound = null;
						
						try
						{
							sound = Sound.valueOf(soundstr.replaceAll(".", "_").toUpperCase());
						}
						
						catch(Exception e)
						{
							
						}
						
						if(sound != null)
						{
							PacketNamedSoundEvent e = new PacketNamedSoundEvent(player, sound, l, volume, pitch);
							callEvent(e);
							
							String s = id.get(e.getSound());
							
							p.getStrings().write(0, s != null ? s : soundstr);
							p.getIntegers().write(0, (int) e.getLocation().getX() * 8);
							p.getIntegers().write(1, (int) e.getLocation().getY() * 8);
							p.getIntegers().write(2, (int) e.getLocation().getZ() * 8);
							p.getFloat().write(0, e.getVolume());
							p.getIntegers().write(3, (int) (e.getPitch() * 63f));
							
							if(e.isCancelled())
							{
								event.setCancelled(true);
							}
						}
						
						else
						{
							PacketCustomSoundEvent e = new PacketCustomSoundEvent(player, soundstr, l, volume, pitch);
							callEvent(e);
							
							p.getStrings().write(0, e.getSound());
							p.getIntegers().write(0, (int) e.getLocation().getX() * 8);
							p.getIntegers().write(1, (int) e.getLocation().getY() * 8);
							p.getIntegers().write(2, (int) e.getLocation().getZ() * 8);
							p.getFloat().write(0, e.getVolume());
							p.getIntegers().write(3, (int) (e.getPitch() * 63f));
							
							if(e.isCancelled())
							{
								event.setCancelled(true);
							}
						}
					}
					
					catch(Exception e)
					{
						
					}
				}
			}
		});
		
		mgr.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Server.KEEP_ALIVE)
		{
			@Override
			public void onPacketSending(PacketEvent event)
			{
				if(event.getPacketType().equals(PacketType.Play.Server.KEEP_ALIVE))
				{
					int id = event.getPacket().getIntegers().read(0);
					Player player = event.getPlayer();
					Timer t = new Timer();
					t.start();
					waiting.put(id, player);
					timers.put(player, t);
				}
			}
		});
		
		mgr.addPacketListener(new PacketAdapter(getPlugin(), ListenerPriority.HIGHEST, PacketType.Play.Client.KEEP_ALIVE)
		{
			@Override
			public void onPacketReceiving(PacketEvent event)
			{
				if(event.getPacketType().equals(PacketType.Play.Client.KEEP_ALIVE))
				{
					try
					{
						int id = event.getPacket().getIntegers().read(0);
						Player player = event.getPlayer();
						
						if(waiting.containsKey(id) && waiting.get(id).equals(player))
						{
							waiting.remove(id);
							Timer t = timers.get(player);
							t.stop();
							timers.remove(player);
							realPing.put(player, (double) t.getTime());
							
							if(!pingHistory.containsKey(player))
							{
								pingHistory.put(player, new GList<Double>());
							}
							
							pingHistory.get(player).add(realPing.get(player));
							
							while(pingHistory.get(player).size() > 128)
							{
								pingHistory.get(player).pop();
							}
						}
					}
					
					catch(Exception e)
					{
						
					}
				}
			}
		});
		
		String sounds = "ambient.cave::block.anvil.break::block.anvil.destroy::block.anvil.fall::block.anvil.hit::block.anvil.land::block.anvil.place::block.anvil.step::block.anvil.use::block.brewing_stand.brew::block.chest.close::block.chest.locked::block.chest.open::block.chorus_flower.death::block.chorus_flower.grow::block.cloth.break::block.cloth.fall::block.cloth.hit::block.cloth.place::block.cloth.step::block.comparator.click::block.dispenser.dispense::block.dispenser.fail::block.dispenser.launch::block.end_gateway.spawn::block.enderchest.close::block.enderchest.open::block.fence_gate.close::block.fence_gate.open::block.fire.ambient::block.fire.extinguish::block.furnace.fire_crackle::block.glass.break::block.glass.fall::block.glass.hit::block.glass.place::block.glass.step::block.grass.break::block.grass.fall::block.grass.hit::block.grass.place::block.grass.step::block.gravel.break::block.gravel.fall::block.gravel.hit::block.gravel.place::block.gravel.step::block.iron_door.close::block.iron_door.open::block.iron_trapdoor.open::block.ladder_break::block.ladder.fall::block.ladder.hit::block.ladder.place::block.ladder.step::block.lava.ambient::block.lava.extinguish::block.lava.pop::block.lever.click::block.metal_pressureplate.click_off::block.metal_pressureplate.click_on::block.metal.break::block.metal.fall::block.metal.hit::block.metal.place::block.metal.step::block.note.basedrum::block.note.bass::block.note.harp::block.note.hat::block.note.pling::block.note.snare::block.piston.contract::block.piston.extend::block.portal.ambient::block.portal.travel::block.portal.trigger::block.redstone_torch.burnout::block.sand.break::block.sand.fall::block.sand.hit::block.sand.place::block.sand.step::block.slime.break::block.slime.place::block.slime_step::block.slime.break::block.slime.fall::block.slime.hit::block.slime.place::block.snow.break::block.snow.fall::block.snow.hit::block.snow.place::block.snow.step::block.stone_button.click_off::block.stone_button.click_on::block.stone_pressureplate.click_off::block.stone_pressureplate.click_on::block.stone.break::block.stone.fall::block.stone.hit::block.stone.place::block.stone.step::block.tripwire.attach::block.tripwire.click_off::block.tripwire.click_on::block.tripwire.detach::block.water.ambient::block.waterlily.place::block.wood_button.click_off::block.wood_button.click_on::block.wood_pressureplate.click_off::block.wood_pressureplate.click_on::block.wood.break::block.wood.fall::block.wood.hit::block.wood.place::block.wood.step::block.wooden_door.close::block.wooden_door.open::block.wooden_trapdoor.close::block.wooden_trapdoor.open::block.wooden_trapdoor.open::enchant.thorns.hit::entity.armorstand.break::entity.armorstand.hit::entity.arrow.hit::entity.arrow.hit_player::entity.arrow.shoot::entity.attack.strong::entity.bat.ambient::entity.bat.death::entity.bat.hurt::entity.bat.loop::entity.bat.takeoff::entity.blaze.ambient::entity.blaze.burn::entity.blaze.death::entity.blaze.hurt::entity.blaze.shoot::entity.bobber.splash::entity.bobber.throw::entity.cat.ambient::entity.cat.death::entity.cat.hurt::entity.cat.purr::entity.cat.purreow::entity.chicken.ambient::entity.chicken.death::entity.chicken.egg::entity.chicken.hurt::entity.chicken.step::entity.cow.ambient::entity.cow.death::entity.cow.hurt::entity.cow.step::entity.creeper.death::entity.creeper.hurt::entity.creeper.primed::entity.donkey.ambient::entity.donkey.angry::entity.donkey.chest::entity.donkey.death::entity.donkey.hurt::entity.egg.throw::entity.elder_guardian.ambient::entity.elder_guardian.ambient_land::entity.elder_guardian.curse::entity.elder_guardian.death::entity.elder_guardian.death_land::entity.elder_guardian.hurt::entity.elder_guardian.hurt_land::entity.enderdragon_fireball.explode::entity.enderdragon.ambient::entity.enderdragon.death::entity.enderdragon.flap::entity.enderdragon.growl::entity.enderdragon.hurt::entity.enderdragon.shoot::entity.endereye.launch::entity.endermen.ambient::entity.endermen.death::entity.endermen.hurt::entity.endermen.scream::entity.endermen.stare::entity.endermen.teleport::entity.enderpearl.throw::entity.endermite.ambient::entity.endermite.death::entity.endermite.hurt::entity.endermite.step::entity.experience_bottle.throw::entity.experience_orb.pickup::entity.experience_orb.touch::entity.firework.blast::entity.firework.blast_far::entity.firework.large_blast::entity.firework.large_blast_far::entity.firework.launch::entity.firework.shoot::entity.firework.twinkle::entity.firework.twinkle_far::entity.generic.big_fall::entity.generic.burn::entity.generic.death::entity.generic.drink::entity.generic.eat::entity.generic.explode::entity.generic.extinguish_fire::entity.generic.hurt::entity.generic.small_fall::entity.generic.splash::entity.generic.swim::entity.ghast.ambient::entity.ghast.death::entity.ghast.hurt::entity.ghast.scream::entity.ghast.shoot::entity.ghast.warn::entity.guardian.ambient::entity.guardian.ambient_land::entity.guardian.attack::entity.guardian.death::entity.guardian.death_land::entity.guardian.flop::entity.guardian.hurt::entity.guardian.hurt_land::entity.horse.ambient::entity.horse.angry::entity.horse.armor::entity.horse.breathe::entity.horse.death::entity.horse.eat::entity.horse.gallop::entity.horse.hurt::entity.horse.jump::entity.horse.land::entity.horse.saddle::entity.horse.step::entity.horse.step_wood::entity.hostile.big_fall::entity.hostile.death::entity.hostile.hurt::entity.hostile.small_fall::entity.hostile.splash::entity.hostile.swim::entity.irongolem.attack::entity.irongolem.death::entity.irongolem.hurt::entity.irongolem.step::entity.item.break::entity.item.pickup::entity.itemframe.break::entity.itemframe.rotate_item::entity.leashknot.break::entity.lightning.impact::entity.lightning.thunder::entity.lingeringpotion.throw::entity.magmacube.death::entity.magmacube.hurt::entity.magmacube.jump::entity.magmacube.squish::entity.minecart.inside::entity.minecart.riding::entity.mooshroom.shear::entity.mule.ambient::entity.mule.death::entity.mule.hurt::entity.pig.ambient::entity.pig.death::entity.pig.hurt::entity.pig.saddle::entity.pig.step::entity.player.attack.crit::entity.player.attack.knockback::entity.player.attack.nodamage::entity.player.attack.sweep::entity.player.big_fall::entity.player.breath::entity.player.death::entity.player.hurt::entity.player.levelup::entity.player.small_fall::entity.player.splash::entity.player.swim::entity.rabbit.ambient::entity.rabbit.attack::entity.rabbit.death::entity.rabbit.hurt::entity.rabbit.jump::entity.sheep.ambient::entity.sheep.death::entity.sheep.hurt::entity.sheep.shear::entity.sheep.step::entity.shulker_bullet.hit::entity.shulker_bullet.hurt::entity.shulker.ambient::entity.shulker.close::entity.shulker.death::entity.shulker.hit::entity.shulker.hurt::entity.shulker.open::entity.shulker.shoot::entity.shulker.teleport::entity.silverfish.ambient::entity.silverfish.death::entity.silverfish.hurt::entity.silverfish.step::entity.skeleton_horse.ambient::entity.skeleton_horse.death::entity.skeleton_horse.hurt::entity.skeleton.ambient::entity.skeleton.death::entity.skeleton.hurt::entity.skeleton.shoot::entity.skeleton.step::entity.slime.attack::entity.slime.death::entity.slime.hurt::entity.slime.jump::entity.slime.squish::entity.small_magmacube.death::entity.small_magmacube.hurt::entity.small_magmacube.squish::entity.small_slime.death::entity.small_slime.hurt::entity.small_slime.jump::entity.small_slime.squish::entity.snowball.throw::entity.snowman.ambient::entity.snowman.death::entity.snowman.hurt::entity.snowman.shoot::entity.spider.ambient::entity.spider.death::entity.spider.hurt::entity.spider.step::entity.splash_potion.break::entity.splash_potion.throw::entity.squid.ambient::entity.squid.death::entity.squid.hurt::entity.tnt.primed::entity.villager.ambient::entity.villager.death::entity.villager.hurt::entity.villager.no::entity.villager.trading::entity.villager.yes::entity.witch.ambient::entity.witch.death::entity.witch.drink::entity.witch.hurt::entity.witch.throw::entity.wither.ambient::entity.wither.break_block::entity.wither.death::entity.wither.hurt::entity.wither.shoot::entity.wither.spawn::entity.wolf.ambient::entity.wolf.death::entity.wolf.growl::entity.wolf.howl::entity.wolf.hurt::entity.wolf.pant::entity.wolf.shake::entity.wolf.step::entity.wolf.whine::entity.zombie_horse.ambient::entity.zombie_horse.death::entity.zombie_horse.hurt::entity.zombie_pig.ambient::entity.zombie_pig.angry::entity.zombie_pig.death::entity.zombie_pig.hurt::entity.zombie_villager.ambient::entity.zombie_villager.converted::entity.zombie_villager.cure::entity.zombie_villager.death::entity.zombie_villager.hurt::entity.zombie_villager.step::entity.zombie.ambient::entity.zombie.attack_door_wood::entity.zombie.attack_iron_door::entity.zombie.break_door_wood::entity.zombie.death::entity.zombie.hurt::entity.zombie.infect::entity.zombie.step::item.armor.equip_chain::item.armor.equip_diamond::item.armor.equip_generic::item.armor.equip_gold::item.armor.equip_iron::item.armor.equip_leather::item.bottle.fill::item.bottle.fill_dragonbreath::item.bucket.empty::item.bucket.empty_lava::item.bucket.fill::item.bucket.fill_lava::item.chorus_fruit.teleport::item.elytra.flying::item.firecharge.use::item.flintandsteel.use::item.hoe.till::item.shield.block::item.shield.break::item.shovel.flatten::music.creative::music.credits::music.dragon::music.end::music.game::music.menu::music.nether::piston.contract::record.11::record.13::record.blocks::record.cat::record.chirp::record.far::record.mall::record.mellohi::record.stal::record.strad::record.wait::record.ward::ui.button.click::weather.rain::weather.rain.above";
		
		for(String i : sounds.split("::"))
		{
			try
			{
				Sound s = Sound.valueOf(i.replaceAll("\\.", "_").toUpperCase());
				id.put(s, i);
			}
			
			catch(Exception e)
			{
				
			}
		}
	}
	
	public EntityHider getHider()
	{
		return entityHider;
	}
	
	@Override
	public void onStop()
	{
		
	}
	
	@EventHandler
	public void on(PlayerJoinEvent e)
	{
		if(!Depend.PROTOLIB.exists())
		{
			return;
		}
		
		new TaskLater()
		{
			@Override
			public void run()
			{
				w(e.getPlayer().getName() + " <> Protocol " + Protocol.getProtocol(e.getPlayer()));
			}
		};
	}
	
	public FakeEquipment getFakeEquipment()
	{
		return fakeEquipment;
	}
	
	public EntityHider getEntityHider()
	{
		return entityHider;
	}
	
	public GMap<Player, Double> getRealPing()
	{
		return realPing;
	}
	
	public GMap<Player, Timer> getTimers()
	{
		return timers;
	}
	
	public GMap<Integer, Player> getWaiting()
	{
		return waiting;
	}
	
	public GMap<Player, GList<Double>> getPingHistory()
	{
		return pingHistory;
	}
	
	public GList<Double> getPingHistory(Player player)
	{
		return getPingHistory().get(player);
	}
	
	public Double getPing(Player player)
	{
		return getRealPing().get(player) / 1000000.0;
	}
	
	public long getPingNanos(Player player)
	{
		return getRealPing().get(player).longValue();
	}
}
