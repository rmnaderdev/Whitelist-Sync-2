package pw.twpi.whitelistsync2.models;


/**
 * DAO for a whitelisted user
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class WhitelistedPlayer {

    private String uuid;
    private String name;
    private boolean isWhitelisted;

    public WhitelistedPlayer() {
    }

    public WhitelistedPlayer(String uuid, String name, boolean isWhitelisted) {
        this.uuid = uuid;
        this.name = name;
        this.isWhitelisted = isWhitelisted;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isWhitelisted() {
        return isWhitelisted;
    }

    public void setWhitelisted(boolean whitelisted) {
        isWhitelisted = whitelisted;
    }

    @Override
    public String toString() {
        return "WhitelistedPlayer{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", isWhitelisted=" + isWhitelisted +
                '}';
    }
}
