package dev.sixik.sdmtoolkit.forge;

import dev.sixik.sdmtoolkit.SDMToolkit;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SDMToolkit.MODID)
public final class SDMToolkitForge {
    public SDMToolkitForge() {
        EventBuses.registerModEventBus(SDMToolkit.MODID, FMLJavaModLoadingContext.get().getModEventBus());

        SDMToolkit.init();
    }
}
