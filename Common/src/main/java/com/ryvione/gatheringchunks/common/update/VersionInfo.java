package com.ryvione.gatheringchunks.common.update;

public class VersionInfo {
    private final String version;
    private final String changelog;
    private final boolean isBeta;

    public VersionInfo(String version, String changelog, boolean isBeta) {
        this.version = version;
        this.changelog = changelog;
        this.isBeta = isBeta;
    }

    public String getVersion() {
        return version;
    }

    public String getChangelog() {
        return changelog;
    }

    public boolean isBeta() {
        return isBeta;
    }
}