package com.norwood.mcheli.weapon;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

import org.jetbrains.annotations.NotNull;

import com.norwood.mcheli.wrapper.ChatMessageComponent;
import com.norwood.mcheli.wrapper.W_EntityPlayer;

public class MCH_DummyEntityPlayer extends W_EntityPlayer {

    public MCH_DummyEntityPlayer(World worldIn, EntityPlayer player) {
        super(worldIn, player);
    }

    public void sendMessage(@NotNull ITextComponent var1) {}

    public boolean canUseCommand(int var1, @NotNull String var2) {
        return false;
    }

    public @NotNull Entity getCommandSenderEntity() {
        return super.getCommandSenderEntity();
    }

    public boolean isSpectator() {
        return false;
    }

    public boolean isCreative() {
        return false;
    }

    public void sendChatToPlayer(ChatMessageComponent chatmessagecomponent) {}
}
