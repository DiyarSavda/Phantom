package org.phantomapi.nbt;

import java.util.ArrayList;
import java.util.List;

public final class Attribute
{
	
	private AttributeType _type;
	private double _base;
	private List<Modifier> _modifiers = new ArrayList<Modifier>();
	
	public static Attribute fromNBT(NBTTagCompound data)
	{
		Attribute attribute = new Attribute(AttributeType.getByInternalName(data.getString("Name")), data.getDouble("Base"));
		if(data.hasKey("Modifiers"))
		{
			Object[] modifiersData = data.getListAsArray("Modifiers");
			attribute._modifiers = new ArrayList<Modifier>(modifiersData.length);
			for(Object mod : modifiersData)
			{
				attribute.addModifier(Modifier.fromNBT((NBTTagCompound) mod));
			}
		}
		return attribute;
	}
	
	public Attribute(AttributeType type, double base)
	{
		_type = type;
		setBase(base);
	}
	
	public AttributeType getType()
	{
		return _type;
	}
	
	public double getMin()
	{
		return _type.getMin();
	}
	
	public double getMax()
	{
		return _type.getMax();
	}
	
	public double getBase()
	{
		return _base;
	}
	
	public void setBase(double value)
	{
		_base = Math.max(Math.min(value, getMax()), getMin());
	}
	
	public List<Modifier> getModifiers()
	{
		return new ArrayList<Modifier>(_modifiers);
	}
	
	public void setModifiers(List<Modifier> modifiers)
	{
		_modifiers.clear();
		if(modifiers != null)
		{
			_modifiers.addAll(modifiers);
		}
	}
	
	public void addModifier(Modifier modifier)
	{
		_modifiers.add(modifier);
	}
	
	public Modifier removeModifier(int index)
	{
		return _modifiers.remove(index);
	}
	
	public NBTTagCompound toNBT()
	{
		NBTTagCompound data = new NBTTagCompound();
		data.setString("Name", _type._internalName);
		data.setDouble("Base", _base);
		if(_modifiers.size() > 0)
		{
			NBTTagList modifiersData = new NBTTagList();
			for(Modifier modifier : _modifiers)
			{
				modifiersData.add(modifier.toNBT());
			}
			data.setList("Modifiers", modifiersData);
		}
		return data;
	}
	
}
