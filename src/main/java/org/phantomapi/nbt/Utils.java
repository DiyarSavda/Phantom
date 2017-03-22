package org.phantomapi.nbt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.util.StringUtil;

public final class Utils
{
	
	private Utils()
	{
	}
	
	public static int parseInt(String str, int defaultValue)
	{
		try
		{
			return Integer.parseInt(str);
		}
		catch(NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	public static int parseInt(String str, int max, int min, int defaultValue)
	{
		int value = parseInt(str, defaultValue);
		return (value > max || value < min ? defaultValue : value);
	}
	
	public static int parseTimeDuration(String str)
	{
		Matcher matcher = Pattern.compile("^(?:(\\d{1,4})d)?(?:(\\d{1,2})h)?(?:(\\d{1,2})m)?(?:(\\d{1,2})s)?$", Pattern.CASE_INSENSITIVE).matcher(str);
		if(matcher.find())
		{
			int d = Utils.parseInt(matcher.group(1), 0);
			int h = Utils.parseInt(matcher.group(2), 0);
			int m = Utils.parseInt(matcher.group(3), 0);
			int s = Utils.parseInt(matcher.group(4), 0);
			if(h < 24 && m < 60 && s < 60)
			{
				return d * 86400 + h * 3600 + m * 60 + s;
			}
		}
		return -1;
	}
	
	public static List<String> getElementsWithPrefix(Collection<String> values, String prefix)
	{
		return getElementsWithPrefix(values, prefix, false);
	}
	
	public static List<String> getElementsWithPrefix(Collection<String> values, String prefix, boolean sort)
	{
		if(prefix == null || prefix.isEmpty())
		{
			List<String> result = new ArrayList<String>(values);
			if(sort)
			{
				Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
			}
			return Collections.unmodifiableList(result);
		}
		else
		{
			List<String> result = new ArrayList<String>();
			for(String string : values)
			{
				if(StringUtil.startsWithIgnoreCase(string, prefix))
				{
					result.add(string);
				}
			}
			if(sort)
			{
				Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
			}
			return Collections.unmodifiableList(result);
		}
	}
	
	public static List<String> getElementsWithPrefixGeneric(Collection<?> values, String prefix)
	{
		return getElementsWithPrefixGeneric(values, prefix, false);
	}
	
	public static List<String> getElementsWithPrefixGeneric(Collection<?> values, String prefix, boolean sort)
	{
		List<String> result = new ArrayList<String>(values.size());
		if(prefix == null || prefix.isEmpty())
		{
			for(Object obj : values)
			{
				result.add(obj.toString());
			}
		}
		else
		{
			for(Object obj : values)
			{
				String str = obj.toString();
				if(StringUtil.startsWithIgnoreCase(str, prefix))
				{
					result.add(str);
				}
			}
		}
		if(sort)
		{
			Collections.sort(result, String.CASE_INSENSITIVE_ORDER);
		}
		return Collections.unmodifiableList(result);
	}
	
}
