package com.norwood.mcheli.aircraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.norwood.mcheli.wrapper.W_SoundUpdater;

@SideOnly(Side.CLIENT)
public class MCH_SoundUpdater extends W_SoundUpdater {

    private final MCH_EntityAircraft theAircraft;
    private final EntityPlayerSP thePlayer;
    private boolean isMoving;
    private boolean silent;
    private float aircraftPitch;
    private float aircraftVolume;
    private float addPitch;
    private boolean isFirstUpdate;
    private double prevDist;
    private int soundDelay = 0;

    public MCH_SoundUpdater(Minecraft mc, MCH_EntityAircraft aircraft, EntityPlayerSP entityPlayerSP) {
        super(mc, aircraft);
        this.theAircraft = aircraft;
        this.thePlayer = entityPlayerSP;
        this.isFirstUpdate = true;
    }

    public void update() {
        if (!this.theAircraft.getSoundName().isEmpty() && this.theAircraft.getAcInfo() != null) {
            if (this.isFirstUpdate) {
                this.isFirstUpdate = false;
                this.initEntitySound(this.theAircraft.getSoundName());
            }

            MCH_AircraftInfo info = this.theAircraft.getAcInfo();
            boolean isBeforeMoving = this.isMoving;
            boolean isDead = this.theAircraft.isDead;
            if (isDead || !this.silent && this.aircraftVolume == 0.0F) {
                if (isDead) {
                    this.stopEntitySound(this.theAircraft);
                }

                this.silent = true;
                if (isDead) {
                    return;
                }
            }

            boolean isRide = this.theAircraft.getSeatIdByEntity(this.thePlayer) >= 0;
            boolean isPlaying = this.isEntitySoundPlaying(this.theAircraft);
            if (!isPlaying && this.aircraftVolume > 0.0F) {
                if (this.soundDelay > 0) {
                    this.soundDelay--;
                } else {
                    this.soundDelay = 20;
                    this.playEntitySound(this.theAircraft.getSoundName(), this.theAircraft, this.aircraftVolume,
                            this.aircraftPitch, true);
                }

                this.silent = false;
            }

            float prevVolume = this.aircraftVolume;
            float prevPitch = this.aircraftPitch;
            this.isMoving = info.soundVolume * this.theAircraft.getSoundVolume() >= 0.01;
            if (this.isMoving) {
                this.aircraftVolume = info.soundVolume * this.theAircraft.getSoundVolume();
                this.aircraftPitch = info.soundPitch * this.theAircraft.getSoundPitch();
                if (!isRide) {
                    double dist = this.thePlayer.getDistance(this.theAircraft.posX, this.thePlayer.posY,
                            this.theAircraft.posZ);
                    double pitch = this.prevDist - dist;
                    if (Math.abs(pitch) > 0.3) {
                        this.addPitch = (float) (this.addPitch + pitch / 40.0);
                        float maxAddPitch = 0.2F;
                        if (this.addPitch < -maxAddPitch) {
                            this.addPitch = -maxAddPitch;
                        }

                        if (this.addPitch > maxAddPitch) {
                            this.addPitch = maxAddPitch;
                        }
                    }

                    this.addPitch = (float) (this.addPitch * 0.9);
                    this.aircraftPitch = this.aircraftPitch + this.addPitch;
                    this.prevDist = dist;
                }

                if (this.aircraftPitch < 0.0F) {
                    this.aircraftPitch = 0.0F;
                }
            } else if (isBeforeMoving) {
                this.aircraftVolume = 0.0F;
                this.aircraftPitch = 0.0F;
            }

            if (!this.silent) {
                if (this.aircraftPitch != prevPitch) {
                    this.setEntitySoundPitch(this.theAircraft, this.aircraftPitch);
                }

                if (this.aircraftVolume != prevVolume) {
                    this.setEntitySoundVolume(this.theAircraft, this.aircraftVolume);
                }
            }

            boolean updateLocation;
            updateLocation = true;
            if (this.aircraftVolume > 0.0F) {
                if (isRide) {
                    this.updateSoundLocation(this.theAircraft);
                } else {
                    double px = this.thePlayer.posX;
                    double py = this.thePlayer.posY;
                    double pz = this.thePlayer.posZ;
                    double dx = this.theAircraft.posX - px;
                    double dy = this.theAircraft.posY - py;
                    double dz = this.theAircraft.posZ - pz;
                    double dist = info.soundRange / 16.0;
                    dx /= dist;
                    dy /= dist;
                    dz /= dist;
                    this.updateSoundLocation(px + dx, py + dy, pz + dz);
                }
            } else if (this.isEntitySoundPlaying(this.theAircraft)) {
                this.stopEntitySound(this.theAircraft);
            }
        }
    }
}
