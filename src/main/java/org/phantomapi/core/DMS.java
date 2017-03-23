package org.phantomapi.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.phantomapi.Phantom;
import org.phantomapi.async.AsyncTask;
import org.phantomapi.async.Callback;
import org.phantomapi.clust.DataCluster;
import org.phantomapi.construct.Controllable;
import org.phantomapi.construct.Controller;
import org.phantomapi.construct.Ticked;
import org.phantomapi.lang.GList;
import org.phantomapi.lang.GMap;
import org.phantomapi.lang.Title;
import org.phantomapi.network.PluginMessage;
import org.phantomapi.statistics.Monitorable;
import org.phantomapi.sync.Task;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.text.PhantomSpinner;
import org.phantomapi.util.C;
import org.phantomapi.util.D;
import org.phantomapi.util.F;
import org.phantomapi.util.PluginUtil;
import org.phantomapi.util.ServerState;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

/**
 * Startup environment tests
 * 
 * @author cyberpwn
 */
@SyncStart
@Ticked(1200)
public class DMS extends Controller implements PluginMessageListener, Monitorable
{
	private String address;
	private Boolean hasInternet;
	private String sname;
	private Boolean sqlt;
	private GList<Plugin> pql;
	private GList<String> servers;
	private HotLoadController hotLoadController;
	private ConfigurationBackupController configurationBackupController;
	private static ServerState state;
	private PhantomSpinner spinner;
	private GMap<Player, String> progressing;
	
	public DMS(Controllable parentController)
	{
		super(parentController);
		
		D.d(this, "STARTING DMS");
		address = null;
		hasInternet = null;
		sname = "Unknown";
		pql = new GList<Plugin>();
		servers = new GList<String>();
		sqlt = false;
		hotLoadController = new HotLoadController(this);
		configurationBackupController = new ConfigurationBackupController(this);
		progressing = new GMap<Player, String>();
		spinner = new PhantomSpinner();
		
		D.d(this, "Register HLC & Backups");
		register(hotLoadController);
		register(configurationBackupController);
		
		D.d(this, "Set state");
		if(Bukkit.getOnlinePlayers().isEmpty())
		{
			state = ServerState.START;
		}
		
		else
		{
			state = ServerState.ENABLE;
		}
		
		D.d(this, "Register messager");
		getPlugin().getServer().getMessenger().registerOutgoingPluginChannel(getPlugin(), "BungeeCord");
		getPlugin().getServer().getMessenger().registerIncomingPluginChannel(getPlugin(), "BungeeCord", this);
		
		new Task(0)
		{
			@Override
			public void run()
			{
				String next = spinner.toString();
				
				for(Player i : progressing.k())
				{
					String msg = progressing.get(i);
					String sub = "";
					
					if(msg.contains(";"))
					{
						sub = msg.split(";")[1];
						msg = msg.split(";")[0];
					}
					
					new Title(C.LIGHT_PURPLE + msg, C.LIGHT_PURPLE + next + " " + C.DARK_GRAY + sub, "  ", 0, 5, 5).send(i);
				}
			}
		};
	}
	
	@EventHandler
	public void on(PlayerCommandPreprocessEvent e)
	{
		if(e.getMessage().equalsIgnoreCase("/stop"))
		{
			D.d(this, "STOP COMMAND CAUGHT");
			
			if(e.getPlayer().hasPermission("bukkit.command.stop"))
			{
				state = ServerState.STOP;
			}
		}
	}
	
	@EventHandler
	public void on(ServerCommandEvent e)
	{
		D.d(this, "STOP COMMAND CAUGHT");
		
		if(e.getCommand().equalsIgnoreCase("stop"))
		{
			state = ServerState.STOP;
		}
	}
	
	@Override
	public void onTick()
	{
		requestServerNaming();
	}
	
	public void requestServerNaming()
	{
		new PluginMessage(getPlugin(), "GetServer").send();
		new PluginMessage(getPlugin(), "GetServers").send();
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
		
		if(subchannel.equals("GetServer"))
		{
			sname = in.readUTF();
		}
		
		if(subchannel.equals("GetServers"))
		{
			servers = new GList<String>(in.readUTF().split(", "));
		}
	}
	
	@Override
	public void onStop()
	{
		if(state.equals(ServerState.RUNNING))
		{
			state = ServerState.DISABLE;
		}
	}
	
	@Override
	public void onStart()
	{
		requestServerNaming();
		
		new TaskLater(5)
		{
			@Override
			public void run()
			{
				D.d(this, "Building Environment");
				Phantom.splash("", "", "", "    " + Phantom.instance().getMsgx().pickRandom(), "    " + Phantom.instance().getMsgx().pickRandom(), "    " + Phantom.instance().getMsgx().pickRandom());
				D.d(this, "Splash, get environment");
				s("-------- Environment --------");
				s("> " + C.AQUA + "Controllers: " + C.GREEN + Phantom.instance().getBindings().size());
				testInternetConnection();
				showDiskSpace();
				s("> " + C.AQUA + "Bungee Server: " + C.GREEN + sname);
				
				for(String i : servers)
				{
					s("  > " + C.AQUA + C.GREEN + i);
				}
				
				if(sqlt)
				{
					w("SQL Test Required due to " + pql.size() + " plugin(s) depending on sql.");
					if(!testMySqlConnection())
					{
						for(Plugin i : pql)
						{
							f(i.getName() + " cannot function without sql.");
							PluginUtil.disable(i);
						}
					}
				}
				
				D.d(this, "Get Boot times");
				s("> " + C.LIGHT_PURPLE + "Async Boot Time: " + C.AQUA + F.f(Phantom.am / 1000.0, 3) + "s " + C.YELLOW + F.pc(Phantom.am / (Phantom.am + Phantom.sm), 1));
				s("> " + C.LIGHT_PURPLE + "Sync Boot Time: " + C.AQUA + F.f(Phantom.sm / 1000.0, 3) + "s " + C.YELLOW + F.pc(Phantom.sm / (Phantom.am + Phantom.sm), 1));
			}
		};
		
		new TaskLater(0)
		{
			@Override
			public void run()
			{
				state = ServerState.RUNNING;
			}
		};
	}
	
	public void showDiskSpace()
	{
		D.d(this, "Showing disk space");
		s(C.YELLOW + "! " + C.AQUA + "Max  Memory: " + C.GREEN + F.memSize(Runtime.getRuntime().maxMemory()));
		s(C.YELLOW + "! " + C.AQUA + "Logic Cores: " + C.GREEN + Runtime.getRuntime().availableProcessors());
		s(C.YELLOW + "! " + C.AQUA + "At Location: " + C.GREEN + getPlugin().getDataFolder().getParentFile().getAbsolutePath());
		s(C.YELLOW + "! " + C.AQUA + "Free  Space: " + C.GREEN + F.fileSize(new File("/").getUsableSpace()));
		s(C.YELLOW + "! " + C.AQUA + "Used  Space: " + C.GREEN + F.fileSize(new File("/").getTotalSpace() - new File("/").getUsableSpace()));
		s(C.YELLOW + "! " + C.AQUA + "Total Space: " + C.GREEN + F.fileSize(new File("/").getTotalSpace()));
	}
	
	public boolean testMySqlConnection()
	{
		D.d(this, "Test SQL");
		w("> " + C.AQUA + "Testing MySQL Connection...");
		
		if(((Phantom) getPlugin()).getMySQLConnectionController().testConnection())
		{
			s("> " + C.AQUA + "Connected: " + C.GREEN + "MySQL");
			return true;
		}
		
		else
		{
			f("> " + C.YELLOW + "NETWORK FAILURE. NOT CONNECTED");
			return false;
		}
	}
	
	public void needsSQL(Plugin pl)
	{
		sqlt = true;
		pql.add(pl);
	}
	
	public void testInternetConnection()
	{
		w("> " + C.AQUA + "Testing Internet Connection...");
		new AsyncTask<String>(new Callback<String>()
		{
			@Override
			public void run()
			{
				address = get();
				
				if(get() == null)
				{
					f("> " + C.YELLOW + "NETWORK FAILURE. NOT CONNECTED");
					hasInternet = false;
				}
				
				else
				{
					s("> " + C.AQUA + "Connected <> Address: " + C.GREEN + get());
					hasInternet = true;
				}
				
				Phantom.instance().setEnvironmentData(getPlugin(), "status-network-failure", !hasInternet);
			}
		})
		{
			@Override
			public String execute()
			{
				try
				{
					BufferedReader pr = new BufferedReader(new InputStreamReader(new URL("http://checkip.amazonaws.com/").openStream()));
					String address = pr.readLine();
					
					pr.close();
					
					return address;
				}
				
				catch(Exception e)
				{
					return null;
				}
			}
		}.start();
	}
	
	public String getAddress()
	{
		return address;
	}
	
	public void setAddress(String address)
	{
		this.address = address;
	}
	
	public Boolean getHasInternet()
	{
		return hasInternet;
	}
	
	public void setHasInternet(Boolean hasInternet)
	{
		this.hasInternet = hasInternet;
	}
	
	public static ServerState getState()
	{
		return state;
	}
	
	public String getServerName()
	{
		return sname;
	}
	
	public String getSname()
	{
		return sname;
	}
	
	public Boolean getSqlt()
	{
		return sqlt;
	}
	
	public GList<Plugin> getPql()
	{
		return pql;
	}
	
	public GList<String> getServers()
	{
		return servers;
	}
	
	public HotLoadController getHotLoadController()
	{
		return hotLoadController;
	}
	
	public ConfigurationBackupController getConfigurationBackupController()
	{
		return configurationBackupController;
	}
	
	public void showProgress(Player p, String message)
	{
		progressing.put(p, message);
	}
	
	public void clearProgress(Player p)
	{
		progressing.remove(p);
	}
	
	@EventHandler
	public void on(PlayerQuitEvent e)
	{
		clearProgress(e.getPlayer());
	}
	
	@Override
	public String getMonitorableData()
	{
		return "Ramdisk: " + C.LIGHT_PURPLE + F.fileSize(DataCluster.totalSize) + C.DARK_GRAY + " Clusters: " + C.LIGHT_PURPLE + F.f(DataCluster.totalClusters) + C.DARK_GRAY + " Nodes: " + C.LIGHT_PURPLE + F.f(DataCluster.totalNodes) + C.DARK_GRAY + " Mods: " + C.LIGHT_PURPLE + F.f(DataCluster.permX);
	}
	
	public PhantomSpinner getSpinner()
	{
		return spinner;
	}
	
	public GMap<Player, String> getProgressing()
	{
		return progressing;
	}
}