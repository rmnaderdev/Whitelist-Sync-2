package net.rmnad.models.api;

import net.rmnad.models.OppedPlayer;

import java.util.Objects;

public class OpEntry {

    private int id;
    private int accountId;
    private String uuid;
    private String name;
    private boolean isOpped;

    public int getId() {
        return id;
    }

    public void setId(int id) {
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

    public boolean getOpped() {
        return isOpped;
    }

    public void setOpped(boolean opped) {
        isOpped = opped;
    }

    @Override
    public String toString() {
        return "OpEntry{" +
                "id='" + id + '\'' +
                ", accountId='" + accountId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OpEntry opEntry = (OpEntry) o;
        return Objects.equals(id, opEntry.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public OppedPlayer toOppedPlayer() {
        return new OppedPlayer(uuid, name, isOpped);
    }
}
