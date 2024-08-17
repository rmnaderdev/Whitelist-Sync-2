package net.rmnad.models;


public class OppedPlayer {

    private boolean isOp;
    private String uuid;
    private String name;
    private int level;
    private boolean bypassesPlayerLimit;

    public OppedPlayer() {
    }

    public OppedPlayer(String uuid, String name, boolean isOp) {
        this.isOp = isOp;
        this.uuid = uuid;
        this.name = name;
        this.level = 5;
        this.bypassesPlayerLimit = false;
    }

    public OppedPlayer(String uuid, String name, boolean isOp, int level, boolean bypassesPlayerLimit) {
        this.isOp = isOp;
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
    }

    public boolean isOp() {
        return isOp;
    }

    public void setIsOp(boolean op) {
        isOp = op;
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
                "isOp=" + isOp +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", level='" + level + '\'' +
                ", bypassesPlayerLimit=" + bypassesPlayerLimit +
                '}';
    }
}
