package org.phantomapi.clust;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.phantomapi.Phantom;
import org.phantomapi.async.A;
import org.phantomapi.core.RedisConnectionController;
import org.phantomapi.lang.GList;
import org.phantomapi.sync.S;
import org.phantomapi.sync.TaskLater;
import org.phantomapi.util.D;
import org.phantomapi.util.ExceptionUtil;

/**
 * The heart of dataclusters. Most should not have to use this. It is
 * essentially a utility class for controllers and any other components of
 * Phantom to use automatically when you need it called. For example,
 * loadCluster would call parts of this when needed.
 * 
 * @author cyberpwn
 */
public class ConfigurationHandler
{
	/**
	 * Read data from config files and dataclusters and put them into the keyed
	 * config fields in the configurable class supplied
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void toFields(Configurable c)
	{
		for(Field i : c.getClass().getFields())
		{
			if(i.isAnnotationPresent(Keyed.class))
			{
				if(isValidType(i.getType()))
				{
					if(Modifier.isPublic(i.getModifiers()) && !Modifier.isStatic(i.getModifiers()))
					{
						try
						{
							String key = i.getDeclaredAnnotation(Keyed.class).value();
							Object value = c.getConfiguration().getAbstract(key);
							
							if(value instanceof List)
							{
								List<?> l = (List<?>) value;
								GList<String> k = new GList<String>();
								
								for(Object j : l)
								{
									k.add(j.toString());
								}
								
								i.set(c, k);
							}
							
							else
							{
								i.set(c, value);
							}
						}
						
						catch(IllegalArgumentException e)
						{
							ExceptionUtil.print(e);
						}
						
						catch(IllegalAccessException e)
						{
							ExceptionUtil.print(e);
						}
					}
					
					else
					{
						new D(c.getCodeName() + "/" + i.getType().getSimpleName() + " " + i.getName()).w("INVALID MODIFIERS. MUST BE PUBLIC NON STATIC");
					}
				}
				
				else
				{
					new D(c.getCodeName() + "/" + i.getType().getSimpleName() + " " + i.getName()).w("INVALID TYPE. NOT SUPPORTED FOR KEYED CONFIGS");;
				}
			}
		}
	}
	
	/**
	 * Write data to the cluster from the keyed fields from the configurable
	 * object
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void fromFields(Configurable c)
	{
		for(Field i : c.getClass().getFields())
		{
			if(i.isAnnotationPresent(Keyed.class))
			{
				if(isValidType(i.getType()))
				{
					if(Modifier.isPublic(i.getModifiers()) && !Modifier.isStatic(i.getModifiers()))
					{
						try
						{
							String key = i.getDeclaredAnnotation(Keyed.class).value();
							Object value = i.get(c);
							c.getConfiguration().trySet(key, value);
							
							if(i.isAnnotationPresent(Comment.class))
							{
								c.getConfiguration().comment(key, i.getDeclaredAnnotation(Comment.class).value());
							}
						}
						
						catch(IllegalArgumentException e)
						{
							ExceptionUtil.print(e);
						}
						
						catch(IllegalAccessException e)
						{
							ExceptionUtil.print(e);
						}
					}
					
					else
					{
						new D(c.getCodeName() + "/" + i.getType().getSimpleName() + " " + i.getName()).w("INVALID MODIFIERS. MUST BE PUBLIC NON STATIC");
					}
				}
				
				else
				{
					new D(c.getCodeName() + "/" + i.getType().getSimpleName() + " " + i.getName()).w("INVALID TYPE. NOT SUPPORTED FOR KEYED CONFIGS");;
				}
			}
		}
	}
	
	/**
	 * But is this type valid?
	 * 
	 * @param type
	 *            the class type
	 * @return true if can be saved
	 */
	public static boolean isValidType(Class<?> type)
	{
		if(type.equals(String.class))
		{
			return true;
		}
		
		else if(type.equals(Integer.class))
		{
			return true;
		}
		
		else if(type.equals(int.class))
		{
			return true;
		}
		
		else if(type.equals(Long.class))
		{
			return true;
		}
		
		else if(type.equals(long.class))
		{
			return true;
		}
		
		else if(type.equals(Double.class))
		{
			return true;
		}
		
		else if(type.equals(double.class))
		{
			return true;
		}
		
		else if(type.equals(GList.class))
		{
			return true;
		}
		
		else if(type.equals(Boolean.class))
		{
			return true;
		}
		
		else if(type.equals(boolean.class))
		{
			return true;
		}
		
		else
		{
			return false;
		}
	}
	
	/**
	 * Read the configurable object from redis
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void readRedis(Configurable c)
	{
		RedisConnectionController r = Phantom.instance().getRedisConnectionController();
		String key = c.getClass().getAnnotation(Redis.class).value() + ":" + c.getCodeName();
		fromFields(c);
		c.onNewConfig();
		
		if(!r.hasKey(key))
		{
			writeRedis(c);
		}
		
		c.getConfiguration().addJson(new JSONObject(r.read(key)));
		toFields(c);
		c.onReadConfig();
	}
	
	/**
	 * Does the given configurable object reside in redis
	 * 
	 * @param c
	 *            the configurable object
	 * @return true if it exists
	 */
	public static boolean redisExists(Configurable c)
	{
		RedisConnectionController r = Phantom.instance().getRedisConnectionController();
		String key = c.getClass().getAnnotation(Redis.class).value() + ":" + c.getCodeName();
		return r.hasKey(key);
	}
	
	/**
	 * Delete redis key
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void redisDelete(Configurable c)
	{
		RedisConnectionController r = Phantom.instance().getRedisConnectionController();
		String key = c.getClass().getAnnotation(Redis.class).value() + ":" + c.getCodeName();
		r.drop(key);
	}
	
	/**
	 * Write the configurable object to redis
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void writeRedis(Configurable c)
	{
		RedisConnectionController r = Phantom.instance().getRedisConnectionController();
		String key = c.getClass().getAnnotation(Redis.class).value() + ":" + c.getCodeName();
		fromFields(c);
		r.write(key, c.getConfiguration().toJSON().toString());
	}
	
	/**
	 * Handle reading in configs. Also adds new paths that do not exist in the
	 * file from the onNewConfig(), and adds default values
	 * 
	 * @param base
	 *            the base directory
	 * @param c
	 *            the configurable object
	 * @throws IOException
	 *             1337
	 */
	public static void read(File base, Configurable c) throws IOException
	{
		File config = new File(base, c.getCodeName() + ".yml");
		
		if(!config.getParentFile().exists())
		{
			config.getParentFile().mkdirs();
		}
		
		if(!config.exists())
		{
			config.createNewFile();
		}
		
		if(config.isDirectory())
		{
			throw new IOException("Cannot read config (it's a folder)");
		}
		
		fromFields(c);
		c.onNewConfig();
		new YAMLDataInput().load(c.getConfiguration(), config);
		toFields(c);
		
		new TaskLater()
		{
			@Override
			public void run()
			{
				c.onReadConfig();
			}
		};
		
		new S()
		{
			@Override
			public void sync()
			{
				new A()
				{
					@Override
					public void async()
					{
						try
						{
							new YAMLDataOutput().save(c.getConfiguration(), config);
							
							new S()
							{
								@Override
								public void sync()
								{
									try
									{
										new TaskLater(3)
										{
											@Override
											public void run()
											{
												Phantom.instance().getDms().getHotLoadController().registerHotLoad(config, c);
											}
										};
									}
									
									catch(Exception e)
									{
										new D("remote").f("FAILED TO CALL FILE. STATE CHANGED");
									}
								}
							};
						}
						
						catch(IOException e)
						{
							e.printStackTrace();
						}
					}
				};
			}
			
		};
	}
	
	/**
	 * Compat read for standalone applications
	 * 
	 * @param base
	 *            the base file
	 * @param c
	 *            the configurable object
	 * @throws IOException
	 *             shit happens
	 */
	public static void compatRead(File base, Configurable c) throws IOException
	{
		File config = new File(base, c.getCodeName() + ".yml");
		
		if(!config.getParentFile().exists())
		{
			config.getParentFile().mkdirs();
		}
		
		if(!config.exists())
		{
			config.createNewFile();
		}
		
		if(config.isDirectory())
		{
			throw new IOException("Cannot read config (it's a folder)");
		}
		
		fromFields(c);
		c.onNewConfig();
		new YAMLDataInput().load(c.getConfiguration(), config);
		toFields(c);
		c.onReadConfig();
		
		try
		{
			new YAMLDataOutput().save(c.getConfiguration(), config);
		}
		
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Handle saving configs
	 * 
	 * @param base
	 *            the base directory
	 * @param c
	 *            the configurable object
	 * @throws IOException
	 *             1337
	 */
	public static void save(File base, Configurable c) throws IOException
	{
		File config = new File(base, c.getCodeName() + ".yml");
		
		if(!config.getParentFile().exists())
		{
			config.getParentFile().mkdirs();
		}
		
		if(!config.exists())
		{
			config.createNewFile();
		}
		
		if(config.isDirectory())
		{
			throw new IOException("Cannot save config (it's a folder)");
		}
		
		fromFields(c);
		new YAMLDataOutput().save(c.getConfiguration(), config);
	}
	
	public static void savenc(File base, Configurable c) throws IOException
	{
		File config = base;
		
		if(!config.getParentFile().exists())
		{
			config.getParentFile().mkdirs();
		}
		
		if(!config.exists())
		{
			config.createNewFile();
		}
		
		if(config.isDirectory())
		{
			throw new IOException("Cannot save config (it's a folder)");
		}
		
		fromFields(c);
		new YAMLDataOutput().save(c.getConfiguration(), config);
	}
	
	/**
	 * Handle reading in configs. Also adds new paths that do not exist in the
	 * file from the onNewConfig(), and adds default values
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void readMySQL(Configurable c, DatabaseConnection connection) throws IOException, ClassNotFoundException, SQLException
	{
		fromFields(c);
		c.onNewConfig();
		
		Phantom.instance().loadSql(c, new Runnable()
		{
			@Override
			public void run()
			{
				Phantom.instance().saveSql(c, new Runnable()
				{
					@Override
					public void run()
					{
						toFields(c);
						c.onReadConfig();
					}
				});
			}
		});
	}
	
	/**
	 * Handle saving configs
	 * 
	 * @param c
	 *            the configurable object
	 */
	public static void saveMySQL(Configurable c)
	{
		fromFields(c);
		Phantom.instance().saveSql(c, new Runnable()
		{
			@Override
			public void run()
			{
				
			}
		});
	}
	
	/**
	 * Checks if the configurable object has a table annotation for SQL purposes
	 * 
	 * @param c
	 *            the configurable object
	 * @return true if a table is defined
	 */
	public static boolean hasTable(Configurable c)
	{
		return c.getClass().isAnnotationPresent(Tabled.class);
	}
	
	/**
	 * Checks if the configurable object has a redis annotation for redis
	 * purposes
	 * 
	 * @param c
	 *            the configurable object
	 * @return true if a table is defined
	 */
	public static boolean hasRedisTable(Configurable c)
	{
		return c.getClass().isAnnotationPresent(Redis.class);
	}
	
	/**
	 * Get the table defined in this tabled configurable object
	 * 
	 * @param c
	 *            the tabled configurable object
	 * @return the table
	 */
	public static String getTable(Configurable c)
	{
		return c.getClass().getAnnotation(Tabled.class).value();
	}
	
	public static String grave(String s)
	{
		return "`" + s + "`";
	}
	
	/**
	 * Check if the given row exists
	 * 
	 * @param table
	 *            the table
	 * @param codeName
	 *            the code name
	 * @param db
	 *            the database
	 * @return true if it exists
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             shit happens
	 */
	public static boolean rowExists(String table, String codeName, MySQL db) throws SQLException, ClassNotFoundException
	{
		Connection conn = null;
		
		if(!db.checkConnection())
		{
			conn = db.openConnection();
		}
		
		else
		{
			conn = db.getConnection();
		}
		
		PreparedStatement ps = conn.prepareStatement("SELECT count(*) from " + table + " WHERE k = ?");
		ps.setString(1, codeName);
		ResultSet resultSet = ps.executeQuery();
		
		if(resultSet.next())
		{
			final int count = resultSet.getInt(1);
			
			return count > 0;
		}
		
		return false;
	}
	
	/**
	 * Check if the given row exists. Keep in mind this only checks the code
	 * name, this does not compare the actual data
	 * 
	 * @param c
	 *            the configurable object in a row
	 * @param db
	 *            the database
	 * @return true if it contains the configurable object
	 * @throws ClassNotFoundException
	 *             shit happens
	 * @throws SQLException
	 *             shit happens
	 */
	public static boolean rowExists(Configurable c, MySQL db) throws ClassNotFoundException, SQLException
	{
		return rowExists(getTable(c), c.getCodeName(), db);
	}
	
	/**
	 * Delete the mysql table
	 * 
	 * @param c
	 *            the configurable object
	 * @param db
	 *            the database
	 * @return the sql connection
	 * @throws ClassNotFoundException
	 *             shit happens
	 * @throws SQLException
	 *             really bad shit happens
	 */
	public static MySQL dropRow(Configurable c, MySQL db) throws ClassNotFoundException, SQLException
	{
		return dropRow(getTable(c), c.getCodeName(), db);
	}
	
	/**
	 * Drop the given row
	 * 
	 * @param table
	 *            the table
	 * @param codeName
	 *            the code name
	 * @param db
	 *            the database
	 * @return the sql
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             shit happens
	 */
	public static MySQL dropRow(String table, String codeName, MySQL db) throws SQLException, ClassNotFoundException
	{
		Connection conn = null;
		
		if(!db.checkConnection())
		{
			conn = db.openConnection();
		}
		
		else
		{
			conn = db.getConnection();
		}
		
		PreparedStatement st = conn.prepareStatement("DELETE FROM " + "`" + table + "`" + " WHERE k=?;");
		st.setString(1, codeName);
		st.executeUpdate();
		
		return db;
	}
	
	/**
	 * Pull data from mysql into the configurable object
	 * 
	 * @param c
	 *            the configurable object
	 * @param connection
	 *            the connection data
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             really bad shit happens
	 */
	public static MySQL fromMysql(Configurable c, MySQL db) throws SQLException, ClassNotFoundException
	{
		c.onNewConfig();
		fromFields(c);
		
		Connection conn = null;
		
		if(!db.checkConnection())
		{
			conn = db.openConnection();
		}
		
		else
		{
			conn = db.getConnection();
		}
		
		PreparedStatement s = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + "`" + getTable(c) + "`" + " (`k` TEXT, `d` TEXT);");
		s.execute();
		s.close();
		
		PreparedStatement st = conn.prepareStatement("SELECT * FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
		st.setString(1, c.getCodeName());
		ResultSet res = st.executeQuery();
		
		if(res.next())
		{
			PreparedStatement stx = conn.prepareStatement("SELECT `d` FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
			stx.setString(1, c.getCodeName());
			ResultSet resx = stx.executeQuery();
			resx.next();
			JSONObject jso = new JSONObject(resx.getString("d"));
			c.getConfiguration().addJson(jso);
			toFields(c);
			c.onReadConfig();
			resx.close();
			stx.close();
		}
		
		res.close();
		st.close();
		
		return db;
	}
	
	/**
	 * Put data from configurable objects into mysql
	 * 
	 * @param c
	 *            the configurable object
	 * @param connection
	 *            the connection data
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             really bad shit happens
	 */
	public static MySQL toMysql(Configurable c, MySQL db) throws SQLException, ClassNotFoundException
	{
		fromFields(c);
		Connection conn = null;
		
		if(!db.checkConnection())
		{
			conn = db.openConnection();
		}
		
		else
		{
			conn = db.getConnection();
		}
		
		PreparedStatement s = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + "`" + getTable(c) + "`" + " (`k` TEXT, `d` TEXT);");
		s.execute();
		s.close();
		
		PreparedStatement st = conn.prepareStatement("SELECT * FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
		st.setString(1, c.getCodeName());
		ResultSet res = st.executeQuery();
		
		if(res.next())
		{
			PreparedStatement stx = conn.prepareStatement("UPDATE " + "`" + getTable(c) + "`" + " SET d=? WHERE k=?;");
			stx.setString(1, c.getConfiguration().toJSON().toString());
			stx.setString(2, c.getCodeName());
			stx.executeUpdate();
			stx.close();
		}
		
		else
		{
			PreparedStatement stx = conn.prepareStatement("INSERT INTO " + "`" + getTable(c) + "`" + " values(?,?);");
			stx.setString(1, c.getCodeName());
			stx.setString(2, c.getConfiguration().toJSON().toString());
			stx.executeUpdate();
			stx.close();
		}
		
		res.close();
		st.close();
		
		return db;
	}
	
	/**
	 * Put data from the configurable object into the sqlite databse
	 * 
	 * @param c
	 *            the configurable object
	 * @param file
	 *            the file
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             shit happens
	 */
	public static void toSQLite(Configurable c, File file) throws SQLException, ClassNotFoundException
	{
		fromFields(c);
		Connection conn = null;
		SQLiteHandle sq = Phantom.instance().getSqLiteConnectionController().getHandle(file);
		conn = sq.getConnection();
		
		PreparedStatement s = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + "`" + getTable(c) + "`" + " (`k` TEXT, `d` TEXT);");
		s.execute();
		s.close();
		
		PreparedStatement st = conn.prepareStatement("SELECT * FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
		st.setString(1, c.getCodeName());
		ResultSet res = st.executeQuery();
		
		if(res.next())
		{
			PreparedStatement stx = conn.prepareStatement("UPDATE " + "`" + getTable(c) + "`" + " SET d=? WHERE k=?;");
			stx.setString(1, c.getConfiguration().toJSON().toString());
			stx.setString(2, c.getCodeName());
			stx.executeUpdate();
			stx.close();
		}
		
		else
		{
			PreparedStatement stx = conn.prepareStatement("INSERT INTO " + "`" + getTable(c) + "`" + " values(?,?);");
			stx.setString(1, c.getCodeName());
			stx.setString(2, c.getConfiguration().toJSON().toString());
			stx.executeUpdate();
			stx.close();
		}
		
		res.close();
		st.close();
	}
	
	/**
	 * Pull data from an sqlite database into the configurable object
	 * 
	 * @param c
	 *            the configurable object
	 * @param file
	 *            the file
	 * @throws SQLException
	 *             shit happens
	 * @throws ClassNotFoundException
	 *             shit happens
	 */
	public static void fromSQLite(Configurable c, File file) throws SQLException, ClassNotFoundException
	{
		c.onNewConfig();
		fromFields(c);
		Connection conn = null;
		SQLiteHandle sq = Phantom.instance().getSqLiteConnectionController().getHandle(file);
		conn = sq.getConnection();
		
		PreparedStatement s = conn.prepareStatement("CREATE TABLE IF NOT EXISTS " + "`" + getTable(c) + "`" + " (`k` TEXT, `d` TEXT);");
		s.execute();
		s.close();
		
		PreparedStatement st = conn.prepareStatement("SELECT * FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
		st.setString(1, c.getCodeName());
		ResultSet res = st.executeQuery();
		
		if(res.next())
		{
			PreparedStatement stx = conn.prepareStatement("SELECT `d` FROM " + "`" + getTable(c) + "`" + " WHERE k=?;");
			stx.setString(1, c.getCodeName());
			ResultSet resx = stx.executeQuery();
			resx.next();
			JSONObject jso = new JSONObject(resx.getString("d"));
			c.getConfiguration().addJson(jso);
			toFields(c);
			c.onReadConfig();
			resx.close();
			stx.close();
		}
		
		res.close();
		st.close();
	}
}