package dev.sixik.sdmtoolkit.network;

public class AsyncServerTasks {

    public static final String SEND_SHOP_DATA = "send_shop_data";

    public static void init() {
        AsyncBridge.initServer();
        BlobTransfer.initServer();
    }
}
