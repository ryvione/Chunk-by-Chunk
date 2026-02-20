/*
 * Original work Copyright (c) immortius
 * Modified work Copyright (c) 2026 Ryvione
 *
 * This file is part of Gathering Chunks (Ryvione's Fork).
 * Original: https://github.com/immortius/chunkbychunk
 *
 * Licensed under the MIT License. See LICENSE file in the project root for details.
 */
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