package pw.twpi.whitelistsync2.models;


/**
 * 
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class OpUser {

    private final boolean isOp;
    private final String uuid;
    private final String name;
    private final int level;
    private final boolean bypassesPlayerLimit;

    public OpUser(String uuid, String name, int level, boolean bypassesPlayerLimit, boolean isOp) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
        this.bypassesPlayerLimit = bypassesPlayerLimit;
        this.isOp = isOp;
    }

    public OpUser(OpUser opUser) {
        this.uuid = opUser.getUuid();
        this.name = opUser.getName();
        this.level = opUser.getLevel();
        this.bypassesPlayerLimit = opUser.isBypassesPlayerLimit();
        this.isOp = opUser.isOp;
    }

    public String getUuid() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public boolean isBypassesPlayerLimit() {
        return bypassesPlayerLimit;
    }

    public boolean isIsOp() {
        return isOp;
    }

    @Override
    public String toString() {
        return "OpUser{" + "isOp=" + isOp + ", uuid=" + uuid + ", name=" + name + ", level=" + level + ", bypassesPlayerLimit=" + bypassesPlayerLimit + '}';
    }
    
    
    
}
