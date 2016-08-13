package org.phantomapi;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.phantomapi.clust.DataCluster;
import org.phantomapi.clust.YAMLDataInput;
import org.phantomapi.clust.YAMLDataOutput;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;
import org.phantomapi.gui.Click;
import org.phantomapi.gui.Dialog;
import org.phantomapi.gui.Notification;
import org.phantomapi.gui.PhantomDialog;
import org.phantomapi.gui.PhantomElement;
import org.phantomapi.gui.PhantomWindow;
import org.phantomapi.gui.Slot;
import org.phantomapi.gui.Window;
import org.phantomapi.lang.GList;
import org.phantomapi.lang.GMap;
import org.phantomapi.lang.Priority;
import org.phantomapi.lang.Title;
import org.phantomapi.nms.NMSX;
import org.phantomapi.sync.ExecutiveIterator;
import org.phantomapi.util.C;
import org.phantomapi.util.Formula;
import org.phantomapi.vfx.ParticleEffect;
import org.phantomapi.vfx.PhantomEffect;
import org.phantomapi.vfx.SphereParticleManipulator;
import org.phantomapi.vfx.VisualEffect;
import org.phantomapi.world.Artifact;
import org.phantomapi.world.Dimension;
import org.phantomapi.world.Direction;
import org.phantomapi.world.EdgeDistortion;
import org.phantomapi.world.MaterialBlock;
import org.phantomapi.world.Schematic;
import org.phantomapi.world.WorldArtifact;
import org.phantomapi.world.WorldStructure;

/**
 * Runs tests on various functions of phantom
 * 
 * @author cyberpwn
 */
public class TestController extends Controller
{
	private GMap<String, Runnable> tests;
	
	public TestController(Controllable parentController)
	{
		super(parentController);
		
		tests = new GMap<String, Runnable>();
		
		tests.put("channel-pool", new Runnable()
		{
			@Override
			public void run()
			{
				GList<String> k = new GList<String>();
				GList<String> v = new GList<String>();
				
				for(int i = 0; i < 10240008 * Math.random(); i++)
				{
					k.add(UUID.randomUUID().toString());
				}
				
				v.qadd("alpha").qadd("beta").qadd("charlie").qadd("delta");
				
				Phantom.schedule(new ExecutiveIterator<String>(k.copy())
				{
					public void onIterate(String next)
					{
						while(Math.random() < 0.6)
						{
							UUID.randomUUID();
							Math.sqrt(Math.sqrt(Math.random()));
						}
						
						if(Math.random() < 0.001)
						{
							Phantom.schedule(v.pickRandom(), new ExecutiveIterator<String>(v)
							{
								@Override
								public void onIterate(String next)
								{
									
								}
							});
						}
					}
				});
			}
		});
		
		tests.put("cluster-write", new Runnable()
		{
			@Override
			public void run()
			{
				DataCluster dc = new DataCluster();
				dc.set("test.string", "Stringd");
				dc.set("test.double", -0.65743345);
				dc.set("test.int", 234);
				dc.set("test.list", new GList<String>().qadd("test").qadd("list").qadd("test"));
				dc.set("test.boolean", true);
				
				try
				{
					new YAMLDataOutput().save(dc, new File(getPlugin().getDataFolder(), "cluster-test.yml"));
				}
				
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		tests.put("cluster-overwrite", new Runnable()
		{
			@Override
			public void run()
			{
				DataCluster dc = new DataCluster();
				dc.set("test.overwrite.data", "New Data");
				dc.set("test.string", "New String Text (Overwrite)");
				
				try
				{
					new YAMLDataOutput().save(dc, new File(getPlugin().getDataFolder(), "cluster-test.yml"));
				}
				
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		});
		
		tests.put("credits", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					NMSX.showEnd(i);
				}
			}
		});
		
		tests.put("title", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					Title t = new Title(C.RED + "TITLE", C.GREEN + "SUBTITLE", C.BLUE + "ACTION", 5, 30, 5);
					t.send(i);
				}
			}
		});
		
		tests.put("notification", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					Title t = new Title(C.RED + "TITLE", C.GREEN + "SUBTITLE", C.BLUE + "ACTION", 5, 30, 5);
					Notification n = new Notification(t, Priority.HIGH);
					Phantom.queueNotification(i, n);
				}
			}
		});
		
		tests.put("formula", new Runnable()
		{
			@Override
			public void run()
			{
				Formula f = new Formula("Math.sin($0) / Math.cos($1)");
				
				for(int i = 0; i < 4; i++)
				{
					double d = Math.random();
					double k = Math.random();
					
					try
					{
						s("Math.sin(" + d + ") / Math.cos(" + k + ") = " + f.evaluate(d, k));
					}
					
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		});
		
		tests.put("notification-multiple", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					Title t = new Title(C.RED + "TITLE", C.GREEN + "SUBTITLE", C.BLUE + "ACTION", 5, 10, 5);
					Notification n = new Notification(t, Priority.HIGH);
					Phantom.queueNotification(i, n);
					
					Title tt = new Title(C.GREEN + "TITLE 2", C.RED + "SUBTITLE 2", C.BLUE + "ACTION 2", 5, 100, 5);
					Notification nt = new Notification(tt, Priority.HIGH);
					Phantom.queueNotification(i, nt);
					
					Title ttt = new Title(C.RED + "TITLE 3", C.BLUE + "SUBTITLE 3", C.BLUE + "ACTION 3", 5, 30, 5);
					Notification ntt = new Notification(ttt, Priority.HIGH);
					Phantom.queueNotification(i, ntt);
				}
			}
		});
		
		tests.put("particle-sphere", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					VisualEffect e = new PhantomEffect()
					{
						@Override
						public void play(Location l)
						{
							ParticleEffect.FIREWORKS_SPARK.display(0, 1, l, 32);
						}
					};
					
					SphereParticleManipulator s = new SphereParticleManipulator()
					{
						@Override
						public void play(Location l)
						{
							e.play(l);
						}
					};
					
					VisualEffect ex = new PhantomEffect()
					{
						@Override
						public void play(Location l)
						{
							s.play(l, 1 + 5.0, 300.0);
						}
					};
					
					ex.play(i.getLocation());
				}
			}
		});
		
		tests.put("artifact", new Runnable()
		{
			
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					Schematic s = new Schematic(new Dimension(9, 9, 9));
					s.fill(new MaterialBlock(Material.AIR));
					s.setFaces(new MaterialBlock(Material.THIN_GLASS));
					s.setFace(new MaterialBlock(Material.ENDER_PORTAL), Direction.U);
					s.setFace(new MaterialBlock(Material.GLASS), Direction.D);
					s.distort(new EdgeDistortion(new MaterialBlock(Material.GLOWSTONE)));
					
					Schematic s2 = new Schematic(new Dimension(3, 9, 3));
					s2.fill(new MaterialBlock(Material.AIR));
					s2.setFaces(new MaterialBlock(Material.THIN_GLASS));
					s2.setFace(new MaterialBlock(Material.ENDER_PORTAL), Direction.U);
					s2.setFace(new MaterialBlock(Material.GLASS), Direction.D);
					s2.distort(new EdgeDistortion(new MaterialBlock(Material.GLOWSTONE)));
					
					Artifact a = new WorldArtifact(i.getLocation().add(-3, 6, -3), s);
					Artifact a1 = new WorldArtifact(i.getLocation().add(12, 6, -12), s2);
					Artifact a2 = new WorldArtifact(i.getLocation().add(12, 6, 12), s2);
					Artifact a3 = new WorldArtifact(i.getLocation().add(-12, 6, -12), s2);
					Artifact a4 = new WorldArtifact(i.getLocation().add(-12, 6, 12), s2);
					
					Artifact structure = new WorldStructure(i.getLocation().add(0, 6, 0));
					((WorldStructure) structure).add(a, new Vector(0, 0, 0));
					((WorldStructure) structure).add(a1, new Vector(12, 0, -12));
					((WorldStructure) structure).add(a2, new Vector(12, 0, 12));
					((WorldStructure) structure).add(a3, new Vector(-12, 0, -12));
					((WorldStructure) structure).add(a4, new Vector(-12, 0, 12));
					
					structure.build();
				}
			}
		});
		
		tests.put("gui", new Runnable()
		{
			@Override
			public void run()
			{
				for(Player i : Phantom.instance().onlinePlayers())
				{
					Window test = new PhantomWindow("Test", i);
					Window main = new PhantomWindow(C.AQUA + "Main", i);
					Window dialog = new PhantomDialog(ChatColor.DARK_RED + "Are you SURE?", i, true)
					{
						public void onCancelled(Player p, Window w, Dialog d)
						{
							test.open();
						}
					};
					
					dialog.addElement(new PhantomElement(Material.SLIME_BALL, new Slot(0, 2), C.GREEN + "YES")
					{
						public void onClick(Player p, Click c, Window w)
						{
							main.open();
						}
					});
					
					dialog.addElement(new PhantomElement(Material.BARRIER, new Slot(0, 3), C.RED + "NO")
					{
						public void onClick(Player p, Click c, Window w)
						{
							test.open();
						}
					});
					
					main.addElement(new PhantomElement(Material.SLIME_BALL, new Slot(0, 2), C.RED + "Colored")
					{
						public void onClick(Player p, Click c, Window w)
						{
							test.open();
						}
					});
					
					test.addElement(new PhantomElement(Material.BARRIER, new Slot(-1, 3), C.RED + "Close")
					{
						public void onClick(Player p, Click c, Window w)
						{
							test.close();
						}
					});
					
					test.addElement(new PhantomElement(Material.CARROT_STICK, new Slot(0, 3), C.RED + "Do something")
					{
						public void onClick(Player p, Click c, Window w)
						{
							dialog.open();
						}
					});
					
					test.addElement(new PhantomElement(Material.MAGMA_CREAM, new Slot(1, 3), C.RED + "Back")
					{
						public void onClick(Player p, Click c, Window w)
						{
							main.open();
						}
					});
					
					main.setBackground(new PhantomElement(Material.STAINED_GLASS_PANE, (byte) 1, new Slot(0, 1), " ")
					{
						public void onClick(Player p, Click c, Window w)
						{
							
						}
					});
					test.setBackground(new PhantomElement(Material.STAINED_GLASS_PANE, (byte) 2, new Slot(0, 1), " ")
					{
						public void onClick(Player p, Click c, Window w)
						{
							
						}
					});
					dialog.setBackground(new PhantomElement(Material.STAINED_GLASS_PANE, (byte) 3, new Slot(0, 1), " ")
					{
						public void onClick(Player p, Click c, Window w)
						{
							
						}
					});
					
					main.open();
				}
			}
		});
		
		tests.put("cluster-read", new Runnable()
		{
			@Override
			public void run()
			{
				DataCluster dc = new DataCluster();
				
				try
				{
					new YAMLDataInput().load(dc, new File(getPlugin().getDataFolder(), "cluster-test.yml"));
					
					for(String i : dc.getData().keySet())
					{
						System.out.println(i + ": " + dc.getAbstract(i));
					}
				}
				
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
		});
	}
	
	public void execute(final CommandSender sender, String test)
	{
		for(String i : tests.k())
		{
			if(i.toLowerCase().contains(test.toLowerCase()))
			{
				sender.sendMessage(ChatColor.GREEN + "Running Test " + i);
				tests.get(i).run();
				break;
			}
		}
	}
	
	public GMap<String, Runnable> getTests()
	{
		return tests;
	}
	
	@Override
	public void onStart()
	{
		
	}

	@Override
	public void onStop()
	{
		
	}
}