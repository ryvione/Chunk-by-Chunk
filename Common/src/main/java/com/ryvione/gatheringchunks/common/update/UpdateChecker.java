package com.ryvione.gatheringchunks.common.update;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.ryvione.gatheringchunks.common.GatheringChunksConstants;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    private static final String API_URL = "http://ryvux-api.nexusraven4545.workers.dev/mods/gatheringchunks";
    private static final String CURRENT_VERSION = "2.2.5-Beta.3.4-Hotfix.2";
    private static final Gson GSON = new Gson();

    private static VersionInfo latestVersionInfo = null;
    private static boolean updateAvailable = false;

    public static CompletableFuture<Void> checkForUpdates() {
        return CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JsonObject json = GSON.fromJson(response.toString(), JsonObject.class);

                    if (json.has("latest_release") && !json.get("latest_release").isJsonNull()) {
                        JsonObject release = json.getAsJsonObject("latest_release");
                        String version = release.get("version").getAsString();
                        String changelog = release.has("changelog") ? release.get("changelog").getAsString() : "";

                        latestVersionInfo = new VersionInfo(version, changelog, false);
                        updateAvailable = !version.equals(CURRENT_VERSION);

                        if (updateAvailable) {
                            GatheringChunksConstants.LOGGER.warn("Update available! Current: {} | Latest: {}", CURRENT_VERSION, version);
                        } else {
                            GatheringChunksConstants.LOGGER.info("Running latest version: {}", CURRENT_VERSION);
                        }
                    }

                    if (json.has("latest_beta") && !json.get("latest_beta").isJsonNull()) {
                        JsonObject beta = json.getAsJsonObject("latest_beta");
                        String betaVersion = beta.get("version").getAsString();

                        if (!betaVersion.equals(CURRENT_VERSION) && compareVersions(betaVersion, CURRENT_VERSION) > 0) {
                            String betaChangelog = beta.has("changelog") ? beta.get("changelog").getAsString() : "";
                            latestVersionInfo = new VersionInfo(betaVersion, betaChangelog, true);
                            updateAvailable = true;
                            GatheringChunksConstants.LOGGER.warn("Beta update available! Current: {} | Latest Beta: {}", CURRENT_VERSION, betaVersion);
                        }
                    }
                }
                connection.disconnect();
            } catch (Exception e) {
                GatheringChunksConstants.LOGGER.error("Failed to check for updates", e);
            }
        });
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("-")[0].split("\\.");
        String[] parts2 = v2.split("-")[0].split("\\.");

        int length = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < length; i++) {
            int num1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int num2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

            if (num1 != num2) {
                return Integer.compare(num1, num2);
            }
        }
        return 0;
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
}