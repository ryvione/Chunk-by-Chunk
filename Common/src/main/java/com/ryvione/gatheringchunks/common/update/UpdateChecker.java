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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private static final String API_URL = "https://ryvux-api.nexusraven4545.workers.dev/mods/gathering-chunks";
    private static final String CURRENT_VERSION = "2.2.5-Beta.3.6-Hotfix.2";
    private static final Gson GSON = new Gson();
    private static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+\\.\\d+)(?:-Beta\\.(\\d+)(?:\\.(\\d+))?)?(?:-Hotfix\\.(\\d+))?");

    private static VersionInfo latestVersionInfo = null;
    private static boolean updateAvailable = false;

    public static CompletableFuture<Void> checkForUpdates() {
        return CompletableFuture.runAsync(() -> {
            GatheringChunksConstants.LOGGER.info("[UpdateChecker] Starting update check...");
            GatheringChunksConstants.LOGGER.info("[UpdateChecker] Current version: {}", formatVersionForDisplay(CURRENT_VERSION));
            GatheringChunksConstants.LOGGER.info("[UpdateChecker] API URL: {}", API_URL);

            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                GatheringChunksConstants.LOGGER.info("[UpdateChecker] Connecting to API...");
                int responseCode = connection.getResponseCode();
                GatheringChunksConstants.LOGGER.info("[UpdateChecker] Response code: {}", responseCode);

                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    String jsonResponse = response.toString();
                    GatheringChunksConstants.LOGGER.info("[UpdateChecker] API Response: {}", jsonResponse);

                    JsonObject json = GSON.fromJson(jsonResponse, JsonObject.class);

                    if (json.has("latest_release") && !json.get("latest_release").isJsonNull()) {
                        JsonObject release = json.getAsJsonObject("latest_release");
                        String version = release.get("version").getAsString();
                        String changelog = release.has("changelog") ? release.get("changelog").getAsString() : "";

                        GatheringChunksConstants.LOGGER.info("[UpdateChecker] Latest release version: {}", formatVersionForDisplay(version));

                        int comparison = compareFullVersions(version, CURRENT_VERSION);
                        if (comparison > 0) {
                            latestVersionInfo = new VersionInfo(version, changelog, false);
                            updateAvailable = true;
                            GatheringChunksConstants.LOGGER.warn("[UpdateChecker] Update Available: {}", formatVersionForDisplay(version));
                        } else {
                            GatheringChunksConstants.LOGGER.info("[UpdateChecker] Running latest release version");
                        }
                    } else {
                        GatheringChunksConstants.LOGGER.warn("[UpdateChecker] No latest_release found in API response");
                    }

                    if (json.has("latest_beta") && !json.get("latest_beta").isJsonNull()) {
                        JsonObject beta = json.getAsJsonObject("latest_beta");
                        String betaVersion = beta.get("version").getAsString();

                        GatheringChunksConstants.LOGGER.info("[UpdateChecker] Latest beta version: {}", formatVersionForDisplay(betaVersion));

                        int comparison = compareFullVersions(betaVersion, CURRENT_VERSION);

                        if (comparison > 0) {
                            String betaChangelog = beta.has("changelog") ? beta.get("changelog").getAsString() : "";
                            latestVersionInfo = new VersionInfo(betaVersion, betaChangelog, true);
                            updateAvailable = true;
                            GatheringChunksConstants.LOGGER.warn("[UpdateChecker] Update Available: {}", formatVersionForDisplay(betaVersion));
                        } else if (comparison == 0) {
                            GatheringChunksConstants.LOGGER.info("[UpdateChecker] Running latest beta version");
                        } else {
                            GatheringChunksConstants.LOGGER.info("[UpdateChecker] Running newer version than latest beta");
                        }
                    } else {
                        GatheringChunksConstants.LOGGER.warn("[UpdateChecker] No latest_beta found in API response");
                    }

                    GatheringChunksConstants.LOGGER.info("[UpdateChecker] Update check complete. Update available: {}", updateAvailable);
                } else {
                    GatheringChunksConstants.LOGGER.error("[UpdateChecker] Failed to fetch update info. HTTP Response Code: {}", responseCode);
                }
                connection.disconnect();
            } catch (Exception e) {
                GatheringChunksConstants.LOGGER.error("[UpdateChecker] Failed to check for updates", e);
            }
        });
    }

    private static String formatVersionForDisplay(String version) {
        Matcher m = VERSION_PATTERN.matcher(version);
        if (!m.matches()) {
            return "Gathering Chunks " + version;
        }

        StringBuilder formatted = new StringBuilder("Gathering Chunks ");
        formatted.append(m.group(1));

        if (m.group(2) != null) {
            formatted.append(" Beta ").append(m.group(2));
            if (m.group(3) != null) {
                formatted.append(".").append(m.group(3));
            }
        }

        if (m.group(4) != null) {
            formatted.append(" - Hotfix ").append(m.group(4));
        }

        return formatted.toString();
    }

    private static int compareFullVersions(String v1, String v2) {
        Matcher m1 = VERSION_PATTERN.matcher(v1);
        Matcher m2 = VERSION_PATTERN.matcher(v2);

        if (!m1.matches() || !m2.matches()) {
            GatheringChunksConstants.LOGGER.warn("[UpdateChecker] Invalid version format: {} or {}", v1, v2);
            return 0;
        }

        int major1 = Integer.parseInt(m1.group(1).split("\\.")[0]);
        int minor1 = Integer.parseInt(m1.group(1).split("\\.")[1]);
        int patch1 = Integer.parseInt(m1.group(1).split("\\.")[2]);
        int beta1 = m1.group(2) != null ? Integer.parseInt(m1.group(2)) : 9999;
        int betaSub1 = m1.group(3) != null ? Integer.parseInt(m1.group(3)) : 9999;
        int hotfix1 = m1.group(4) != null ? Integer.parseInt(m1.group(4)) : 9999;

        int major2 = Integer.parseInt(m2.group(1).split("\\.")[0]);
        int minor2 = Integer.parseInt(m2.group(1).split("\\.")[1]);
        int patch2 = Integer.parseInt(m2.group(1).split("\\.")[2]);
        int beta2 = m2.group(2) != null ? Integer.parseInt(m2.group(2)) : 9999;
        int betaSub2 = m2.group(3) != null ? Integer.parseInt(m2.group(3)) : 9999;
        int hotfix2 = m2.group(4) != null ? Integer.parseInt(m2.group(4)) : 9999;

        if (major1 != major2) return Integer.compare(major1, major2);
        if (minor1 != minor2) return Integer.compare(minor1, minor2);
        if (patch1 != patch2) return Integer.compare(patch1, patch2);
        if (beta1 != beta2) return Integer.compare(beta1, beta2);
        if (betaSub1 != betaSub2) return Integer.compare(betaSub1, betaSub2);
        return Integer.compare(hotfix1, hotfix2);
    }

    public static boolean isUpdateAvailable() {
        return updateAvailable;
    }

    public static VersionInfo getLatestVersionInfo() {
        return latestVersionInfo;
    }

    public static String getCurrentVersion() {
        return CURRENT_VERSION;
    }

    public static String getFormattedCurrentVersion() {
        return formatVersionForDisplay(CURRENT_VERSION);
    }

    public static String getFormattedLatestVersion() {
        return latestVersionInfo != null ? formatVersionForDisplay(latestVersionInfo.getVersion()) : null;
    }
}