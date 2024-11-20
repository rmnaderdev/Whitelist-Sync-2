package net.rmnad.core.services;

import com.google.gson.Gson;
import net.rmnad.core.Log;
import net.rmnad.core.WhitelistSyncCore;
import net.rmnad.core.logging.LogMessages;
import net.rmnad.core.models.api.ModVersionInfo;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.net.ConnectException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

public class VersionChecker {

    private final ApiClientHelper apiClientHelper;

    public VersionChecker() {
        this.apiClientHelper = new ApiClientHelper(WhitelistSyncCore.CONFIG.webApiHost, null);
    }

    public ModVersionInfo getLatestVersion() {
        try {
            OkHttpClient client = this.apiClientHelper.getClient();

            Request request = new Request.Builder()
                    .url(this.apiClientHelper.getApiHost() + "/api/ModUpdate")
                    .addHeader("content-type", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    Gson gson = new Gson();
                    return gson.fromJson(response.body().string(), ModVersionInfo.class);
                }
            }
        } catch (ConnectException e) {
            Log.warning(LogMessages.WARN_WhitelistSyncWebVersionCheckConnectException);
        }catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            Log.error("Error checking latest mod version.");
        }

        return null;
    }

    public void checkVersion(ArtifactVersion currentModVersion, ArtifactVersion currentMinecraftVersion) {
        ModVersionInfo versionInfo = this.getLatestVersion();
        if (versionInfo != null) {
            DefaultArtifactVersion latestVersion = new DefaultArtifactVersion(versionInfo.getLatestModVersion());
            if (latestVersion.compareTo(currentModVersion) > 0) {
                Log.info(versionInfo.getOutOfDateMessage());
                Log.info("CurseForge: " + versionInfo.getCurseForgeLink() + "?version=" + currentMinecraftVersion);
                Log.info("Modrinth: " + versionInfo.getModrinthLink() + "?g=" + currentMinecraftVersion);
            } else {
                Log.info(versionInfo.getUpToDateMessage());
            }
        }
    }

    public void checkVersion(String currentModVersionStr, String currentMinecraftVersionStr) {
        ModVersionInfo versionInfo = this.getLatestVersion();

        // Parse the version strings
        ArtifactVersion currentModVersion = new DefaultArtifactVersion(currentModVersionStr);
        ArtifactVersion currentMinecraftVersion = new DefaultArtifactVersion(currentMinecraftVersionStr);

        checkVersion(currentModVersion, currentMinecraftVersion);
    }
}
