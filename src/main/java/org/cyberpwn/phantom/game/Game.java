package org.cyberpwn.phantom.game;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.cyberpwn.phantom.lang.Alphabet;
import org.cyberpwn.phantom.lang.GList;
import org.cyberpwn.phantom.util.C;

public interface Game<M extends GameMap<M, G, T, P>, G extends Game<M, G, T, P>, T extends Team<M, G, T, P>, P extends GamePlayer<M, G, T, P>>
{
	public GList<T> getTeams();
	
	public GameEventBus<M, G, T, P> getBus();
	
	public UUID getId();
	
	public void registerGameEventListener(GameListener l);
	
	public void unregisterGameEventListener(GameListener l);
		
	public M getMap();
	
	public Alphabet getAlpha();
	
	public GList<Player> getPlayers();
	
	public GList<P> getGamePlayers();
	
	public GList<Player> getPlayers(T t);
	
	public GList<P> getGamePlayers(T t);
	
	public Boolean contains(Player p);
	
	public Boolean contains(P p);
	
	public Boolean hasTeam(T t);
	
	public Boolean canJoin();
	
	public T getTeam(String name);
	
	public T getTeam(C color);
	
	public P getGamePlayer(Player p);
	
	public T getTeam(P p);
	
	public T onSelectTeam();
	
	public void addTeam(T t) throws GameInitializationExeption;
	
	public void join(P p, T t);
	
	public void join(P p);
	
	public void quit(P p);
}
