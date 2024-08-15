package net.rmnad.models.api;

//export interface IWhitelistEntry {
//    _id: ObjectId;
//    accountId: ObjectId;
//    uuid: string;
//    name: string;
//}

import java.util.Objects;

public class WhitelistEntry {

    private String _id;
    private String accountId;
    private String uuid;
    private String name;
    private Boolean isWhitelisted;

    public WhitelistEntry() {
    }

    public WhitelistEntry(String _id, String accountId, String uuid, String name) {
        this._id = _id;
        this.accountId = accountId;
        this.uuid = uuid;
        this.name = name;
    }

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

    public Boolean getWhitelisted() {
        return isWhitelisted;
    }

    public void setWhitelisted(Boolean whitelisted) {
        isWhitelisted = whitelisted;
    }

    @Override
    public String toString() {
        return "WhitelistEntry{" +
                "_id='" + _id + '\'' +
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
        return Objects.equals(_id, that._id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(_id);
    }
}
