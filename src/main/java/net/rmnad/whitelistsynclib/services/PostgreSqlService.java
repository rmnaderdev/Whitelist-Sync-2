package net.rmnad.whitelistsynclib.services;

import net.rmnad.whitelistsynclib.WhitelistSyncLib;
import net.rmnad.whitelistsynclib.callbacks.IOnUserAdd;
import net.rmnad.whitelistsynclib.callbacks.IOnUserRemove;
import net.rmnad.whitelistsynclib.models.OppedPlayer;
import net.rmnad.whitelistsynclib.models.WhitelistedPlayer;

import java.sql.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * Service for PostgreSQL databases
 *
 * @author Foxite <the@dirkkok.nl>
 */
public class PostgreSqlService implements BaseService {

	private final boolean syncingOpList;
	
	private final String url;
	private final String username;
	private final String password;

	public PostgreSqlService(String databaseName, String ip, int port, String username, String password, boolean syncingOpList) {
		this.url = "jdbc:postgresql://" + ip + ":" + port + "/" + databaseName;
		this.username = username;
		this.password = password;
		
		this.syncingOpList = syncingOpList;
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
					WhitelistSyncLib.LOGGER.debug("The whitelist table contains " + stmt.getResultSet().getInt(1) + " items");
				} else {
					WhitelistSyncLib.LOGGER.info("The whitelist table is present but empty");
				}

			} catch (SQLException e) {
				WhitelistSyncLib.LOGGER.error("Caught exception while trying to count the whitelist table", e);

				if (e.getMessage().contains("relation \"whitelist\" does not exist")) {
					WhitelistSyncLib.LOGGER.info("Error appears to be caused by a missing table; creating it", e);
					try (Statement stmt = connection.createStatement()) { // Exception will be caught by outermost try block
						stmt.execute("CREATE TABLE public.whitelist (\n" +
								"    uuid uuid NOT NULL,\n" +
								"    playername character varying NOT NULL\n" +
								");");
					}

					WhitelistSyncLib.LOGGER.info("Whitelists table has been created");
				}
			}

			return true;
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while setting up database", e);
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
			WhitelistSyncLib.LOGGER.error("Unexpected exception while reading database whitelist", e);
		}
		return ret;
	}

	@Override
	public ArrayList<OppedPlayer> getOppedPlayersFromDatabase() {
		ArrayList<OppedPlayer> ret = new ArrayList<>();
		if (!this.syncingOpList) {
			WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return ret;
		}
		try (Connection connection = getConnection();
		     Statement statement = connection.createStatement();
		     ResultSet result = statement.executeQuery("SELECT uuid, playername, is_opped FROM public.oplist")) {

			while (result.next()) {
				ret.add(new OppedPlayer(result.getString("uuid"), result.getString("playername"), result.getBoolean("is_opped")));
			}
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while reading database oplist", e);
		}

		return ret;
	}

	@Override
	public boolean copyLocalWhitelistedPlayersToDatabase(ArrayList<WhitelistedPlayer> whitelistedPlayers) {
		// soft to/do: run this off-thread
		try (Connection connection = getConnection();
			 Statement stmt = connection.createStatement()) {
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
			WhitelistSyncLib.LOGGER.error("Unexpected exception while copying local whitelist to database", e);
		}
		return false;
	}

	@Override
	public boolean copyLocalOppedPlayersToDatabase(ArrayList<OppedPlayer> oppedPlayers) {
		if (!this.syncingOpList) {
			WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			StringBuilder sql = new StringBuilder("INSERT INTO public.oplist(uuid, playername, is_opped) VALUES ");
			boolean isNotFirst = false;
			for (OppedPlayer wlp : oppedPlayers) {
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
			WhitelistSyncLib.LOGGER.error("Unexpected exception while copying local oplist to database", e);
		}
		return false;
	}

	@Override
	public boolean copyDatabaseWhitelistedPlayersToLocal(ArrayList<WhitelistedPlayer> localWhitelistedPlayers, IOnUserAdd onUserAdd, IOnUserRemove onUserRemove) {
		try (Connection connection = getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT uuid, playername, is_whitelisted FROM public.whitelist")) {
			while (result.next()) {
				UUID uuid = UUID.fromString(result.getString("uuid"));
				String name = result.getString("playername");

				if (result.getBoolean("is_whitelisted") && localWhitelistedPlayers.stream().noneMatch(o -> o.getUuid().equals(uuid))) {
					onUserAdd.call(uuid, name);
				} else if (localWhitelistedPlayers.stream().anyMatch(o -> o.getUuid().equals(uuid))) {
					onUserRemove.call(uuid, name);
				}
			}
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while copying whitelist to local", e);
		}
		return false;
	}

	@Override
	public boolean copyDatabaseOppedPlayersToLocal(ArrayList<OppedPlayer> localOppedPlayers, IOnUserAdd onUserAdd, IOnUserRemove onUserRemove) {
		if (!this.syncingOpList) {
			return false;
		}
		try (Connection connection = getConnection()) {
			 Statement statement = connection.createStatement();
			 ResultSet result = statement.executeQuery("SELECT uuid, playername, is_opped FROM public.oplist");

			int i = 0;
			while (result.next()) {
				i++;
				UUID uuid = UUID.fromString(result.getString("uuid"));
				String name = result.getString("playername");

				if (result.getBoolean("is_opped") && localOppedPlayers.stream().noneMatch(op -> op.getUuid().equals(uuid))) {
					onUserAdd.call(uuid, name);
				} else if (localOppedPlayers.stream().anyMatch(op -> op.getUuid().equals(uuid))){
					onUserRemove.call(uuid, name);
				}
			}
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while copying oplist to local", e);
		}
		return false;
	}

	@Override
	public boolean addWhitelistPlayer(UUID uuid, String name) {
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			if (stmt.executeUpdate("UPDATE public.whitelist SET is_whitelisted = true WHERE uuid = '" + uuid.toString() + "'") == 0) {
				stmt.executeUpdate(String.format("INSERT INTO public.whitelist(uuid, playername, is_whitelisted) VALUES ('%s', '%s', true)", uuid.toString(), name));
			}
			return true;
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while adding player to database whitelist", e);
			return false;
		}
	}

	@Override
	public boolean addOppedPlayer(UUID uuid, String name) {
		if (!this.syncingOpList) {
			WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}

		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			if (stmt.executeUpdate("UPDATE public.oplist SET is_opped = true WHERE uuid = '" + uuid.toString() + "'") == 0) {
				stmt.executeUpdate(String.format("INSERT INTO public.oplist(uuid, playername, is_opped) VALUES ('%s', '%s', true)", uuid.toString(), name));
			}
			return true;
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while adding player to database oplist", e);
			return false;
		}
	}

	@Override
	public boolean removeWhitelistPlayer(UUID uuid, String name) {
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			stmt.executeUpdate("UPDATE public.whitelist SET is_whitelisted = false WHERE uuid = '" + uuid.toString() + "'");
			return true;
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while removing player from database whitelist", e);

			return false;
		}
	}

	@Override
	public boolean removeOppedPlayer(UUID uuid, String name) {
		if (!this.syncingOpList) {
			WhitelistSyncLib.LOGGER.error("Op list syncing is currently disabled in your config. Please enable it and restart the server to use this feature.");
			return false;
		}
		try (Connection connection = getConnection();
		     Statement stmt = connection.createStatement()) {
			stmt.executeUpdate("UPDATE public.oplist SET is_opped = false WHERE uuid = '" + uuid.toString() + "'");
			return true;
		} catch (SQLException e) {
			WhitelistSyncLib.LOGGER.error("Unexpected exception while removing player from database oplist", e);
			return false;
		}
	}

}