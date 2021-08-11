package pw.twpi.whitelistsync2.services;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.WhitelistEntry;
import pw.twpi.whitelistsync2.Config;
import pw.twpi.whitelistsync2.WhitelistSync2;
import pw.twpi.whitelistsync2.json.OppedPlayersFileUtilities;
import pw.twpi.whitelistsync2.json.WhitelistedPlayersFileUtilities;
import pw.twpi.whitelistsync2.models.OppedPlayer;
import pw.twpi.whitelistsync2.models.WhitelistedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for PostgreSQL databases
 *
 * @author Foxite <the@dirkkok.nl>
 */
public class PostgreSqlService implements BaseService {
	private final String url;
	private final String username;
	private final String password;

	public PostgreSqlService() {
		this.url = "jdbc:postgresql://" + Config.POSTGRESQL_IP.get() + ":" + Config.POSTGRESQL_PORT.get() + "/" + Config.POSTGRESQL_DB_NAME.get();
		this.username = Config.POSTGRESQL_USERNAME.get();
		this.password = Config.POSTGRESQL_PASSWORD.get();
	}

	@Override
	public boolean requiresSyncing() {
		return true;
	}

	private Connection getConnection() throws SQLException {
		Connection connection = null;

		try {
			Class.forName("org.postgresql.Driver"); // This executes the static constructor of the class, which registers it to JDBC (or something)
			connection = DriverManager.getConnection(this.url, this.username, this.password);
		} catch (Exception e) {
			throw new SQLException("Error connecting to PostgreSQL database. See the inner exception message for more information", e);
		}
		return connection;
	}

	@Override
	public boolean initializeDatabase() {
		try (Connection connection = getConnection()) {
			try (Statement stmt = connection.createStatement();
			     ResultSet countResult = stmt.executeQuery("SELECT COUNT(*) FROM public.whitelist LIMIT 1")) {
				if (countResult.next() && countResult.getInt(1) > 0) {
					WhitelistSync2.LOGGER.debug("The whitelist table contains " + stmt.getResultSet().getInt(1) + " items");
				} else {
					WhitelistSync2.LOGGER.info("The whitelist table is present but empty");
				}

			} catch (SQLException e) {
				WhitelistSync2.LOGGER.error("Caught exception while trying to count the whitelist table", e);

				if (e.getMessage().contains("relation \"whitelist\" does not exist")) {
					WhitelistSync2.LOGGER.info("Error appears to be caused by a missing table; creating it", e);
					try (Statement stmt = connection.createStatement()) { // Exception will be caught by outermost try block
						stmt.execute("CREATE TABLE public.whitelist (\n" +
								"    uuid uuid NOT NULL,\n" +
								"    playername character varying NOT NULL\n" +
								");");
					}

					WhitelistSync2.LOGGER.info("Whitelists table has been created");
				}
			}

			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while setting up database", e);
			return false;
		}
	}

	@Override
	public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromDatabase() {
		ArrayList<WhitelistedPlayer> ret = new ArrayList<>();
		try (Connection connection = getConnection();
		     Statement statement = connection.createStatement();
		     ResultSet result = statement.executeQuery("SELECT uuid, playername, is_whitelisted FROM public.whitelist")) {

			while (result.next()) {
				ret.add(new WhitelistedPlayer(result.getString("uuid"), result.getString("playername"), result.getBoolean("is_whitelisted")));
			}
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while reading database whitelist", e);
		}
		return ret;
	}

	@Override
	public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
		ArrayList<OppedPlayer> ret = new ArrayList<>();
		if (!Config.SYNC_OP_LIST.get()) {
			WhitelistSync2.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return ret;
		}
		try (Connection connection = getConnection();
		     Statement statement = connection.createStatement();
		     ResultSet result = statement.executeQuery("SELECT uuid, playername, is_opped FROM public.oplist")) {

			while (result.next()) {
				ret.add(new OppedPlayer(result.getString("uuid"), result.getString("playername"), result.getBoolean("is_opped")));
			}
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while reading database oplist", e);
		}

		return ret;
	}

	@Override
	public boolean copyLocalWhitelistedPlayersToDatabase() {
		// soft to/do: run this off-thread
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			ArrayList<WhitelistedPlayer> whitelistedPlayers = getWhitelistedPlayersFromLocal();
			StringBuilder sql = new StringBuilder("INSERT INTO public.whitelist(uuid, playername, is_whitelisted) VALUES ");
			boolean isNotFirst = false;
			for (WhitelistedPlayer wlp : whitelistedPlayers) {
				if (isNotFirst) {
					sql.append(", ");
				}
				sql.append(String.format("(%s, %s, %s)", wlp.getUuid(), wlp.getName(), wlp.isWhitelisted() ? "true" : "false"));
				isNotFirst = true;
			}
			stmt.executeUpdate(sql.toString());
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while copying local whitelist to database", e);
		}
		return false;
	}

	@Override
	public boolean copyLocalOppedPlayersToDatabase() {
		if (!Config.SYNC_OP_LIST.get()) {
			WhitelistSync2.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			ArrayList<OppedPlayer> whitelistedPlayers = getOppedPlayersFromLocal();
			StringBuilder sql = new StringBuilder("INSERT INTO public.oplist(uuid, playername, is_opped) VALUES ");
			boolean isNotFirst = false;
			for (OppedPlayer wlp : whitelistedPlayers) {
				if (isNotFirst) {
					sql.append(", ");
				}
				sql.append(String.format("(%s, %s, %s)", wlp.getUuid(), wlp.getName(), wlp.isOp() ? "true" : "false"));
				isNotFirst = true;
			}
			stmt.executeUpdate(sql.toString());
			stmt.close();
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while copying local oplist to database", e);
		}
		return false;
	}

	@Override
	public boolean copyDatabaseWhitelistedPlayersToLocal(MinecraftServer server) {
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT uuid, playername, is_whitelisted FROM public.whitelist")) {
			ArrayList<WhitelistedPlayer> localWhitelistedPlayers = WhitelistedPlayersFileUtilities.getWhitelistedPlayers();
			while (result.next()) {
				String uuid = result.getString("uuid");
				GameProfile player = new GameProfile(UUID.fromString(uuid), result.getString("playername"));
				if (result.getBoolean("is_whitelisted") && localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
					server.getPlayerList().getWhiteList().add(new WhitelistEntry(player));
				} else if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
					server.getPlayerList().getWhiteList().remove(new WhitelistEntry(player));
				}
			}
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while copying whitelist to local", e);
		}
		return false;
	}

	@Override
	public boolean copyDatabaseOppedPlayersToLocal(MinecraftServer server) {
		if (!Config.SYNC_OP_LIST.get()) {
			return false;Text is available under the Creative Commons Attribution-ShareAlike License; additional terms may apply. By using this site, you agree to the Terms of Use and Privacy Policy. Wikipedia® is a registered trademark of the Wikimedia Foundation, Inc., a non-profit organization.
		}
		try (Connection connection = getConnection()) {
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT uuid, playername, is_opped FROM public.oplist");

			ArrayList<OppedPlayer> localOppedPlayers = OppedPlayersFileUtilities.getOppedPlayers();
			int i = 0;
			while (result.next()) {
				i++;
				String uuid = result.getString("uuid");
				GameProfile player = new GameProfile(UUID.fromString(uuid), result.getString("playername"));
				if (result.getBoolean("is_opped") && localOppedPlayers.stream().noneMatch(op -> op.getUuid().equals(uuid))) {
					server.getPlayerList().op(player);
				} else if (localOppedPlayers.stream().anyMatch(op -> op.getUuid().equals(uuid))){
					server.getPlayerList().deop(player);
				}
			}
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while copying oplist to local", e);
		}
		return false;
	}

	@Override
	public boolean addWhitelistPlayer(GameProfile player) {
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			if (stmt.executeUpdate("UPDATE public.whitelist SET is_whitelisted = true WHERE uuid = '" + player.getId() + "'") == 0) {
				stmt.executeUpdate(String.format("INSERT INTO public.whitelist(uuid, playername, is_whitelisted) VALUES ('%s', '%s', true)", player.getId(), player.getName()));
			}
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while adding player to database whitelist", e);
			return false;
		}
	}

	@Override
	public boolean addOppedPlayer(GameProfile player) {
		if (!Config.SYNC_OP_LIST.get()) {
			WhitelistSync2.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}

		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			if (stmt.executeUpdate("UPDATE public.oplist SET is_opped = true WHERE uuid = '" + player.getId() + "'") == 0) {
				stmt.executeUpdate(String.format("INSERT INTO public.oplist(uuid, playername, is_opped) VALUES ('%s', '%s', true)", player.getId(), player.getName()));
			}
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while adding player to database oplist", e);
			return false;
		}
	}

	@Override
	public boolean removeWhitelistPlayer(GameProfile player) {
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			stmt.executeUpdate("UPDATE public.whitelist SET is_whitelisted = false WHERE uuid = '" + player.getId() + "'");
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while removing player from database whitelist", e);

			return false;
		}
	}

	@Override
	public boolean removeOppedPlayer(GameProfile player) {
		if (!Config.SYNC_OP_LIST.get()) {
			WhitelistSync2.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			stmt.executeUpdate("UPDATE public.oplist SET is_opped = false WHERE uuid = '" + player.getId() + "'");
			return true;
		} catch (SQLException e) {
			WhitelistSync2.LOGGER.error("Unexpected exception while removing player from database oplist", e);
			return false;
		}
	}

	@Override
	public ArrayList<WhitelistedPlayer> getWhitelistedPlayersFromLocal() {
		return WhitelistedPlayersFileUtilities.getWhitelistedPlayers();
	}

	@Override
	public ArrayList<OppedPlayer> getOppedPlayersFromLocal() {
		return OppedPlayersFileUtilities.getOppedPlayers();
	}
}