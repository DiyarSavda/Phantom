package org.phantomapi.util;

import java.util.Collection;
import java.util.HashSet;

public class USet<T> extends HashSet<T>
{
	private static final long serialVersionUID = 1L;

	public USet()
	{
		super();
	}

	public USet(Collection<? extends T> c)
	{
		super(c);
	}

	public USet(int initialCapacity, float loadFactor)
	{
		super(initialCapacity, loadFactor);
	}

	public USet(int initialCapacity)
	{
		super(initialCapacity);
	}
}
