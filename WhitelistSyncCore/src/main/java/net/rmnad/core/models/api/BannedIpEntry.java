package net.rmnad.core.models.api;

import net.rmnad.core.models.BannedIp;

import java.util.Objects;

public class BannedIpEntry {

    private int id;
    private int accountId;
    private String ip;
    private String reason;
    private boolean isBanned;

    public BannedIpEntry() {
    }

    public BannedIpEntry(int id, int accountId, String ip) {
        this.id = id;
        this.accountId = accountId;
        this.ip = ip;
    }

    public int getId() {
        return id;
    }

    public void setId(int Id) {
        this.id = id;
    }

    public int getAccountId() {
        return accountId;
    }

    public void setAccountId(int accountId) {
        this.accountId = accountId;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public boolean getBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    @Override
    public String toString() {
        return "BannedPlayerEntry{" +
                "id=" + id +
                ", accountId=" + accountId +
                ", ip='" + ip + '\'' +
                ", reason='" + reason + '\'' +
                ", isBanned=" + isBanned +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BannedIpEntry that = (BannedIpEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public BannedIp toBannedIp() {
        return new BannedIp(this.ip, this.reason);
    }
}
