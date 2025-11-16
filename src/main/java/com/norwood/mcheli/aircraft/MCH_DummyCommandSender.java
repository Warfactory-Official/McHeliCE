package com.norwood.mcheli.aircraft;

import com.norwood.mcheli.helper.MCH_Utils;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

public class MCH_DummyCommandSender implements ICommandSender {

    public static final MCH_DummyCommandSender instance = new MCH_DummyCommandSender();

    public static void execCommand(String s) {
        ICommandManager icommandmanager = MCH_Utils.getServer().getCommandManager();
        icommandmanager.executeCommand(instance, s);
    }

    public @NotNull String getName() {
        return "";
    }

    public @NotNull ITextComponent getDisplayName() {
        return null;
    }

    public void sendMessage(@NotNull ITextComponent component) {}

    public boolean canUseCommand(int permLevel, @NotNull String commandName) {
        return true;
    }

    public @NotNull World getEntityWorld() {
        return null;
    }

    public MinecraftServer getServer() {
        return MCH_Utils.getServer();
    }
}
