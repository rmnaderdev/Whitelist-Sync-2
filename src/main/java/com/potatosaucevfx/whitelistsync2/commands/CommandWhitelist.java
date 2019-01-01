package com.potatosaucevfx.whitelistsync2.commands;

import com.mojang.authlib.GameProfile;
import com.potatosaucevfx.whitelistsync2.Utilities;
import com.potatosaucevfx.whitelistsync2.WhitelistSync2;
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
 * Command class for whitelisting
 *
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class CommandWhitelist implements ICommand {

    private final ArrayList aliases;

    private final String USAGE_STRING = "/wl <list|add|remove|sync|copyServerToDatabase>";

    private final BaseService service;

    public CommandWhitelist(BaseService service) {
        this.service = service;
        aliases = new ArrayList();
        aliases.add("wl");
        aliases.add("whitelistsync");
    }

    @Override
    public String getName() {
        return "wl";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return USAGE_STRING;
    }

    @Override
    public List<String> getAliases() {
        return aliases;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (world.isRemote) {
            WhitelistSync2.logger.error("I don't run on client-side!");
        } 
        else {
            if (args.length > 0) {
                if (args.length > 0) {
                    //Action for showing list
                    if (args[0].equalsIgnoreCase("list")) {
                        
                        sender.sendMessage(new TextComponentString(Utilities.FormatWhitelistUsersOutput(service.pullWhitelistedNamesFromDatabase(server))));
                    
                    } // Actions for adding a player to whitelist
                    else if (args[0].equalsIgnoreCase("add")) {
                        if (args.length > 1) {
                            server.getPlayerList().addWhitelistedPlayer(server.getPlayerProfileCache().getGameProfileForUsername(args[1]));
                            service.addPlayerToDatabaseWhitelist(server.getPlayerProfileCache().getGameProfileForUsername(args[1]));
                            sender.sendMessage(new TextComponentString(args[1] + " added to the whitelist."));
                        } else {
                            sender.sendMessage(new TextComponentString("You must specify a name to add to the whitelist!"));
                        }
                    } // Actions for removing player from whitelist
                    else if (args[0].equalsIgnoreCase("remove")) {
                        if (args.length > 1) {
                            GameProfile gameprofile = server.getPlayerList().getWhitelistedPlayers().getByName(args[1]);
                            if (gameprofile != null) {
                                server.getPlayerList().removePlayerFromWhitelist(gameprofile);
                                service.removePlayerFromDatabaseWhitelist(gameprofile);
                                sender.sendMessage(new TextComponentString(args[1] + " removed from the whitelist."));
                            } else {
                                sender.sendMessage(new TextComponentString("You must specify a valid name to remove from the whitelist!"));
                            }
                        }
                    } // Sync Database to server
                    else if (args[0].equalsIgnoreCase("sync")) {
                        service.updateLocalWhitelistFromDatabase(server); // TODO: Add feedback
                    } // Sync server to database
                    else if (args[0].equalsIgnoreCase("copyservertodatabase")) {
                        service.pushLocalWhitelistToDatabase(server); // TODO: Add feedback
                    } 
                    else {
                        sender.sendMessage(new TextComponentString(USAGE_STRING));
                    }
                } 
                else {
                    sender.sendMessage(new TextComponentString(USAGE_STRING));
                }
            } 
            else {
                sender.sendMessage(new TextComponentString(USAGE_STRING));
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "wl");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
            String[] args, @Nullable BlockPos pos) {
        if (args.length == 1) {
            return CommandBase.getListOfStringsMatchingLastWord(args, "list", "add", "remove", "sync", "copyServerToDatabase");
        } else {
            if (args.length == 2) {
                if (args[0].equals("remove")) {
                    return CommandBase.getListOfStringsMatchingLastWord(args, server.getPlayerList().getWhitelistedPlayerNames());
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
