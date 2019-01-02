package com.potatosaucevfx.whitelistsync2.services;

import com.google.gson.Gson;
import com.mojang.authlib.GameProfile;
import com.potatosaucevfx.whitelistsync2.WhitelistSync2;
import com.potatosaucevfx.whitelistsync2.config.ConfigHandler;
import com.potatosaucevfx.whitelistsync2.models.JsonResponse;
import com.potatosaucevfx.whitelistsync2.models.StatusResponse;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import static spark.Spark.*;

/**
 *
 * @author Richard Nader, Jr. <nader1rm@cmich.edu>
 */
public class MicroServiceThread implements Runnable {

    private final MinecraftServer server;
    private final BaseService service;

    private final String API_KEY;

    public MicroServiceThread(MinecraftServer server, BaseService service) {
        this.server = server;
        this.service = service;

        this.API_KEY = ConfigHandler.API_KEY;
    }

    @Override
    public void run() {
        initExceptionHandler((e) -> WhitelistSync2.logger.error("Error with micro service\n" + e.getMessage() + "\n" + e.getStackTrace()));
        port(ConfigHandler.API_PORT);

        // Mapping for testing apikey and server status
        get("/alive", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, "OK"));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });

        // Mapping for getting player count
        get("/players/count", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(server.getCurrentPlayerCount())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });

        // Mapping for getting list of player names
        get("/players/names", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                List<String> uuids = new ArrayList<>();
                for (GameProfile player : server.getOnlinePlayerProfiles()) {
                    uuids.add(player.getId().toString());
                }

                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(server.getPlayerList().getOnlinePlayerNames())));
            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });

        // Mapping for getting list of player uuids
        get("/players/uuids", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                List<String> uuids = new ArrayList<>();
                for (GameProfile player : server.getOnlinePlayerProfiles()) {
                    uuids.add(player.getId().toString());
                }
                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(uuids.toArray())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for getting list of whitelisted player names
        get("/whitelist/list/names", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                ArrayList<String> names = service.pullWhitelistedNamesFromDatabase(server);
                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(names.toArray())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for getting list of opped player names
        get("/op/list/names", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                ArrayList<String> names = service.pullOppedNamesFromDatabase(server);
                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(names.toArray())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for getting list of whitelisted player uuids
        get("/whitelist/list/uuids", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                ArrayList<String> names = service.pullWhitelistedUuidsFromDatabase(server);
                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(names.toArray())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for getting list of opped player uuids
        get("/op/list/uuids", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                ArrayList<String> names = service.pullOpUuidsFromDatabase(server);
                return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, new Gson().toJsonTree(names.toArray())));

            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });

        // Mapping for adding player to whitelist
        post("/whitelist/add", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                String name = req.queryParams("name");

                if (!name.isEmpty()) {
                    GameProfile user = server.getPlayerProfileCache().getGameProfileForUsername(name);

                    if (user != null) {
                        
                        if (service.addPlayerToDatabaseWhitelist(user)) {
                            
                            server.getPlayerList().addWhitelistedPlayer(user);
                            return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, user.getName() + " added to the whitelist."));
                            
                        } else {
                            return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Error adding " + user.getName() + " from whitelist!"));
                        }
                        
                    } else {
                        return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                    }

                } else {
                    return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                }
                
            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for removing player from whitelist
        post("/whitelist/remove", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                String name = req.queryParams("name");

                if (!name.isEmpty()) {
                    GameProfile user = server.getPlayerProfileCache().getGameProfileForUsername(name);

                    if (user != null) {
                        
                        if (service.removePlayerFromDatabaseWhitelist(user)) {
                            
                            server.getPlayerList().removePlayerFromWhitelist(user);
                            return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, user.getName() + " removed from the whitelist."));
                            
                        } else {
                            return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Error removing " + user.getName() + " from whitelist!"));
                        }
                        
                    } else {
                        return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                    }

                } else {
                    return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                }
                
            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for opping player
        post("/op/op", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                String name = req.queryParams("name");

                if (!name.isEmpty()) {
                    GameProfile user = server.getPlayerProfileCache().getGameProfileForUsername(name);

                    if (user != null) {
                        
                        if (service.addPlayerToDatabaseOp(user)) {
                            
                            server.getPlayerList().addOp(user);
                            return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, user.getName() + " opped!"));
                            
                        } else {
                            return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Error opping " + user.getName() + "!"));
                        }
                        
                    } else {
                        return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                    }

                } else {
                    return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                }
                
            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
        // Mapping for removing player from whitelist
        post("/op/deop", (req, res) -> {
            res.type("application/json");
            if (req.queryParams("apikey").equals(API_KEY)) {

                String name = req.queryParams("name");

                if (!name.isEmpty()) {
                    GameProfile user = server.getPlayerProfileCache().getGameProfileForUsername(name);

                    if (user != null) {
                        
                        if (service.removePlayerFromDatabaseOp(user)) {
                            
                            server.getPlayerList().removeOp(user);
                            return new Gson().toJson(new JsonResponse(StatusResponse.SUCCESS, user.getName() + " de-opped!"));
                            
                        } else {
                            return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Error de-opping " + user.getName() + "!"));
                        }
                        
                    } else {
                        return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                    }

                } else {
                    return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid Name"));
                }
                
            } else {
                return new Gson().toJson(new JsonResponse(StatusResponse.ERROR, "Invalid API Key"));
            }
        });
        
    }

}
