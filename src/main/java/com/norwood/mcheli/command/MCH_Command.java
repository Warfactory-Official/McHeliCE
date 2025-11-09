package com.norwood.mcheli.command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextComponent.Serializer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.event.CommandEvent;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import com.google.gson.JsonParseException;
import com.norwood.mcheli.MCH_BaseInfo;
import com.norwood.mcheli.MCH_Config;
import com.norwood.mcheli.MCH_MOD;
import com.norwood.mcheli.Tags;
import com.norwood.mcheli.helicopter.MCH_HeliInfo;
import com.norwood.mcheli.helper.MCH_Utils;
import com.norwood.mcheli.helper.info.ContentRegistries;
import com.norwood.mcheli.helper.info.ContentRegistry;
import com.norwood.mcheli.helper.info.emitters.IEmitter;
import com.norwood.mcheli.helper.info.emitters.YamlEmitter;
import com.norwood.mcheli.multiplay.MultiplayerHandler;
import com.norwood.mcheli.networking.packet.PacketHandleCommand;
import com.norwood.mcheli.networking.packet.PacketSyncServerSettings;
import com.norwood.mcheli.networking.packet.PacketTitle;
import com.norwood.mcheli.plane.MCH_PlaneInfo;
import com.norwood.mcheli.ship.MCH_ShipInfo;
import com.norwood.mcheli.tank.MCH_TankInfo;
import com.norwood.mcheli.throwable.MCH_ThrowableInfo;
import com.norwood.mcheli.vehicle.MCH_VehicleInfo;
import com.norwood.mcheli.weapon.MCH_WeaponInfo;

public class MCH_Command extends CommandBase {

    public static final String CMD_GET_SS = "sendss";
    public static final String CMD_MOD_LIST = "modlist";
    public static final String CMD_RECONFIG = "reconfig";
    public static final String CMD_TITLE = "title";
    public static final String CMD_FILL = "fill";
    public static final String CMD_STATUS = "status";
    public static final String CMD_KILL_ENTITY = "killentity";
    public static final String CMD_REMOVE_ENTITY = "removeentity";
    public static final String CMD_ATTACK_ENTITY = "attackentity";
    public static final String CMD_SHOW_BB = "showboundingbox";
    public static final String CMD_DELAY_BB = "delayhitbox";
    public static final String CMD_LIST = "list";
    public static final String CMD_DUMP_YAML = "dumpyaml";
    public static final String[] ALL_COMMAND = new String[] {
            "sendss", "modlist", "reconfig", "title", "fill", "status", "killentity", "removeentity", "attackentity",
            "showboundingbox", "list", "delayhitbox", "dumpyaml"
    };
    public static final MCH_Command instance = new MCH_Command();
    private static final Pattern sanitize = Pattern.compile("[^a-zA-Z0-9._-]");

    public static boolean canUseCommand(Entity player) {
        return player instanceof EntityPlayer && instance.canCommandSenderUseCommand(player);
    }

    public static boolean checkCommandPermission(MinecraftServer server, ICommandSender sender, String cmd) {
        if (new CommandGameMode().checkPermission(server, sender)) {
            return true;
        } else {
            if (sender instanceof EntityPlayer && !cmd.isEmpty()) {
                String playerName = ((EntityPlayer) sender).getGameProfile().getName();

                for (MCH_Config.CommandPermission c : MCH_Config.CommandPermissionList) {
                    if (c.name.equals(cmd)) {
                        for (String s : c.players) {
                            if (s.equalsIgnoreCase(playerName)) {
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }
    }

    public static void onCommandEvent(CommandEvent event) {
        if (event.getCommand() instanceof MCH_Command) {
            if (event.getParameters().length > 0 && !event.getParameters()[0].isEmpty()) {
                if (!checkCommandPermission(MCH_Utils.getServer(), event.getSender(), event.getParameters()[0])) {
                    event.setCanceled(true);
                    TextComponentTranslation c = new TextComponentTranslation("commands.generic.permission");
                    c.getStyle().setColor(TextFormatting.RED);
                    event.getSender().sendMessage(c);
                }
            } else {
                event.setCanceled(true);
            }
        }
    }

    public @NotNull String getName() {
        return Tags.MODID;
    }

    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return true;
    }

    public @NotNull String getUsage(@NotNull ICommandSender sender) {
        return "commands.com.norwood.mcheli.usage";
    }

    @Override
    public void execute(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                        String @NotNull [] prm) throws CommandException {
        if (!MCH_Config.EnableCommand.prmBool) return;
        if (prm.length == 0) throw new CommandException("Missing subcommand. Use /mcheli list");

        String subcommand = prm[0].toLowerCase(Locale.ROOT);
        if (!checkCommandPermission(server, sender, subcommand)) {
            TextComponentTranslation c = new TextComponentTranslation("commands.generic.permission");
            c.getStyle().setColor(TextFormatting.RED);
            sender.sendMessage(c);
            return;
        }

        switch (subcommand) {
            case "sendss" -> handleSendScreenshot(server, sender, prm);
            case "modlist" -> handleModList(server, sender, prm);
            case "reconfig" -> handleReconfig(sender, prm);
            case "title" -> handleTitle(prm);
            case "fill" -> executeFill(sender, prm);
            case "status" -> executeStatus(sender, prm);
            case "killentity" -> executeKillEntity(sender, prm);
            case "removeentity" -> executeRemoveEntity(sender, prm);
            case "attackentity" -> executeAttackEntity(sender, prm);
            case "showboundingbox" -> handleShowBoundingBox(sender, prm);
            case "list" -> handleList(sender);
            case "delayhitbox" -> handleDelayHitbox(sender, prm);
            case "dumpyaml" -> handleDumpYaml(sender, prm);
            default -> throw new CommandException("Unknown mcheli command. Please type /mcheli list");
        }
    }

    private void handleSendScreenshot(MinecraftServer server, ICommandSender sender,
                                      String[] prm) throws CommandException {
        if (prm.length != 2) throw new CommandException("Usage: /mcheli sendss playerName");
        EntityPlayerMP player = getPlayer(server, sender, prm[1]);
        PacketHandleCommand.send(player, PacketHandleCommand.CommandAction.NONE, prm[1]);
    }

    private void handleModList(MinecraftServer server, ICommandSender sender, String[] prm) throws CommandException {
        if (prm.length < 2) throw new CommandException("Usage: /mcheli modlist playerName...");
        EntityPlayerMP reqPlayer = sender instanceof EntityPlayerMP ? (EntityPlayerMP) sender : null;
        for (int i = 1; i < prm.length; i++) {
            EntityPlayerMP player = getPlayer(server, sender, prm[i]);
            PacketHandleCommand.send(player, PacketHandleCommand.CommandAction.REQUEST_MOD_INFO,
                    "" + MultiplayerHandler.getPlayerInfoId(reqPlayer));
        }
    }

    private void handleReconfig(ICommandSender sender, String[] prm) throws CommandException {
        if (prm.length != 1) throw new CommandException("Usage: /mcheli reconfig");
        MCH_MOD.proxy.reconfig();
        if (!sender.getEntityWorld().isRemote) {
            PacketSyncServerSettings.sendAll();
        }
        String msg = MCH_MOD.proxy.isSinglePlayer() ? "Reload com.norwood.mcheli.cfg" :
                "Reload server side com.norwood.mcheli.cfg";
        sender.sendMessage(new TextComponentString(msg));
    }

    private void handleTitle(String[] prm) throws CommandException {
        if (prm.length < 4)
            throw new WrongUsageException("Usage: /mcheli title time[1~180] position[0~4] message[JSON]");
        String s = buildString(prm, 3);

        int showTime = Math.max(1, Math.min(180, Integer.parseInt(prm[1])));
        int pos = Math.max(0, Math.min(5, Integer.parseInt(prm[2])));

        try {
            ITextComponent ichatcomponent = Serializer.jsonToComponent(s);
            new PacketTitle(
                    Serializer.componentToJson(ichatcomponent),
                    20 * showTime,
                    pos).sendToClients();
        } catch (JsonParseException ex) {
            Throwable root = ExceptionUtils.getRootCause(ex);
            throw new SyntaxErrorException("com.norwood.mcheli.title.jsonException",
                    root == null ? "" : root.getMessage());
        }
    }

    private void handleShowBoundingBox(ICommandSender sender, String[] prm) throws CommandException {
        if (prm.length != 2) throw new CommandException("Usage: /mcheli showboundingbox true|false");
        boolean enable = parseBoolean(prm[1]);
        MCH_Config.EnableDebugBoundingBox.prmBool = enable;
        PacketSyncServerSettings.sendAll();
        sender.sendMessage(new TextComponentString(enable ? "Enabled bounding box [F3 + b]" : "Disabled bounding box"));
        MCH_MOD.proxy.save();
    }

    private void handleList(ICommandSender sender) {
        String msg = String.join(", ", ALL_COMMAND);
        sender.sendMessage(new TextComponentString("/mcheli command list : " + msg));
    }

    private static String sanitizeFileName(String key) {
        return sanitize.matcher(key).replaceAll("_");
    }

    private void handleDumpYaml(ICommandSender sender, String[] prm) throws CommandException {
        // Usage: /mcheli dumpyaml [outputDir]
        String outDir = prm.length >= 2 ? prm[1] : "mcheli_yaml_dump";
        Path base = Paths.get(outDir);
        IEmitter emitter = new YamlEmitter();
        int written = 0;
        try {
            // Ensure base exists
            Files.createDirectories(base);

            // Dump each registry
            written += dumpRegistry(emitter, base.resolve("helicopters"), ContentRegistries.heli());
            written += dumpRegistry(emitter, base.resolve("planes"), ContentRegistries.plane());
            written += dumpRegistry(emitter, base.resolve("ships"), ContentRegistries.ship());
            written += dumpRegistry(emitter, base.resolve("tanks"), ContentRegistries.tank());
            written += dumpRegistry(emitter, base.resolve("vehicles"), ContentRegistries.vehicle());
            written += dumpWeapons(emitter, base.resolve("weapons"), ContentRegistries.weapon());
            written += dumpThrowable(emitter, base.resolve("throwable"), ContentRegistries.throwable());
            written += dumpHud(emitter, base.resolve("hud"), ContentRegistries.hud());

            sender.sendMessage(
                    new TextComponentString("Dumped " + written + " YAML files to " + base.toAbsolutePath()));
        } catch (IOException e) {
            throw new CommandException("Failed to dump YAML: " + e.getMessage());
        } catch (Exception e) {
            throw new CommandException("YAML emission error: " + e.getMessage());
        }
    }

    private int dumpRegistry(IEmitter emitter, Path dir,
                             ContentRegistry<? extends MCH_BaseInfo> reg) throws IOException {
        int count = 0;
        if (reg == null) return 0;
        Files.createDirectories(dir);
        for (Entry<String, ? extends MCH_BaseInfo> e : reg.entries()) {
            String key = sanitizeFileName(e.getKey());
            Path out = dir.resolve(key + ".yml");
            MCH_BaseInfo info = e.getValue();
            String content;
            if (info instanceof MCH_HeliInfo) {
                content = emitter.emitHelicopter((MCH_HeliInfo) info);
            } else if (info instanceof MCH_PlaneInfo) {
                content = emitter.emitPlane((MCH_PlaneInfo) info);
            } else if (info instanceof MCH_ShipInfo) {
                content = emitter.emitShip((MCH_ShipInfo) info);
            } else if (info instanceof MCH_TankInfo) {
                content = emitter.emitTank((MCH_TankInfo) info);
            } else if (info instanceof MCH_VehicleInfo) {
                content = emitter.emitVehicle((MCH_VehicleInfo) info);
            } else {
                continue;
            }
            YamlEmitter.writeTo(out, content);
            count++;
        }
        return count;
    }

    private int dumpWeapons(IEmitter emitter, Path dir, ContentRegistry<MCH_WeaponInfo> reg) throws IOException {
        int count = 0;
        if (reg == null) return 0;
        Files.createDirectories(dir);
        for (Entry<String, MCH_WeaponInfo> e : reg.entries()) {
            String key = sanitizeFileName(e.getKey());
            Path out = dir.resolve(key + ".yml");
            YamlEmitter.writeTo(out, emitter.emitWeapon(e.getValue()));
            count++;
        }
        return count;
    }

    private int dumpThrowable(IEmitter emitter, Path dir, ContentRegistry<MCH_ThrowableInfo> reg) throws IOException {
        int count = 0;
        if (reg == null) return 0;
        Files.createDirectories(dir);
        for (Entry<String, MCH_ThrowableInfo> e : reg.entries()) {
            String key = sanitizeFileName(e.getKey());
            Path out = dir.resolve(key + ".yml");
            YamlEmitter.writeTo(out, emitter.emitThrowable(e.getValue()));
            count++;
        }
        return count;
    }

    private int dumpHud(IEmitter emitter, Path dir,
                        ContentRegistry<com.norwood.mcheli.hud.MCH_Hud> reg) throws IOException {
        int count = 0;
        if (reg == null) return 0;
        Files.createDirectories(dir);
        for (Entry<String, com.norwood.mcheli.hud.MCH_Hud> e : reg.entries()) {
            String key = sanitizeFileName(e.getKey());
            Path out = dir.resolve(key + ".yml");
            YamlEmitter.writeTo(out, emitter.emitHud(e.getValue()));
            count++;
        }
        return count;
    }

    private void handleDelayHitbox(ICommandSender sender, String[] prm) throws CommandException {
        if (prm.length == 1) {
            sender.sendMessage(new TextComponentString(
                    "Current delay of hitbox = " + MCH_Config.HitBoxDelayTick.prmInt + " [0 ~ 50]"));
            return;
        }
        if (prm.length != 2) throw new CommandException("Usage: /mcheli delayhitbox 0 ~ 50");
        MCH_Config.HitBoxDelayTick.prmInt = Math.min(50, parseInt(prm[1]));
        MCH_MOD.proxy.save();
        sender.sendMessage(new TextComponentString(
                "Current delay of hitbox = " + MCH_Config.HitBoxDelayTick.prmInt + " [0 ~ 50]"));
    }

    private void executeAttackEntity(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length < 3) {
            throw new WrongUsageException(
                    "/mcheli attackentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive> <damage> [damage source]");
        } else {
            String className = args[1].toLowerCase();
            float damage = Float.parseFloat(args[2]);
            String damageName = args.length >= 4 ? args[3].toLowerCase() : "";
            DamageSource ds = DamageSource.GENERIC;
            if (!damageName.isEmpty()) {
                switch (damageName) {
                    case "player" -> {
                        if (sender instanceof EntityPlayer) {
                            ds = DamageSource.causePlayerDamage((EntityPlayer) sender);
                        }
                    }
                    case "anvil" -> ds = DamageSource.ANVIL;
                    case "cactus" -> ds = DamageSource.CACTUS;
                    case "drown" -> ds = DamageSource.DROWN;
                    case "fall" -> ds = DamageSource.FALL;
                    case "fallingblock" -> ds = DamageSource.FALLING_BLOCK;
                    case "generic" -> ds = DamageSource.GENERIC;
                    case "infire" -> ds = DamageSource.IN_FIRE;
                    case "inwall" -> ds = DamageSource.IN_WALL;
                    case "lava" -> ds = DamageSource.LAVA;
                    case "magic" -> ds = DamageSource.MAGIC;
                    case "onfire" -> ds = DamageSource.ON_FIRE;
                    case "starve" -> ds = DamageSource.STARVE;
                    case "wither" -> ds = DamageSource.WITHER;
                }
            }

            int attacked = 0;
            List<Entity> list = sender.getEntityWorld().loadedEntityList;

            for (Entity entity : list) {
                if (entity != null && !(entity instanceof EntityPlayer) &&
                        entity.getClass().getName().toLowerCase().contains(className)) {
                    entity.attackEntityFrom(ds, damage);
                    attacked++;
                }
            }

            sender.sendMessage(
                    new TextComponentString(attacked + " entity attacked(" + args[1] + ", damage=" + damage + ")."));
        }
    }

    private void executeKillEntity(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length < 2) {
            throw new WrongUsageException(
                    "/mcheli killentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive>");
        } else {
            String className = args[1].toLowerCase();
            int killed = 0;
            List<Entity> list = sender.getEntityWorld().loadedEntityList;

            for (Entity entity : list) {
                if (entity != null && !(entity instanceof EntityPlayer) &&
                        entity.getClass().getName().toLowerCase().contains(className)) {
                    entity.setDead();
                    killed++;
                }
            }

            sender.sendMessage(new TextComponentString(killed + " entity killed(" + args[1] + ")."));
        }
    }

    private void executeRemoveEntity(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length < 2) {
            throw new WrongUsageException(
                    "/mcheli removeentity <entity class name : example1 EntityBat , example2 minecraft.entity.passive>");
        } else {
            String className = args[1].toLowerCase();
            List<Entity> list = sender.getEntityWorld().loadedEntityList;
            int removed = 0;

            for (Entity entity : list) {
                if (entity != null && !(entity instanceof EntityPlayer) &&
                        entity.getClass().getName().toLowerCase().contains(className)) {
                    entity.isDead = true;
                    removed++;
                }
            }

            sender.sendMessage(new TextComponentString(removed + " entity removed(" + args[1] + ")."));
        }
    }

    private void executeStatus(ICommandSender sender, String[] args) throws WrongUsageException {
        if (args.length < 2) {
            throw new WrongUsageException("/mcheli status <entity or tile> [min num]");
        } else {
            if (args[1].equalsIgnoreCase("entity")) {
                this.executeStatusSub(sender, args, "Server loaded Entity List",
                        sender.getEntityWorld().loadedEntityList);
            } else if (args[1].equalsIgnoreCase("tile")) {
                this.executeStatusSub(sender, args, "Server loaded Tile Entity List",
                        sender.getEntityWorld().loadedTileEntityList);
            }
        }
    }

    private void executeStatusSub(ICommandSender sender, String[] args, String title, List<?> list) {
        int minNum = args.length >= 3 ? Integer.parseInt(args[2]) : 0;
        HashMap<String, Integer> map = new HashMap<>();

        for (Object o : list) {
            String key = o.getClass().getName();
            if (map.containsKey(key)) {
                map.put(key, map.get(key) + 1);
            } else {
                map.put(key, 1);
            }
        }

        List<Entry<String, Integer>> entries = new ArrayList<>(map.entrySet());
        entries.sort(Comparator.comparing(Entry::getKey));
        boolean send = false;
        sender.sendMessage(new TextComponentString("--- " + title + " ---"));

        for (Entry<String, Integer> s : entries) {
            if (s.getValue() >= minNum) {
                String msg = " " + s.getKey() + " : " + s.getValue();
                System.out.println(msg);
                sender.sendMessage(new TextComponentString(msg));
                send = true;
            }
        }

        if (!send) {
            System.out.println("none");
            sender.sendMessage(new TextComponentString("none"));
        }
    }

    public void executeFill(ICommandSender sender, String[] args) throws CommandException {
        if (args.length < 8) {
            throw new WrongUsageException(
                    "/mcheli fill <x1> <y1> <z1> <x2> <y2> <z2> <block name> [meta data] [oldBlockHandling] [data tag]");
        } else {
            int x1 = sender.getPosition().getX();
            int y1 = sender.getPosition().getY();
            int z1 = sender.getPosition().getZ();
            int x2 = sender.getPosition().getX();
            int y2 = sender.getPosition().getY();
            int z2 = sender.getPosition().getZ();
            x1 = MathHelper.floor(parseCoordinate(x1, args[1], true).getResult());
            y1 = MathHelper.floor(parseCoordinate(y1, args[2], true).getResult());
            z1 = MathHelper.floor(parseCoordinate(z1, args[3], true).getResult());
            x2 = MathHelper.floor(parseCoordinate(x2, args[4], true).getResult());
            y2 = MathHelper.floor(parseCoordinate(y2, args[5], true).getResult());
            z2 = MathHelper.floor(parseCoordinate(z2, args[6], true).getResult());
            Block block = CommandBase.getBlockByText(sender, args[7]);
            IBlockState iblockstate = block.getDefaultState();
            if (args.length >= 9) {
                iblockstate = convertArgToBlockState(block, args[8]);
            }

            World world = sender.getEntityWorld();
            if (x1 > x2) {
                int t = x1;
                x1 = x2;
                x2 = t;
            }

            if (y1 > y2) {
                int t = y1;
                y1 = y2;
                y2 = t;
            }

            if (z1 > z2) {
                int t = z1;
                z1 = z2;
                z2 = t;
            }

            if (y1 >= 0 && y2 < 256) {
                int blockNum = (x2 - x1 + 1) * (y2 - y1 + 1) * (z2 - z1 + 1);
                if (blockNum > 3000000) {
                    throw new CommandException("commands.setblock.tooManyBlocks " + blockNum + " limit=327680",
                            blockNum, 3276800);
                } else {
                    boolean result = false;
                    boolean keep = args.length >= 10 && args[9].equals("keep");
                    boolean destroy = args.length >= 10 && args[9].equals("destroy");
                    boolean override = args.length >= 10 && args[9].equals("override");
                    NBTTagCompound nbttagcompound = new NBTTagCompound();
                    boolean flag = false;
                    if (args.length >= 11 && block.hasTileEntity(iblockstate)) {
                        String s = getChatComponentFromNthArg(sender, args, 10).getUnformattedText();

                        try {

                            nbttagcompound = JsonToNBT.getTagFromJson(s);
                            flag = true;
                        } catch (NBTException var27) {
                            throw new CommandException("commands.setblock.tagError", var27.getMessage());
                        }
                    }

                    for (int x = x1; x <= x2; x++) {
                        for (int y = y1; y <= y2; y++) {
                            for (int z = z1; z <= z2; z++) {
                                BlockPos blockpos = new BlockPos(x, y, z);
                                if (world.isBlockLoaded(blockpos) && (world.isAirBlock(blockpos) ? !override : !keep)) {
                                    if (destroy) {
                                        world.destroyBlock(blockpos, false);
                                    }

                                    TileEntity block2 = world.getTileEntity(blockpos);
                                    if (block2 instanceof IInventory ii) {

                                        for (int i = 0; i < ii.getSizeInventory(); i++) {
                                            ItemStack is = ii.removeStackFromSlot(i);
                                            if (!is.isEmpty()) {
                                                is.setCount(0);
                                            }
                                        }
                                    }

                                    if (world.setBlockState(blockpos, iblockstate, 3)) {
                                        if (flag) {
                                            TileEntity tileentity = world.getTileEntity(blockpos);
                                            if (tileentity != null) {
                                                nbttagcompound.setInteger("x", x);
                                                nbttagcompound.setInteger("y", y);
                                                nbttagcompound.setInteger("z", z);
                                                tileentity.readFromNBT(nbttagcompound);
                                            }
                                        }

                                        result = true;
                                    }
                                }
                            }
                        }
                    }

                    if (result) {
                        notifyCommandListener(sender, this, "commands.setblock.success");
                    } else {
                        throw new CommandException("commands.setblock.noChange");
                    }
                }
            } else {
                throw new CommandException("commands.setblock.outOfWorld");
            }
        }
    }

    public @NotNull List<String> getTabCompletions(@NotNull MinecraftServer server, @NotNull ICommandSender sender,
                                                   String @NotNull [] prm, BlockPos targetPos) {
        if (!MCH_Config.EnableCommand.prmBool) {
            return super.getTabCompletions(server, sender, prm, targetPos);
        } else if (prm.length <= 1) {
            return getListOfStringsMatchingLastWord(prm, ALL_COMMAND);
        } else {
            if (prm[0].equalsIgnoreCase("sendss")) {
                if (prm.length == 2) {
                    return getListOfStringsMatchingLastWord(prm, server.getOnlinePlayerNames());
                }
            } else if (prm[0].equalsIgnoreCase("modlist")) {
                return getListOfStringsMatchingLastWord(prm, server.getOnlinePlayerNames());
            } else {
                if (prm[0].equalsIgnoreCase("fill")) {
                    if ((prm.length == 2 || prm.length == 5) && sender instanceof Entity entity) {
                        List<String> a = new ArrayList<>();
                        int x = entity.posX < 0.0 ? (int) (entity.posX - 1.0) : (int) entity.posX;
                        int z = entity.posZ < 0.0 ? (int) (entity.posZ - 1.0) : (int) entity.posZ;
                        a.add(x + " " + (int) (entity.posY + 0.5) + " " + z);
                        return a;
                    }

                    return prm.length == 10 ?
                            getListOfStringsMatchingLastWord(prm, "replace", "destroy", "keep", "override") :
                            (prm.length == 8 ? getListOfStringsMatchingLastWord(prm, Block.REGISTRY.getKeys()) : null);
                }

                if (prm[0].equalsIgnoreCase("status")) {
                    if (prm.length == 2) {
                        return getListOfStringsMatchingLastWord(prm, "entity", "tile");
                    }
                } else if (prm[0].equalsIgnoreCase("attackentity")) {
                    if (prm.length == 4) {
                        return getListOfStringsMatchingLastWord(
                                prm,
                                "player",
                                "inFire",
                                "onFire",
                                "lava",
                                "inWall",
                                "drown",
                                "starve",
                                "cactus",
                                "fall",
                                "outOfWorld",
                                "generic",
                                "magic",
                                "wither",
                                "anvil",
                                "fallingBlock");
                    }
                } else if (prm[0].equalsIgnoreCase("showboundingbox") && prm.length == 2) {
                    return getListOfStringsMatchingLastWord(prm, "true", "false");
                }
            }

            return super.getTabCompletions(server, sender, prm, targetPos);
        }
    }
}
