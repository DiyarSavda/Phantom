package org.phantomapi.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;

abstract class EntityNBTBase
{
	
	private static HashMap<EntityType, Class<? extends EntityNBT>> _entityClasses;
	private static HashMap<Class<? extends EntityNBT>, NBTGenericVariableContainer> _entityVariables;
	private static HashMap<EntityType, NBTGenericVariableContainer> _entityVariablesByType;
	
	static
	{
		_entityClasses = new HashMap<EntityType, Class<? extends EntityNBT>>();
		_entityVariables = new HashMap<Class<? extends EntityNBT>, NBTGenericVariableContainer>();
		_entityVariablesByType = new HashMap<EntityType, NBTGenericVariableContainer>();
		// Force static initialization of the EntityNBT class.
		try
		{
			Class.forName(EntityNBT.class.getName());
		}
		catch(ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	static void registerVariables(Class<? extends EntityNBT> entityClass, NBTGenericVariableContainer variables)
	{
		_entityVariables.put(entityClass, variables);
	}
	
	static void registerVariables(EntityType entityType, NBTGenericVariableContainer variables)
	{
		_entityVariablesByType.put(entityType, variables);
	}
	
	static void registerEntity(EntityType entityType, Class<? extends EntityNBT> entityClass)
	{
		_entityClasses.put(entityType, entityClass);
	}
	
	public static boolean isValidType(EntityType entityType)
	{
		return _entityClasses.containsKey(entityType);
	}
	
	public static Collection<EntityType> getValidEntityTypes()
	{
		return _entityClasses.keySet();
	}
	
	private static EntityNBT newInstance(EntityType entityType, NBTTagCompound data)
	{
		Class<? extends EntityNBT> entityClass = _entityClasses.get(entityType);
		EntityNBTBase instance;
		if(entityClass != null)
		{
			try
			{
				instance = entityClass.newInstance();
			}
			catch(Exception e)
			{
				throw new RuntimeException("Error when instantiating " + entityClass.getName() + ".", e);
			}
			instance._entityType = entityType;
		}
		else
		{
			instance = new EntityNBT(entityType);
		}
		if(data != null)
		{
			instance._data = data;
		}
		instance._data.setString("id", EntityTypeMap.getName(entityType));
		return (EntityNBT) instance;
	}
	
	public static EntityNBT fromEntityType(EntityType entityType)
	{
		if(_entityClasses.containsKey(entityType))
		{
			return newInstance(entityType, null);
		}
		return null;
	}
	
	public static EntityNBT fromEntityData(NBTTagCompound data)
	{
		EntityType entityType = EntityTypeMap.getByName(data.getString("id"));
		if(entityType != null)
		{
			return newInstance(entityType, data);
		}
		return null;
	}
	
	public static EntityNBT fromEntity(Entity entity)
	{
		EntityNBT entityNbt = newInstance(entity.getType(), NBTUtils.getEntityNBTData(entity));
		// When cloning, remove the UUID to force all entities to have a unique
		// one.
		entityNbt._data.remove("UUIDMost");
		entityNbt._data.remove("UUIDLeast");
		return entityNbt;
	}
	
	public static EntityNBT unserialize(String serializedData)
	{
		try
		{
			NBTTagCompound data = NBTTagCompound.unserialize(Base64.decode(serializedData));
			
			// Backward compatibility with pre-1.9.
			// On 1.9 the entities are stacked for bottom to top.
			// This conversion needs to happen before instantiating any class,
			// we cannot use onUnserialize.
			while(data.hasKey("Riding"))
			{
				NBTTagCompound riding = data.getCompound("Riding");
				data.remove("Riding");
				riding.setList("Passengers", new NBTTagList(data));
				data = riding;
			}
			
			EntityNBT entityNBT = fromEntityData(data);
			entityNBT.onUnserialize();
			return entityNBT;
		}
		catch(Throwable e)
		{
			throw new RuntimeException("Error unserializing EntityNBT.", e);
		}
	}
	
	private EntityType _entityType;
	protected NBTTagCompound _data;
	
	protected EntityNBTBase(EntityType entityType)
	{
		_entityType = entityType;
		_data = new NBTTagCompound();
		if(entityType != null)
		{
			_data.setString("id", EntityTypeMap.getName(entityType));
		}
	}
	
	public EntityType getEntityType()
	{
		return _entityType;
	}
	
	public NBTVariableContainer[] getAllVariables()
	{
		NBTGenericVariableContainer aux;
		ArrayList<NBTVariableContainer> list = new ArrayList<NBTVariableContainer>(3);
		
		if((aux = _entityVariablesByType.get(_entityType)) != null)
		{
			list.add(aux.boundToData(_data));
		}
		
		for(Class<?> clazz = this.getClass(); clazz != Object.class; clazz = clazz.getSuperclass())
		{
			if((aux = _entityVariables.get(clazz)) != null)
			{
				list.add(aux.boundToData(_data));
			}
		}
		return list.toArray(new NBTVariableContainer[0]);
	}
	
	public NBTVariable getVariable(String name)
	{
		NBTGenericVariableContainer aux;
		name = name.toLowerCase();
		
		if((aux = _entityVariablesByType.get(_entityType)) != null)
		{
			if(aux.hasVariable(name))
			{
				return aux.getVariable(name, _data);
			}
		}
		
		for(Class<?> clazz = this.getClass(); clazz != Object.class; clazz = clazz.getSuperclass())
		{
			if((aux = _entityVariables.get(clazz)) != null)
			{
				if(aux.hasVariable(name))
				{
					return aux.getVariable(name, _data);
				}
			}
		}
		return null;
	}
	
	public Entity spawn(Location location)
	{
		return NBTUtils.spawnEntity(_data, location);
	}
	
	public String serialize()
	{
		try
		{
			return Base64.encodeBytes(_data.serialize(), Base64.GZIP);
		}
		catch(Throwable e)
		{
			throw new RuntimeException("Error serializing EntityNBT.", e);
		}
	}
	
	void onUnserialize()
	{
	}
	
	@Override
	public EntityNBT clone()
	{
		return fromEntityData(_data.clone());
	}
	
	public NBTTagCompound getData()
	{
		return _data.clone();
	}
	
	public NBTTagCompound getDataRaw()
	{
		return _data.clone();
	}
	
	public String getMetadataString()
	{
		NBTTagCompound data = _data.clone();
		data.remove("id");
		return data.toString();
	}
	
}
