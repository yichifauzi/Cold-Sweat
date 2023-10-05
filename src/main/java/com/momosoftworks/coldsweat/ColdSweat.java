package com.momosoftworks.coldsweat;

import com.momosoftworks.coldsweat.client.event.ClientJoinSetup;
import com.momosoftworks.coldsweat.client.gui.Overlays;
import com.momosoftworks.coldsweat.common.event.EntityTempManager;
import com.momosoftworks.coldsweat.config.ColdSweatConfig;
import com.momosoftworks.coldsweat.config.WorldSettingsConfig;
import com.momosoftworks.coldsweat.core.network.ColdSweatPacketHandler;
import com.momosoftworks.coldsweat.util.compat.CompatManager;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import com.momosoftworks.coldsweat.core.init.TempModifierInit;
import com.momosoftworks.coldsweat.util.world.TaskScheduler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import net.minecraftforge.common.MinecraftForge;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = ColdSweat.MOD_ID, version = ColdSweat.VERSION)
public class ColdSweat
{
    public static final String MOD_ID = "assets/cold_sweat";
    public static final String VERSION = "2.1.1";
    public static final Logger LOGGER = LogManager.getLogger();
    
    @EventHandler
    public void init(FMLInitializationEvent event)
    {
        regEventHandler(this);
        regEventHandler(new EntityTempManager());
        regEventHandler(new TaskScheduler());
        regEventHandler(new TempModifierInit());
        regEventHandler(new CompatManager());
        regEventHandler(new TaskScheduler());
        regEventHandler(new Overlays());
        regEventHandler(new ColdSweatConfig());
        regEventHandler(new WorldSettingsConfig());
        regEventHandler(new ClientJoinSetup());

        ColdSweatPacketHandler.CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(ColdSweatPacketHandler.NETWORK_ID);
        ColdSweatPacketHandler.registerMessages();
    }

    void regEventHandler(Object eventHandler)
    {   MinecraftForge.EVENT_BUS.register(eventHandler);
        FMLCommonHandler.instance().bus().register(eventHandler);
    }

    @EventHandler
    public static void preInit(FMLPreInitializationEvent event)
    {   String configDir = event.getModConfigurationDirectory().toString();
        ColdSweatConfig.init(configDir);
        WorldSettingsConfig.init(configDir);
    }

    @EventHandler
    public void serverStarted(FMLServerStartedEvent event)
    {   TempModifierInit.buildRegistries();
    }
}
