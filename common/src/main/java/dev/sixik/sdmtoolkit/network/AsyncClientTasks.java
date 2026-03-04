package dev.sixik.sdmtoolkit.network;

public class AsyncClientTasks {

    public static void init() {
        AsyncBridge.initClient();
        BlobTransfer.initClient();
    }
}
