package org.phantomapi.util;

import java.io.File;
import java.io.IOException;
import java.net.URI;

/**
 * A GFile object (extends File)
 * 
 * @author cyberpwn
 *
 */
public class UFile extends File
{
	private static final long serialVersionUID = 1L;
	
	/**
	 * Create a new file
	 * 
	 * @param parent
	 *            the parent file
	 * @param child
	 *            the child name
	 */
	public UFile(File parent, String child)
	{
		super(parent, child);
	}
	
	/**
	 * Create a file object from a URI
	 * 
	 * @param uri
	 *            the URI
	 */
	public UFile(URI uri)
	{
		super(uri);
	}
	
	/**
	 * Create a file object
	 * 
	 * @param parent
	 *            string parent
	 * @param child
	 *            string child
	 */
	public UFile(String parent, String child)
	{
		super(parent, child);
	}
	
	/**
	 * Create a file parent//child1/child2/child3
	 * 
	 * @param parent
	 *            the parents
	 * @param childs
	 *            the children
	 */
	public UFile(UFile parent, String... childs)
	{
		super(parent, new UList<String>(childs).toString(pathSeparator));
	}
	
	/**
	 * Create a file from a string
	 * 
	 * @param child
	 *            the file path
	 */
	public UFile(String child)
	{
		super(child);
	}
	
	/**
	 * Create a new file, and make the directories for it aswell
	 */
	public boolean createNewFile() throws IOException
	{
		if(!getParentFile().exists())
		{
			getParentFile().mkdirs();
		}
		
		return super.createNewFile();
	}
}
