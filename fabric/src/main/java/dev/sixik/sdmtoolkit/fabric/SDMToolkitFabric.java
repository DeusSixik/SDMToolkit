package dev.sixik.sdmtoolkit.fabric;

import dev.sixik.sdmtoolkit.SDMToolkit;
import net.fabricmc.api.ModInitializer;

public final class SDMToolkitFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SDMToolkit.init();
    }
}
