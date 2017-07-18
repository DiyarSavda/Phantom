package org.phantomapi.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UMap<K, V> extends ConcurrentHashMap<K, V>
{
	private static final long serialVersionUID = 1527847670799761130L;
	
	public UMap()
	{
		super();
	}
	
	public UMap(Map<K, V> map)
	{
		super();
		
		for(K i : map.keySet())
		{
			put(i, map.get(i));
		}
	}
	
	/**
	 * Copy the map
	 * 
	 * @return the copied map
	 */
	public UMap<K, V> copy()
	{
		UMap<K, V> m = new UMap<K, V>();
		
		for(K k : this.keySet())
		{
			m.put(k, get(k));
		}
		
		return m;
	}
	
	/**
	 * Chain put
	 * 
	 * @param k
	 *            the key
	 * @param v
	 *            the value
	 * @return the modified map
	 */
	public UMap<K, V> qput(K k, V v)
	{
		put(k, v);
		return this.copy();
	}
	
	/**
	 * Flips the maps keys and values.
	 * 
	 * @return GMap V, K instead of K, V
	 */
	public UMap<V, UList<K>> flip()
	{
		UMap<V, UList<K>> flipped = new UMap<V, UList<K>>();
		
		for(K i : keySet())
		{
			if(i == null)
			{
				continue;
			}
			
			if(!flipped.containsKey(get(i)))
			{
				flipped.put(get(i), new UList<K>());
			}
			
			flipped.get(get(i)).add(i);
		}
		
		return flipped;
	}
	
	@Override
	public String toString()
	{
		UList<String> s = new UList<String>();
		
		for(K i : keySet())
		{
			s.add(i.toString() + ": " + get(i).toString());
		}
		
		return "[" + s.toString() + "]";
	}
	
	/**
	 * Add maps contents into the current map
	 * 
	 * @param umap
	 *            the map to add in
	 * @return the modified current map
	 */
	public UMap<K, V> append(UMap<K, V> umap)
	{
		for(K i : umap.keySet())
		{
			put(i, umap.get(i));
		}
		
		return this;
	}
	
	/**
	 * Get a copied GList of the keys (modification safe)
	 * 
	 * @return keys
	 */
	public UList<K> k()
	{
		return new UList<K>(keySet());
	}
	
	/**
	 * Get a copied GSet of the keys (modification safe)
	 * 
	 * @return keys
	 */
	public USet<K> kset()
	{
		return new USet<K>(keySet());
	}
	
	/**
	 * Get a copied GList of the values (modification safe)
	 * 
	 * @return values
	 */
	public UList<V> v()
	{
		return new UList<V>(values());
	}
	
	/**
	 * Get a copied GSet of the values (modification safe)
	 * 
	 * @return values
	 */
	public USet<V> vset()
	{
		return new USet<V>(values());
	}
	
	/**
	 * Put if and only if the key does not yet exist
	 * 
	 * @param k
	 *            the key
	 * @param v
	 *            the value
	 */
	public void putNVD(K k, V v)
	{
		if(!containsValue(v))
		{
			put(k, v);
		}
	}
	
	/**
	 * Override. Works just like containsKey(Object o)
	 */
	@Override
	public boolean contains(Object o)
	{
		return containsKey(o);
	}
	
	/**
	 * Get a Glist of values from a list of keys
	 * 
	 * @param keys
	 *            the requested keys
	 * @return the resulted values
	 */
	public UList<V> get(UList<K> keys)
	{
		UList<V> ulv = new UList<V>();
		
		for(K i : keys)
		{
			if(get(i) != null)
			{
				ulv.add(get(i));
			}
		}
		
		return ulv;
	}
	
	/**
	 * Removes duplicate values by removing keys with values that match other
	 * values with different keys
	 * 
	 * @return the modified map
	 */
	public UMap<K, V> removeDuplicateValues()
	{
		UMap<K, V> map = this.copy();
		UList<K> keys = map.k().removeDuplicates();
		
		clear();
		
		for(K i : keys)
		{
			put(i, map.get(i));
		}
		
		return this;
	}
	
	/**
	 * Put a bunch of keys and values does nothing if the lists sizes dont
	 * match. The order of the list is how assignment will be determined
	 * 
	 * @param k
	 *            the keys
	 * @param v
	 *            the values
	 */
	public void put(UList<K> k, UList<V> v)
	{
		if(k.size() != v.size())
		{
			return;
		}
		
		for(int i = 0; i < k.size(); i++)
		{
			put(k, v);
		}
	}
	
	/**
	 * Sort keys based on values sorted
	 * 
	 * @return the sorted keys
	 */
	public UList<K> sortK()
	{
		UList<K> k = new UList<K>();
		UList<V> v = v();
		
		v.sort();
		
		for(V i : v)
		{
			for(K j : k())
			{
				if(get(j).equals(i))
				{
					k.add(j);
				}
			}
		}
		
		return k;
	}
	
	/**
	 * Sort values based on keys sorted
	 * 
	 * @return the sorted values
	 */
	public UList<V> sortV()
	{
		UList<V> v = new UList<V>();
		UList<K> k = k();
		
		k.sort();
		
		for(K i : k)
		{
			for(V j : v())
			{
				if(get(i).equals(j))
				{
					v.add(j);
				}
			}
		}
		
		return v;
	}
}
