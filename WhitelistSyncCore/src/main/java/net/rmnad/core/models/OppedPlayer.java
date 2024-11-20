package net.rmnad.core.models;


public class OppedPlayer {
    private String uuid;
    private String name;
    private int level;
    private boolean bypassesPlayerLimit;

    public OppedPlayer() {
    }

    public OppedPlayer(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
        this.level = 5;
        this.bypassesPlayerLimit = false;
    }

    public OppedPlayer(String uuid, String name, int level, boolean bypassesPlayerLimit) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
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

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public boolean getBypassesPlayerLimit() {
        return bypassesPlayerLimit;
    }

    public void setBypassesPlayerLimit(boolean bypassesPlayerLimit) {
        this.bypassesPlayerLimit = bypassesPlayerLimit;
    }

    @Override
    public String toString() {
        return "OppedPlayer{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", bypassesPlayerLimit=" + bypassesPlayerLimit +
                '}';
    }
}
