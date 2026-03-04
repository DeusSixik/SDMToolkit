package dev.sixik.sdmtoolkit;

import com.mojang.logging.LogUtils;
import dev.architectury.platform.Platform;
import dev.sixik.sdmtoolkit.network.AsyncBridge;
import dev.sixik.sdmtoolkit.network.BlobTransfer;
import net.fabricmc.api.EnvType;
import org.slf4j.Logger;

public final class SDMToolkit {
    public static final String MODID = "sdmtoolkit";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static void init() {
        if(Platform.getEnv() == EnvType.CLIENT) {
            AsyncBridge.initClient();
            BlobTransfer.initClient();
        } else {
            AsyncBridge.initServer();
            BlobTransfer.initServer();
        }
    }

}
