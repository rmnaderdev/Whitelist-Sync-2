package net.rmnad.core.models.api;

public class ModVersionInfo {

    private String latestModVersion;
    private String curseForgeLink;
    private String modrinthLink;
    private String minSupportedVersion;
    private String upToDateMessage;
    private String outOfDateMessage;

    public String getLatestModVersion() {
        return latestModVersion;
    }

    public void setLatestModVersion(String latestModVersion) {
        this.latestModVersion = latestModVersion;
    }

    public String getCurseForgeLink() {
        return curseForgeLink;
    }

    public void setCurseForgeLink(String curseForgeLink) {
        this.curseForgeLink = curseForgeLink;
    }

    public String getModrinthLink() {
        return modrinthLink;
    }

    public void setModrinthLink(String modrinthLink) {
        this.modrinthLink = modrinthLink;
    }

    public String getMinSupportedVersion() {
        return minSupportedVersion;
    }

    public void setMinSupportedVersion(String minSupportedVersion) {
        this.minSupportedVersion = minSupportedVersion;
    }

    public String getUpToDateMessage() {
        return upToDateMessage;
    }

    public void setUpToDateMessage(String upToDateMessage) {
        this.upToDateMessage = this.upToDateMessage;
    }

    public String getOutOfDateMessage() {
        return outOfDateMessage;
    }

    public void setOutOfDateMessage(String outOfDateMessage) {
        this.outOfDateMessage = outOfDateMessage;
    }
}
