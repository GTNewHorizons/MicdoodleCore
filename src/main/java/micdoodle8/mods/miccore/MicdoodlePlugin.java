package micdoodle8.mods.miccore;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.LoaderException;
import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.versioning.DefaultArtifactVersion;
import cpw.mods.fml.common.versioning.VersionParser;
import cpw.mods.fml.relauncher.FMLInjectionData;
import cpw.mods.fml.relauncher.IFMLCallHook;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin.TransformerExclusions;
import net.minecraft.launchwrapper.Launch;
import net.minecraftforge.common.MinecraftForge;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

@TransformerExclusions(value = { "micdoodle8.mods.miccore" })
public class MicdoodlePlugin implements IFMLLoadingPlugin, IFMLCallHook
{
	public static boolean hasRegistered = false;
	public static final String mcVersion = "[1.7.2],[1.7.10]";
	public static File mcDir;
    public static File canonicalConfigDir;

    public MicdoodlePlugin()
    {
        DepLoader.load();
    }

	public static void versionCheck(String reqVersion, String mod)
	{
		final String mcVersion = (String) FMLInjectionData.data()[4];

		if (!VersionParser.parseRange(reqVersion).containsVersion(new DefaultArtifactVersion(mcVersion)))
		{
			final String err = "This version of " + mod + " does not support minecraft version " + mcVersion;
			System.err.println(err);

			final JEditorPane ep = new JEditorPane("text/html", "<html>" + err + "<br>Remove it from your mods folder and check <a href=\"https://github.com/GTNewHorizons/MicdoodleCore\">here</a> for updates" + "</html>");

			ep.setEditable(false);
			ep.setOpaque(false);
			ep.addHyperlinkListener(new HyperlinkListener()
			{
				@Override
				public void hyperlinkUpdate(HyperlinkEvent event)
				{
					try
					{
						if (event.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED))
						{
							Desktop.getDesktop().browse(event.getURL().toURI());
						}
					}
					catch (final Exception e)
					{
					}
				}
			});

			JOptionPane.showMessageDialog(null, ep, "Fatal error", JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
	}

	@Override
	public String[] getASMTransformerClass()
	{
		MicdoodlePlugin.versionCheck(MicdoodlePlugin.mcVersion, "MicdoodleCore");
		final String[] asmStrings = new String[] { "micdoodle8.mods.miccore.MicdoodleTransformer" };

		if (!MicdoodlePlugin.hasRegistered)
		{
			final List<String> asm = Arrays.asList(asmStrings);

			for (final String s : asm)
			{
				try
				{
					final Class<?> c = Class.forName(s);

					if (c != null)
					{
						System.out.println("Successfully Registered Transformer");
					}
				}
				catch (final Exception ex)
				{
					System.out.println("Error while running transformer " + s);
					return null;
				}
			}

			MicdoodlePlugin.hasRegistered = true;
		}

		return asmStrings;
	}

	@Override
	public String getModContainerClass()
	{
		return "micdoodle8.mods.miccore.MicdoodleModContainer";
	}

	@Override
	public String getSetupClass()
	{
		return "micdoodle8.mods.miccore.MicdoodlePlugin";
	}

	@Override
	public void injectData(Map<String, Object> data)
	{
		if (data.containsKey("mcLocation"))
		{
			MicdoodlePlugin.mcDir = (File) data.get("mcLocation");
            File configDir = new File(mcDir, "config");
            File modsDir = new File(mcDir, "mods");
        	String minecraftVersion = (String) FMLInjectionData.data()[4];
        	File subDir = new File(modsDir, minecraftVersion);
            String canonicalConfigPath;

            try
            {
                canonicalConfigPath = configDir.getCanonicalPath();
                canonicalConfigDir = configDir.getCanonicalFile();
            }
            catch (IOException ioe)
            {
                throw new LoaderException(ioe);
            }

            if (!canonicalConfigDir.exists())
            {
                FMLLog.fine("No config directory found, creating one: %s", canonicalConfigPath);
                boolean dirMade = canonicalConfigDir.mkdir();

                if (!dirMade)
                {
                    FMLLog.severe("Unable to create the config directory %s", canonicalConfigPath);
                    throw new LoaderException();
                }

                FMLLog.info("Config directory created successfully");
            }

            ConfigManagerMicCore.init();
		}

		System.out.println("[Micdoodle8Core]: " + "Patching game...");
	}

	@Override
	public Void call() throws Exception
	{
		return null;
	}

	private static Constructor<?> sleepCancelledConstructor;
	private static Constructor<?> orientCameraConstructor;
	private static String galacticraftCoreClass = "micdoodle8.mods.galacticraft.core.event.EventHandlerGC";

	public static void onSleepCancelled()
	{
		try
		{
			if (MicdoodlePlugin.sleepCancelledConstructor == null)
			{
				MicdoodlePlugin.sleepCancelledConstructor = Class.forName(MicdoodlePlugin.galacticraftCoreClass + "$SleepCancelledEvent").getConstructor();
			}

			MinecraftForge.EVENT_BUS.post((Event) MicdoodlePlugin.sleepCancelledConstructor.newInstance());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void orientCamera()
	{
		try
		{
			if (MicdoodlePlugin.orientCameraConstructor == null)
			{
				MicdoodlePlugin.orientCameraConstructor = Class.forName(MicdoodlePlugin.galacticraftCoreClass + "$OrientCameraEvent").getConstructor();
			}

			MinecraftForge.EVENT_BUS.post((Event) MicdoodlePlugin.orientCameraConstructor.newInstance());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public String getAccessTransformerClass()
	{
        boolean deobfuscated = true;

        try {
            deobfuscated = Launch.classLoader.getClassBytes("net.minecraft.world.World") != null;
        } catch (final Exception e) { }
        
        if (deobfuscated)
        {
        	return "micdoodle8.mods.miccore.MicdoodleAccessTransformerDeObf";
        }
        return "micdoodle8.mods.miccore.MicdoodleAccessTransformer";
	}
}
