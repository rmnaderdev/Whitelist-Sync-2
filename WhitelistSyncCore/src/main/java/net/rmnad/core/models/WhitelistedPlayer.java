package net.rmnad.core.models;
/**
 * DAO for a whitelisted user
 */
public class WhitelistedPlayer {

    private String uuid;
    private String name;

    public WhitelistedPlayer() {
    }

    public WhitelistedPlayer(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
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

    @Override
    public String toString() {
        return "WhitelistedPlayer{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
