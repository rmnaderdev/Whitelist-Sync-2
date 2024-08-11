package net.rmnad.forge_1_12_2.commands;

import com.mojang.authlib.GameProfile;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListWhitelistEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.rmnad.Log;
import net.rmnad.Utilities;
import net.rmnad.forge_1_12_2.WhitelistSync2;
import net.rmnad.json.WhitelistedPlayersFileReader;
import net.rmnad.services.BaseService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
            Log.error("I don't run on client-side!");
        } else {
            if (args.length > 0) {
                if (args.length > 0) {
                    //Action for showing list
                    if (args[0].equalsIgnoreCase("list")) {

                        sender.sendMessage(new TextComponentString(Utilities.FormatWhitelistedPlayersOutput(service.getWhitelistedPlayersFromDatabase())));

                    } // Actions for adding a player to whitelist
                    else if (args[0].equalsIgnoreCase("add")) {

                        if (args.length > 1) {

                            GameProfile user = server.getPlayerProfileCache().getGameProfileForUsername(args[1]);

                            if (user != null) {

                                if (service.addWhitelistPlayer(user.getId(), user.getName())) {
                                    server.getPlayerList().addWhitelistedPlayer(user);
                                    sender.sendMessage(new TextComponentString(user.getName() + " added to the whitelist."));
                                } else {
                                    sender.sendMessage(new TextComponentString("Error adding " + user.getName() + " from whitelist!"));
                                }

                            } else {
                                sender.sendMessage(new TextComponentString("User " + args[1] + " not found!"));
                            }

                        } else {
                            sender.sendMessage(new TextComponentString("You must specify a name to add to the whitelist!"));
                        }
                    } // Actions for removing player from whitelist
                    else if (args[0].equalsIgnoreCase("remove")) {

                        if (args.length > 1) {

                            GameProfile user = server.getPlayerList().getWhitelistedPlayers().getByName(args[1]);
                            if (user != null) {

                                if (service.removeWhitelistPlayer(user.getId(), user.getName())) {
                                    server.getPlayerList().removePlayerFromWhitelist(user);
                                    sender.sendMessage(new TextComponentString(user.getName() + " removed from the whitelist."));
                                } else {
                                    sender.sendMessage(new TextComponentString("Error removing " + user.getName() + " from whitelist!"));
                                }

                            } else {
                                sender.sendMessage(new TextComponentString("You must specify a valid name to remove from the whitelist!"));
                            }

                        }

                    } // Sync Database to server
                    else if (args[0].equalsIgnoreCase("sync")) {

                        if (service.pullDatabaseWhitelistToLocal(
                                WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH),
                                (uuid, name) -> {
                                    server.getPlayerList().getWhitelistedPlayers().addEntry(new UserListWhitelistEntry(new GameProfile(uuid, name)));
                                },
                                (uuid, name) -> {
                                    server.getPlayerList().getWhitelistedPlayers().removeEntry(new GameProfile(uuid, name));
                                }
                        )) {
                            sender.sendMessage(new TextComponentString("Local up to date with database!"));
                        } else {
                            sender.sendMessage(new TextComponentString("Error syncing local to database!"));
                        }

                    } // Sync server to database
                    else if (args[0].equalsIgnoreCase("push")) {

                        if (service.pushLocalWhitelistToDatabase(WhitelistedPlayersFileReader.getWhitelistedPlayers(WhitelistSync2.SERVER_FILEPATH))) {
                            sender.sendMessage(new TextComponentString("Pushed local to database!"));
                        } else {
                            sender.sendMessage(new TextComponentString("Error pushing local to database!"));
                        }

                    } else {
                        sender.sendMessage(new TextComponentString(USAGE_STRING));
                    }
                } else {
                    sender.sendMessage(new TextComponentString(USAGE_STRING));
                }
            } else {
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