package com.tom.mcvideo.transcoder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyManager {
	/** The server properties object. */
	private final Properties serverProperties = new Properties();
	/** The server properties file. */
	private final File serverPropertiesFile;

	public PropertyManager(File propertiesFile)
	{
		this.serverPropertiesFile = propertiesFile;

		if (propertiesFile.exists())
		{
			FileInputStream fileinputstream = null;

			try
			{
				fileinputstream = new FileInputStream(propertiesFile);
				this.serverProperties.load(fileinputstream);
			}
			catch (Exception exception)
			{
				System.err.println("[PropertyMngr] Failed to load " + propertiesFile);
				exception.printStackTrace();
				this.generateNewProperties();
			}
			finally
			{
				if (fileinputstream != null)
				{
					try
					{
						fileinputstream.close();
					}
					catch (IOException var11)
					{
						;
					}
				}
			}
		}
		else
		{
			System.err.println("[PropertyMngr] " + propertiesFile + " does not exist");
			this.generateNewProperties();
		}
	}

	/**
	 * Generates a new properties file.
	 */
	public void generateNewProperties()
	{
		System.out.println("[PropertyMngr] Generating new properties file");
		this.saveProperties();
	}

	/**
	 * Writes the properties to the properties file.
	 */
	public void saveProperties()
	{
		FileOutputStream fileoutputstream = null;

		try
		{
			fileoutputstream = new FileOutputStream(this.serverPropertiesFile);
			this.serverProperties.store(fileoutputstream, "Server properties");
		}
		catch (Exception exception)
		{
			System.err.println("[PropertyMngr] Failed to save " + this.serverPropertiesFile);
			exception.printStackTrace();
			this.generateNewProperties();
		}
		finally
		{
			if (fileoutputstream != null)
			{
				try
				{
					fileoutputstream.close();
				}
				catch (IOException var10)
				{
					;
				}
			}
		}
	}

	/**
	 * Returns this PropertyManager's file object used for property saving.
	 */
	public File getPropertiesFile()
	{
		return this.serverPropertiesFile;
	}

	/**
	 * Returns a string property. If the property doesn't exist the default is returned.
	 */
	public String getStringProperty(String key, String defaultValue)
	{
		if (!this.serverProperties.containsKey(key))
		{
			this.serverProperties.setProperty(key, defaultValue);
			this.saveProperties();
		}

		return this.serverProperties.getProperty(key, defaultValue);
	}

	/**
	 * Gets an integer property. If it does not exist, set it to the specified value.
	 */
	public int getIntProperty(String key, int defaultValue)
	{
		try
		{
			return Integer.parseInt(this.getStringProperty(key, "" + defaultValue));
		}
		catch (Exception var4)
		{
			this.serverProperties.setProperty(key, "" + defaultValue);
			this.saveProperties();
			return defaultValue;
		}
	}

	public long getLongProperty(String key, long defaultValue)
	{
		try
		{
			return Long.parseLong(this.getStringProperty(key, "" + defaultValue));
		}
		catch (Exception var5)
		{
			this.serverProperties.setProperty(key, "" + defaultValue);
			this.saveProperties();
			return defaultValue;
		}
	}

	/**
	 * Gets a boolean property. If it does not exist, set it to the specified value.
	 */
	public boolean getBooleanProperty(String key, boolean defaultValue)
	{
		try
		{
			return Boolean.parseBoolean(this.getStringProperty(key, "" + defaultValue));
		}
		catch (Exception var4)
		{
			this.serverProperties.setProperty(key, "" + defaultValue);
			this.saveProperties();
			return defaultValue;
		}
	}

	/**
	 * Saves an Object with the given property name.
	 */
	public void setProperty(String key, Object value)
	{
		this.serverProperties.setProperty(key, "" + value);
	}

	public boolean hasProperty(String key)
	{
		return this.serverProperties.containsKey(key);
	}

	public void removeProperty(String key)
	{
		this.serverProperties.remove(key);
	}
}
