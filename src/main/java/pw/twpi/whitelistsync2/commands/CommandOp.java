package pw.twpi.whitelistsync2.commands;

import com.mojang.authlib.GameProfile;
import net.rmnad.minecraft.forge.whitelistsynclib.services.BaseService;
import pw.twpi.whitelistsync2.Utilities;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.config.Config;

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
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;

/**
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
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
    public String getName() {
        return "wlop";
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
            WhitelistSync2.LOGGER.error("I don't run on client-side!");
        } else {
            if (Config.SYNC_OP_LIST) {
                if (args.length > 0) {
                    //Action for showing list
                    if (args[0].equalsIgnoreCase("list")) {

                        sender.sendMessage(new TextComponentString(Utilities.FormatOppedPlayersOutput(service.getOppedPlayersFromDatabase())));

                    } // Actions for adding a player to whitelist
                    else if (args[0].equalsIgnoreCase("op")) {

                        if (args.length > 1) {

                            GameProfile player = server.getPlayerProfileCache().getGameProfileForUsername(args[1]);

                            if (player != null) {

                                if (service.addOppedPlayer(player.getId(), player.getName())) {
                                    server.getPlayerList().addOp(player);
                                    sender.sendMessage(new TextComponentString(player.getName() + " opped!"));
                                } else {
                                    sender.sendMessage(new TextComponentString("Error opping " + player.getName() + "!"));
                                }

                            } else {
                                sender.sendMessage(new TextComponentString("User " + args[1] + " not found!"));
                            }

                        } else {
                            sender.sendMessage(new TextComponentString("You must specify a name to op!"));
                        }

                    } // Actions for removing player from whitelist
                    else if (args[0].equalsIgnoreCase("deop")) {

                        if (args.length > 1) {

                            GameProfile player = server.getPlayerList().getOppedPlayers().getGameProfileFromName(args[1]);

                            if (player != null) {

                                if (service.removeOppedPlayer(player.getId(), player.getName())) {
                                    server.getPlayerList().removeOp(player);
                                    sender.sendMessage(new TextComponentString(player.getName() + " de-opped!"));
                                } else {
                                    sender.sendMessage(new TextComponentString("Error de-opping " + player.getName() + "!"));
                                }

                            } else {
                                sender.sendMessage(new TextComponentString("User " + args[1] + " not found!"));
                            }

                        } else {
                            sender.sendMessage(new TextComponentString("You must specify a valid name to deop!"));
                        }

                    } else if (args[0].equalsIgnoreCase("sync")) {

                        if (service.copyDatabaseOppedPlayersToLocal(
                                OppedPlayersFileUtilities.getOppedPlayers(),
                                (uuid, name)->{
                                    // Called when user added to op list
                                    server.getPlayerList().addOp(new GameProfile(uuid, name));
                                },
                                (uuid, name) -> {
                                    // Called when user removed from op list
                                    server.getPlayerList().removeOp(new GameProfile(uuid, name));
                                })) {
                            sender.sendMessage(new TextComponentString("Local up to date with database!"));
                        } else {
                            sender.sendMessage(new TextComponentString("Error syncing local to database!"));
                        }

                    } // Sync server to database
                    else if (args[0].equalsIgnoreCase("copyservertodatabase")) {

                        if (service.copyLocalOppedPlayersToDatabase(OppedPlayersFileUtilities.getOppedPlayers())) {
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
                sender.sendMessage(new TextComponentString("Whitelist Sync Op management is not enabled. You must enable it in the config."));
            }
        }
    }

    @Override
    public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
        return sender.canUseCommand(4, "wlop");
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender,
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
