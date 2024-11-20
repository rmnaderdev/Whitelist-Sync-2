package net.rmnad.core.models.api;

import net.rmnad.core.models.BannedPlayer;

import java.util.Objects;

public class BannedPlayerEntry {

    private int id;
    private int accountId;
    private String uuid;
    private String name;
    private String reason;
    private boolean isBanned;

    public BannedPlayerEntry() {
    }

    public BannedPlayerEntry(int id, int accountId, String uuid, String name) {
        this.id = id;
        this.accountId = accountId;
        this.uuid = uuid;
        this.name = name;
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
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", reason='" + reason + '\'' +
                ", isBanned=" + isBanned +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BannedPlayerEntry that = (BannedPlayerEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public BannedPlayer toBannedPlayer() {
        return new BannedPlayer(this.uuid, this.name, this.reason);
    }
}
