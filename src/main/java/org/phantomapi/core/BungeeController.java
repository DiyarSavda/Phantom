package org.phantomapi.core;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.phantomapi.Phantom;
import org.phantomapi.clust.DataCluster;
import org.phantomapi.clust.JSONDataInput;
import org.phantomapi.clust.JSONDataOutput;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;
import org.phantomapi.construct.Ticked;
import org.phantomapi.event.BungeeConnectionEstablished;
import org.phantomapi.lang.GList;
import org.phantomapi.network.ForwardedPluginMessage;
import org.phantomapi.network.Network;
import org.phantomapi.network.NetworkedServer;
import org.phantomapi.network.PhantomNetwork;
import org.phantomapi.network.PluginMessage;
import org.phantomapi.statistics.Monitorable;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.transmit.Transmission;
import org.phantomapi.transmit.Transmitter;
import org.phantomapi.util.C;
import org.phantomapi.util.ExceptionUtil;
import org.phantomapi.util.Refreshable;
import org.phantomapi.util.Timer;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

@Ticked(100)
public class BungeeController extends Controller implements PluginMessageListener, Monitorable, Transmitter
{
	private DataCluster cc;
	private GList<Transmitter> transmitters;
	private GList<Transmission> queue;
	private GList<Transmission> responders;
	private Boolean connected;
	private String sname;
	private Network network;
	private Integer ti;
	private Integer to;
	private Timer t;
	public static long linkSpeed = -1;
	private GList<String> server;
	
	public BungeeController(Controllable parentController)
	{
		super(parentController);
		
		cc = new DataCluster();
		responders = new GList<Transmission>();
		transmitters = new GList<Transmitter>();
		connected = false;
		sname = null;
		queue = new GList<Transmission>();
		network = new PhantomNetwork();
		server = new GList<String>();
		ti = 0;
		to = 0;
		t = new Timer();
		
		getPlugin().getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(), "BungeeCord");
		getPlugin().getServer().getMessenger().registerIncomingPluginChannel(getPlugin(), "BungeeCord", this);
	}
	
	public void registerTransmitter(Transmitter t)
	{
		transmitters.add(t);
	}
	
	public void unregisterTransmitter(Transmitter t)
	{
		transmitters.remove(t);
	}
	
	@Override
	public void onTick()
	{
		new TaskLater((int) (Math.random() * 10))
		{
			@Override
			public void run()
			{
				hit();
			}
		};
	}
	
	@EventHandler
	public void on(PlayerJoinEvent e)
	{
		if(Phantom.getServerName() == null)
		{
			hit();
		}
	}
	
	public void hit()
	{
		((Refreshable) network).refresh();
		new PluginMessage(getPlugin(), "GetServers").send();
		
		if(cc.contains("servers"))
		{
			for(String i : cc.getStringList("servers"))
			{
				new PluginMessage(getPlugin(), "PlayerCount", i).send();
				new PluginMessage(getPlugin(), "PlayerList", i).send();
			}
			
			new PluginMessage(getPlugin(), "PlayerCount", "ALL").send();
			new PluginMessage(getPlugin(), "PlayerList", "ALL").send();
			new PluginMessage(getPlugin(), "GetServer").send();
			t.start();
		}
		
		GList<Transmission> qq = queue.copy();
		queue.clear();
		
		for(Transmission i : qq)
		{
			try
			{
				transmit(i);
			}
			
			catch(IOException e)
			{
				e.printStackTrace();
			}
		}
		
		for(NetworkedServer i : Phantom.instance().getNetwork().getServers())
		{
			if(!cc.contains("server." + i.getName() + ".count") || cc.getInt("server." + i.getName() + ".count") == 0)
			{
				if(server.contains(i.getName()))
				{
					f("Connection to " + i.getName() + " lost");
					server.remove(i.getName());
				}
			}
			
			else if(!server.contains(i.getName()))
			{
				server.add(i.getName());
				s("Connection to " + i.getName() + " established");
			}
		}
	}
	
	public boolean canFire(Transmission t)
	{
		return Phantom.instance().isBungeecord() && Phantom.getServers().contains(t.getDestination()) && Phantom.getNetworkCount(t.getDestination()) > 0 && Phantom.getNetworkCount(t.getSource()) > 0;
	}
	
	public void fire(Transmission t) throws IOException
	{
		if(t.hasPayload())
		{
			responders.add(t);
		}
		
		ByteArrayOutputStream boas = new ByteArrayOutputStream();
		boas.write(t.compress());
		new ForwardedPluginMessage(Phantom.instance(), "PhantomTransmission", t.getDestination(), boas).send();
		s(C.GREEN + sname + " -> " + t.getDestination() + C.YELLOW + " [" + t.getType() + "]");
		to++;
	}
	
	public void transmit(Transmission t) throws IOException
	{
		if(Phantom.getServers() == null)
		{
			queue.add(t);
			return;
		}
		
		if(t.getDestination().equals("ALL"))
		{
			for(String i : Phantom.getServers())
			{
				if(i.equals(Phantom.getServerName()))
				{
					continue;
				}
				
				Transmission meta = t.clone();
				meta.set("t.d", i);
				meta.transmit();
			}
			
			return;
		}
		
		if(canFire(t))
		{
			fire(t);
		}
		
		else
		{
			queue.add(t);
		}
	}
	
	@Override
	public void onStart()
	{
		File df = new File(Phantom.instance().getDataFolder(), "transmission-queue");
		
		if(df.exists())
		{
			v("Loaded " + df.listFiles().length + " cached transmissions");
			
			for(File i : df.listFiles())
			{
				Transmission t = new Transmission("")
				{
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;
					
					@Override
					public void onResponse(Transmission t)
					{
						
					}
				};
				
				try
				{
					new JSONDataInput().load(t, i);
					queue.add(t);
				}
				
				catch(IOException e)
				{
					ExceptionUtil.print(e);
				}
				
				i.delete();
			}
		}
		
		df.delete();
	}
	
	@Override
	public void onStop()
	{
		File df = new File(Phantom.instance().getDataFolder(), "transmission-queue");
		
		if(!queue.isEmpty())
		{
			v("Saving " + queue.size() + " pending transmissions...");
			
			df.mkdirs();
			
			for(Transmission i : queue)
			{
				File file = new File(df, i.getSource() + "- -" + i.getDestination() + " [" + i.getTimeStamp() + "]");
				
				if(i.getSource() == null || i.getSource().equals("null"))
				{
					continue;
				}
				
				try
				{
					new JSONDataOutput().save(i, file);
				}
				
				catch(IOException e)
				{
					ExceptionUtil.print(e);
				}
			}
		}
		
		queue.clear();
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message)
	{
		if(!channel.equals("BungeeCord"))
		{
			return;
		}
		
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		
		if(subchannel.equals("GetServers"))
		{
			GList<String> servers = new GList<String>(in.readUTF().split(", "));
			cc.set("servers", servers);
			
			for(Transmission i : queue.copy())
			{
				if(canFire(i))
				{
					try
					{
						fire(i);
					}
					
					catch(IOException e)
					{
						
					}
					
					queue.remove(i);
				}
			}
		}
		
		else if(subchannel.equals("PlayerCount"))
		{
			String server = in.readUTF();
			int playercount = in.readInt();
			
			cc.set("server." + server + ".count", playercount);
		}
		
		else if(subchannel.equals("PlayerList"))
		{
			String server = in.readUTF();
			String[] playerList = in.readUTF().split(", ");
			
			cc.set("server." + server + ".players", new GList<String>(playerList));
		}
		
		else if(subchannel.equals("GetServer"))
		{
			String server = in.readUTF();
			
			cc.set("this", server);
			sname = server;
			
			if(!connected)
			{
				new TaskLater(20)
				{
					@Override
					public void run()
					{
						s("Bungeecord Connection Established.");
						callEvent(new BungeeConnectionEstablished(Phantom.getServerName(), new GList<String>(Phantom.getServers())));
					}
				};
			}
			
			connected = true;
			t.stop();
			linkSpeed = Math.abs(t.getTime() - (50 * 1000000));
			t = new Timer();
		}
		
		else if(subchannel.equals("PhantomTransmission"))
		{
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);
			
			try
			{
				Transmission t = new Transmission(msgbytes)
				{
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;
					
					@Override
					public void onResponse(Transmission t)
					{
						
					}
				};
				
				s(C.AQUA + sname + " <- " + t.getSource() + C.YELLOW + " [" + t.getType() + "]");
				ti++;
				
				for(Transmitter i : transmitters)
				{
					i.onTransmissionReceived(t);
				}
				
				if(t.hasPayload() && !t.getType().endsWith("-response"))
				{
					Transmission tt = new Transmission(t.getType() + "-response", t.getSource())
					{
						/**
						 * 
						 */
						private static final long serialVersionUID = 1L;
						
						@Override
						public void onResponse(Transmission t)
						{
							
						}
					};
					
					tt.setData(t.getData());
					tt.set("t.t", t.getType() + "-response");
					tt.set("t.d", t.getSource());
					tt.set("t.s", Phantom.getServerName());
					tt.set("t.k", t.getString("t.r"));
					tt.transmit();
				}
				
				if(t.contains("t.k"))
				{
					for(Transmission i : responders.copy())
					{
						if(i.getString("t.r").equals(t.getString("t.k")))
						{
							i.onResponse(t);
							responders.remove(i);
						}
					}
				}
			}
			
			catch(IOException e)
			{
				
			}
		}
		
		((Refreshable) network).refresh();
	}
	
	public DataCluster get()
	{
		return cc;
	}
	
	public boolean connected()
	{
		return connected;
	}
	
	public String getServerName()
	{
		return sname;
	}
	
	public DataCluster getCc()
	{
		return cc;
	}
	
	public GList<Transmitter> getTransmitters()
	{
		return transmitters;
	}
	
	public GList<Transmission> getQueue()
	{
		return queue;
	}
	
	public GList<Transmission> getResponders()
	{
		return responders;
	}
	
	public Boolean getConnected()
	{
		return connected;
	}
	
	public String getSname()
	{
		return sname;
	}
	
	@Override
	public Network getNetwork()
	{
		return network;
	}
	
	public Integer getTi()
	{
		int too = ti;
		ti = 0;
		return too;
	}
	
	public Integer getTo()
	{
		int too = to;
		to = 0;
		return too;
	}
	
	@Override
	public String getMonitorableData()
	{
		return "IO: " + C.GREEN + getTi() + C.DARK_GRAY + "/" + C.RED + getTo() + C.DARK_GRAY + " Cache: " + C.LIGHT_PURPLE + queue.size();
	}
	
	@Override
	public void onTransmissionReceived(Transmission transmission)
	{
		
	}
}
