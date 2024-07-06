package net.rmnad.whitelistsync2.models;

public class BannedIp {

    private String ip;
    private String created;
    private String source;
    private String expires;
    private String reason;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
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
        return "BannedIp{" +
                "ip='" + ip + '\'' +
                ", created='" + created + '\'' +
                ", source='" + source + '\'' +
                ", expires='" + expires + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
