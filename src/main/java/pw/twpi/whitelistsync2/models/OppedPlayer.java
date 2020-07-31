package pw.twpi.whitelistsync2.models;


/**
 * 
 * @author Richard Nader, Jr. <rmnader@svsu.edu>
 */
public class OppedPlayer {

    private boolean isOp;
    private String uuid;
    private String name;

    public OppedPlayer() {
    }

    public OppedPlayer(String uuid, String name, boolean isOp) {
        this.uuid = uuid;
        this.name = name;
        this.isOp = isOp;
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

    @Override
    public String toString() {
        return "OppedPlayer{" +
                "isOp=" + isOp +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
