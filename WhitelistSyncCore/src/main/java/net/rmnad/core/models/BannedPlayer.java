package net.rmnad.core.models;

public class BannedPlayer {
    private String uuid;
    private String name;
    private String created;
    private String source;
    private String expires;
    private String reason;

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

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExpires() {
        return expires;
    }

    public void setExpires(String expires) {
        this.expires = expires;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "BannedPlayer{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", created='" + created + '\'' +
                ", source='" + source + '\'' +
                ", expires='" + expires + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
