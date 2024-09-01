package net.rmnad.models.api;

import net.rmnad.models.WhitelistedPlayer;

import java.util.Objects;

public class WhitelistEntry {

    private int id;
    private int accountId;
    private String uuid;
    private String name;
    private boolean isWhitelisted;

    public WhitelistEntry() {
    }

    public WhitelistEntry(int id, int accountId, String uuid, String name) {
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

    public boolean getWhitelisted() {
        return isWhitelisted;
    }

    public void setWhitelisted(boolean whitelisted) {
        isWhitelisted = whitelisted;
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", isWhitelisted=" + isWhitelisted +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhitelistEntry that = (WhitelistEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public WhitelistedPlayer toWhitelistedPlayer() {
        return new WhitelistedPlayer(this.uuid, this.name, this.isWhitelisted);
    }
}
