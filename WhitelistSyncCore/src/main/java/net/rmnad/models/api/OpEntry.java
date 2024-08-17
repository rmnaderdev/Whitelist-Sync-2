package net.rmnad.models.api;

import net.rmnad.models.OppedPlayer;

import java.util.Objects;

public class OpEntry {

    private String _id;
    private String accountId;
    private String uuid;
    private String name;
    private boolean isOpped;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
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
                "_id='" + _id + '\'' +
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
        return Objects.equals(_id, opEntry._id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_id);
    }

    public OppedPlayer toOppedPlayer() {
        return new OppedPlayer(uuid, name, isOpped);
    }
}
