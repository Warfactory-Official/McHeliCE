package com.norwood.mcheli.compat.oneprobe;

import com.norwood.mcheli.aircraft.MCH_EntityAircraft;
import com.norwood.mcheli.wrapper.modelloader.ModelVBO;
import io.netty.buffer.ByteBuf;
import mcjty.theoneprobe.TheOneProbe;
import mcjty.theoneprobe.api.*;
import mcjty.theoneprobe.apiimpl.styles.EntityStyle;
import mcjty.theoneprobe.network.NetworkTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import static com.norwood.mcheli.aircraft.AircraftGui.getTexturePath;
import static mcjty.theoneprobe.apiimpl.client.ElementEntityRender.fixEntityId;

@Optional.Interface(iface = "mcjty.theoneprobe.api.IProbeInfoProvider", modid = "theoneprobe")
public class AircraftInfoProvider implements IEntityDisplayOverride {

    @Optional.Method(modid = "theoneprobe")
    public static void register() {
        TheOneProbe.theOneProbeImp.registerEntityDisplayOverride(new AircraftInfoProvider());
        AircraftElement.implID = TheOneProbe.theOneProbeImp.registerElementFactory(AircraftElement::new);
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public boolean overrideStandardInfo(ProbeMode probeMode, IProbeInfo iProbeInfo, EntityPlayer entityPlayer,
                                        World world, Entity entity, IProbeHitEntityData iProbeHitEntityData) {
        if (entity instanceof MCH_EntityAircraft aircraft) {
            var info = aircraft.getAcInfo();
            if (info == null) return false;

            IProbeInfo root = iProbeInfo.vertical();
            IProbeInfo row = root.horizontal();

            row.element(new AircraftElement(aircraft, row.defaultEntityStyle()
                    .scale(info.oneProbeScale * 0.8f)
                    .width(100)
                    .height(100)));

            root.text(String.format("§e%s§r", aircraft.getName()));
            root.text(String.format("HP: %d / %d", aircraft.getHP(), aircraft.getMaxHP()));
            root.text(String.format("Speed: %.0f m/s", aircraft.getCurrentSpeed()));

            if (aircraft.getRiddenByEntity() != null) {
                root.text("Pilot: " + aircraft.getRiddenByEntity().getName());
                root.text(String.format("Weapon: %s",
                        aircraft.getCurrentWeapon(entityPlayer) != null ?
                                aircraft.getCurrentWeapon(entityPlayer).getDisplayName() : "None"));
            }

            return true;
        }
        return false;
    }

    public static class AircraftElement implements IElement {
        public static int implID;
        private final String entityName;
        private final Integer playerID;
        private final NBTTagCompound entityNBT;
        private final IEntityStyle style;

        @SuppressWarnings("unused")
        public AircraftElement(String entityName, IEntityStyle style) {
            this.entityName = entityName;
            this.entityNBT = null;
            this.style = style;
            this.playerID = null;
        }

        public AircraftElement(Entity entity, IEntityStyle style) {
            if (entity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) entity;
                this.entityNBT = null;
                this.playerID = player.getEntityId();
            } else {
                this.entityNBT = entity.serializeNBT();
                this.playerID = null;
            }

            this.entityName = EntityList.getEntityString(entity);
            this.style = style;
        }

        public AircraftElement(ByteBuf buf) {
            this.entityName = NetworkTools.readString(buf);
            this.style = (new EntityStyle()).width(buf.readInt()).height(buf.readInt()).scale(buf.readFloat());
            if (buf.readBoolean()) {
                this.entityNBT = NetworkTools.readNBT(buf);
            } else {
                this.entityNBT = null;
            }

            if (buf.readBoolean()) {
                this.playerID = buf.readInt();
            } else {
                this.playerID = null;
            }

        }

        private void renderAircraft(IEntityStyle style, int x, int y, MCH_EntityAircraft entity) {
            var info = entity.getAcInfo();
            assert entity.getAcInfo() != null;
            var model = (ModelVBO) entity.getAcInfo().model;
            var location = getTexturePath(entity);


            double safeWidth = Math.max(Math.max(model.sizeX, model.sizeZ), 0.001);
            double safeHeight = Math.max(model.sizeY, 0.001);

            double sclX = this.getWidth() / safeWidth;
            double sclY = this.getHeight() / safeHeight;
            double scl = 0.9 * Math.min(sclX, sclY);
            GlStateManager.pushMatrix();
            {
                GlStateManager.enableDepth();
                GlStateManager.enableLighting();
                RenderHelper.enableStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableTexture2D();

                double localCenterX = getWidth() / 2.0;
                double localCenterY = getHeight() / 2.0;
                GlStateManager.translate(localCenterX, localCenterY, 100.0);

                GlStateManager.scale(scl, -scl, scl);

                float angle = (System.currentTimeMillis() % 18000L) / 18000.0f * 360.0f;
                GlStateManager.rotate(angle, 0, 1, 0);

                double meshCenterX = (model.minX + model.maxX) / 2.0;
                double meshCenterY = (model.minY + model.maxY) / 2.0;
                double meshCenterZ = (model.minZ + model.maxZ) / 2.0;

                GlStateManager.translate(-meshCenterX, -meshCenterY, -meshCenterZ);
                Minecraft.getMinecraft().getTextureManager().bindTexture(location);
                model.renderStatic(info);

                RenderHelper.disableStandardItemLighting();
                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
            }
            GlStateManager.popMatrix();

        }

        public void render(int x, int y) {
            if (entityName != null && !entityName.isEmpty()) {
                Entity entity = null;
                if (entityNBT != null) {
                    entity = EntityList.createEntityFromNBT(entityNBT, Minecraft.getMinecraft().world);
                } else {
                    String fixed = fixEntityId(entityName);
                    EntityEntry value = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(fixed));
                    if (value != null) {
                        entity = value.newInstance(Minecraft.getMinecraft().world);
                    }
                }

                if (entity instanceof MCH_EntityAircraft entityAircraft && entityAircraft.getAcInfo() != null && entityAircraft.getAcInfo().model != null) {
                    renderAircraft(style, x, y, entityAircraft);
                }
            }

        }

        public int getWidth() {
            return this.style.getWidth();
        }

        public int getHeight() {
            return this.style.getHeight();
        }

        public void toBytes(ByteBuf buf) {
            NetworkTools.writeString(buf, this.entityName);
            buf.writeInt(this.style.getWidth());
            buf.writeInt(this.style.getHeight());
            buf.writeFloat(this.style.getScale());
            if (this.entityNBT != null) {
                buf.writeBoolean(true);
                NetworkTools.writeNBT(buf, this.entityNBT);
            } else {
                buf.writeBoolean(false);
            }

            if (this.playerID != null) {
                buf.writeBoolean(true);
                buf.writeInt(this.playerID);
            } else {
                buf.writeBoolean(false);
            }

        }

        public int getID() {
            return implID;
        }

    }


}
