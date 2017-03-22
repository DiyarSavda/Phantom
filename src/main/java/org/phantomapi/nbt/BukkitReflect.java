package org.phantomapi.nbt;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.SimpleCommandMap;

public final class BukkitReflect
{
	
	private final static class CachedPackage
	{
		
		private String _packageName;
		private HashMap<String, Class<?>> _cache = new HashMap<String, Class<?>>();
		
		public CachedPackage(String packageName)
		{
			_packageName = packageName;
		}
		
		public Class<?> getClass(String className)
		{
			Class<?> clazz = _cache.get(className);
			if(clazz == null)
			{
				try
				{
					clazz = this.getClass().getClassLoader().loadClass(_packageName + "." + className);
				}
				catch(ClassNotFoundException e)
				{
					throw new RuntimeException("Cannot find class " + _packageName + "." + className + ".", e);
				}
				_cache.put(className, clazz);
			}
			return clazz;
		}
		
	}
	
	private static boolean _isPrepared = false;
	
	private static CachedPackage _craftBukkitPackage;
	private static CachedPackage _minecraftPackage;
	
	private static Method _getCommandMap;
	
	private static HashMap<Material, String> _materialNames = new HashMap<Material, String>();
	
	public static void prepareReflection()
	{
		if(!_isPrepared)
		{
			Class<?> craftServerClass = Bukkit.getServer().getClass();
			_craftBukkitPackage = new CachedPackage(craftServerClass.getPackage().getName());
			try
			{
				Method getHandle = craftServerClass.getMethod("getHandle");
				_minecraftPackage = new CachedPackage(getHandle.getReturnType().getPackage().getName());
				_getCommandMap = craftServerClass.getMethod("getCommandMap");
			}
			catch(NoSuchMethodException e)
			{
				throw new RuntimeException("Cannot find the required methods on the server class.", e);
			}
			
			_isPrepared = true;
			
			try
			{
				Class<?> mc_Item = getMinecraftClass("Item");
				Field mc_Item_REGISTRY = mc_Item.getField("REGISTRY");
				Method mc_Item_getById = mc_Item.getMethod("getById", int.class);
				Method mc_RegistryMaterials_b = mc_Item_REGISTRY.getType().getMethod("b", Object.class);
				Object the_REGISTRY = mc_Item_REGISTRY.get(null);
				for(Material material : Material.values())
				{
					@SuppressWarnings("deprecation")
					Object item = mc_Item_getById.invoke(null, material.getId());
					if(item != null)
					{
						_materialNames.put(material, mc_RegistryMaterials_b.invoke(the_REGISTRY, item).toString());
					}
				}
			}
			catch(Exception e)
			{
				throw new RuntimeException("Error while preparing Material names.", e);
			}
		}
	}
	
	private BukkitReflect()
	{
	}
	
	public static Class<?> getCraftBukkitClass(String className)
	{
		prepareReflection();
		return _craftBukkitPackage.getClass(className);
	}
	
	public static Class<?> getMinecraftClass(String className)
	{
		prepareReflection();
		return _minecraftPackage.getClass(className);
	}
	
	public static SimpleCommandMap getCommandMap()
	{
		prepareReflection();
		return (SimpleCommandMap) invokeMethod(Bukkit.getServer(), _getCommandMap);
	}
	
	public static String getMaterialName(Material material)
	{
		prepareReflection();
		return _materialNames.get(material);
	}
	
	static Object invokeMethod(Object object, Method method, Object... args)
	{
		try
		{
			return method.invoke(object, args);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error while invoking " + method.getName() + ".", e);
		}
	}
	
	static Object getFieldValue(Object object, Field field)
	{
		try
		{
			return field.get(object);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error while getting field value " + field.getName() + " of class " + field.getDeclaringClass().getName() + ".", e);
		}
	}
	
	static void setFieldValue(Object object, Field field, Object value)
	{
		try
		{
			field.set(object, value);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error while setting field value " + field.getName() + " of class " + field.getDeclaringClass().getName() + ".", e);
		}
	}
	
	static Object newInstance(Class<?> clazz)
	{
		try
		{
			return clazz.newInstance();
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error creating instance of " + clazz.getName() + ".", e);
		}
	}
	
	static Object newInstance(Constructor<?> contructor, Object... initargs)
	{
		try
		{
			return contructor.newInstance(initargs);
		}
		catch(Exception e)
		{
			throw new RuntimeException("Error creating instance of " + contructor.getDeclaringClass().getName() + ".", e);
		}
	}
	
}
