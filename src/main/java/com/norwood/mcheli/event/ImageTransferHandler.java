package com.norwood.mcheli.event;

import com.norwood.mcheli.multiplay.MCH_MultiplayClient;

public class ImageTransferHandler {

    public static int sendLDCount = 0;
    private static final int IMAGE_SEND_INTERVAL = 10;

    protected static void handleImageDataSending() {
        if (++sendLDCount < IMAGE_SEND_INTERVAL) return;

        MCH_MultiplayClient.sendImageData();
        sendLDCount = 0;
    }
}
