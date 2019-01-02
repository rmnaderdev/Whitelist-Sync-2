package com.potatosaucevfx.whitelistsync2.commands;

import com.mojang.authlib.GameProfile;
import com.potatosaucevfx.whitelistsync2.Utilities;
import com.potatosaucevfx.whitelistsync2.WhitelistSync2;
import com.potatosaucevfx.whitelistsync2.config.ConfigHandler;
import com.potatosaucevfx.whitelistsync2.services.BaseService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

/**
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class CommandOp implements ICommand {

    private final ArrayList aliases;

    private final String USAGE_STRING = "/wlop <list|op|deop|sync|copyServerToDatabase>";

    private final BaseService service;

    public CommandOp(BaseService service) {
        this.service = service;
        aliases = new ArrayList();
        aliases.add("wlop");
        aliases.add("whitelistsyncop");
    }

    @Override
    public String getCommandName() {
        return "wlop";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return USAGE_STRING;
    }

    @Override
    public List<String> getCommandAliases() {
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (world.isRemote) {
            WhitelistSync2.logger.error("I don't run on client-side!");
        } else {
            if (ConfigHandler.SYNC_OP_LIST) {
                if (args.length > 0) {
                    //Action for showing list
                    if (args[0].equalsIgnoreCase("list")) {

                        sender.addChatMessage(new TextComponentString(Utilities.FormatOpUsersOutput(service.pullOppedNamesFromDatabase(server))));

                    } // Actions for adding a player to whitelist
                    else if (args[0].equalsIgnoreCase("op")) {

                        if (args.length > 1) {

                            GameProfile player = server.getPlayerProfileCache().getGameProfileForUsername(args[1]);

                            if (player != null) {

                                if (service.addPlayerToDatabaseOp(player)) {
                                    server.getPlayerList().addOp(player);
                                    sender.addChatMessage(new TextComponentString(player.getName() + " opped!"));
                                } else {
                                    sender.addChatMessage(new TextComponentString("Error opping " + player.getName() + "!"));
                                }

                            } else {
                                sender.addChatMessage(new TextComponentString("User " + args[1] + " not found!"));
                            }

                        } else {
                            sender.addChatMessage(new TextComponentString("You must specify a name to op!"));
                        }

                    } // Actions for removing player from whitelist
                    else if (args[0].equalsIgnoreCase("deop")) {

                        if (args.length > 1) {

                            GameProfile gameprofile = server.getPlayerList().getOppedPlayers().getGameProfileFromName(args[1]);

                            if (gameprofile != null) {

                                if (service.removePlayerFromDatabaseOp(gameprofile)) {
                                    server.getPlayerList().removeOp(gameprofile);
                                    sender.addChatMessage(new TextComponentString(gameprofile.getName() + " de-opped!"));
                                } else {
                                    sender.addChatMessage(new TextComponentString("Error de-opping " + gameprofile.getName() + "!"));
                                }

                            } else {
                                sender.addChatMessage(new TextComponentString("User " + args[1] + " not found!"));
                            }

                        } else {
                            sender.addChatMessage(new TextComponentString("You must specify a valid name to deop!"));
                        }

                    } else if (args[0].equalsIgnoreCase("sync")) {

                        if (service.updateLocalOpListFromDatabase(server)) {
                            sender.addChatMessage(new TextComponentString("Local up to date with database!"));
                        } else {
                            sender.addChatMessage(new TextComponentString("Error syncing local to database!"));
                        }

                    } // Sync server to database
                    else if (args[0].equalsIgnoreCase("copyservertodatabase")) {

                        if (service.pushLocalOpListToDatabase(server)) {
                            sender.addChatMessage(new TextComponentString("Pushed local to database!"));
                        } else {
                            sender.addChatMessage(new TextComponentString("Error pushing local to database!"));
                        }

                    } else {
                        sender.addChatMessage(new TextComponentString(USAGE_STRING));
                    }
                } else {
                    sender.addChatMessage(new TextComponentString(USAGE_STRING));
                }
            } else {
                sender.addChatMessage(new TextComponentString("Whitelist Sync Op management is not enabled. You must enable it in the config."));
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canCommandSenderUseCommand(4, "wlop");
    }

    @Override
    public List<String> getTabCompletionOptions(MinecraftServer server, ICommandSender sender,
            String[] args,
            @Nullable BlockPos pos) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "list", "add", "remove", "sync", "copyServerToDatabase");
        } else {
            if (args.length == 2) {
                if (args[0].equals("remove")) {
                    return CommandBase.getListOfStringsMatchingLastWord(args, server.getPlayerList().getOppedPlayerNames());
                }

                if (args[0].equals("add")) {
                    return CommandBase.getListOfStringsMatchingLastWord(args, server.getPlayerProfileCache().getUsernames());
                }
            }
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isUsernameIndex(String[] args, int index) {
        return false;
    }

    @Override
    public int compareTo(ICommand o) {
        return 0;
    }

}
