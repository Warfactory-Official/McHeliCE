package com.norwood.mcheli.networking.packet;

import static com.norwood.mcheli.multiplay.MultiplayerHandler.*;

import net.minecraft.entity.player.EntityPlayerMP;

import hohserg.elegant.networking.api.ClientToServerPacket;
import hohserg.elegant.networking.api.ElegantPacket;

@ElegantPacket// FIXME
public class PacketImgDataChunk implements ClientToServerPacket {

    public static final int PACKET_MAX_SIZE = 128;
    public int imageDataIndex = -1;
    public int imageDataSize = 0;
    public int imageDataTotalSize = 0;
    public byte[] buf;

    @Override
    public void onReceive(EntityPlayerMP player) {
        try {
            // Validate basic data
            if (this.imageDataIndex < 0 || this.imageDataTotalSize <= 0) return;

            // First chunk: initialize buffer
            if (this.imageDataIndex == 0) {
                if (imageData != null && !lastPlayerName.isEmpty()) {
                    LogError("[mcheli]Err1: Saving the %s screenshot to server FAILED!!!", lastPlayerName);
                }
                imageData = new byte[this.imageDataTotalSize];
                lastPlayerName = player.getDisplayName().getFormattedText();
                lastDataPercent = 0.0;
            }

            // If allocation failed, reset state and bail
            if (imageData == null) {
                resetState();
                return;
            }

            // Copy chunk into buffer
            if (this.imageDataSize > 0) {
                System.arraycopy(this.buf, 0, imageData, this.imageDataIndex, this.imageDataSize);
            }

            // Log progress every 10%
            double dataPercent = (double) (this.imageDataIndex + this.imageDataSize) / this.imageDataTotalSize * 100.0;
            if (dataPercent - lastDataPercent >= 10.0 || lastDataPercent == 0.0) {
                LogInfo(
                        "[mcheli]Saving %s screenshot to server. %.0f%% : %d / %d bytes",
                        player.getDisplayName(),
                        dataPercent,
                        this.imageDataIndex + this.imageDataSize,
                        this.imageDataTotalSize);
                lastDataPercent = dataPercent;
            }

            // Final chunk: save to disk
            if (this.imageDataIndex + this.imageDataSize >= this.imageDataTotalSize) {
                saveScreenshot(player.getDisplayName().toString(), imageData);
                resetState();
            }
        } catch (Exception e) {
            e.printStackTrace();
            resetState();
        }
    }
}
