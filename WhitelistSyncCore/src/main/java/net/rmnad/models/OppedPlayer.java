package net.rmnad.models;


public class OppedPlayer {

    private boolean isOp;
    private String uuid;
    private String name;
    private int level;
    private Boolean bypassesPlayerLimit;

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

    public Boolean getBypassesPlayerLimit() {
        return bypassesPlayerLimit;
    }

    public void setBypassesPlayerLimit(Boolean bypassesPlayerLimit) {
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
