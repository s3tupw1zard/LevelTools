package me.byteful.plugin.leveltools.util;

import me.byteful.plugin.leveltools.LevelToolsPlugin;
import me.byteful.plugin.leveltools.api.scheduler.Scheduler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdateChecker {
    private static final URI LATEST_RELEASE_API =
            URI.create("https://api.github.com/repos/s3tupw1zard/LevelTools/releases/latest");
    private static final String RELEASES_URL =
            "https://github.com/s3tupw1zard/LevelTools/releases";
    private static final Pattern TAG_NAME_PATTERN =
            Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"v?([^\\\"]+)\\\"");
    private static final Pattern STABLE_VERSION_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");

    @NotNull
    private final LevelToolsPlugin plugin;
    private final Scheduler scheduler;
    private final HttpClient httpClient;
    private String lastCheckedVersion;

    public UpdateChecker(@NotNull LevelToolsPlugin plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public void check() {
        plugin.getLogger().info("Checking for updates...");
        final String currentVersion = plugin.getDescription().getVersion();

        if (currentVersion.contains("-")) {
            plugin.getLogger().info(
                    "Update check skipped for development build "
                            + currentVersion
                            + ". Releases: "
                            + RELEASES_URL);
            return;
        }

        scheduler.asyncDelayed(() -> checkLatestRelease(currentVersion), 1L);
    }

    private void checkLatestRelease(@NotNull String currentVersion) {
        final HttpRequest request = HttpRequest.newBuilder(LATEST_RELEASE_API)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "LevelTools/" + currentVersion)
                .GET()
                .build();

        try {
            final HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                plugin.getLogger().info("No releases have been published for this fork yet.");
                return;
            }

            if (response.statusCode() != 200) {
                plugin.getLogger().info(
                        "Unable to check for updates: GitHub returned HTTP "
                                + response.statusCode()
                                + ".");
                return;
            }

            final Matcher matcher = TAG_NAME_PATTERN.matcher(response.body());
            if (!matcher.find()) {
                plugin.getLogger().info("Unable to check for updates: release tag was missing.");
                return;
            }

            final String latestVersion = matcher.group(1);
            lastCheckedVersion = latestVersion;

            final Optional<StableVersion> current = StableVersion.parse(currentVersion);
            final Optional<StableVersion> latest = StableVersion.parse(latestVersion);
            if (current.isEmpty() || latest.isEmpty()) {
                plugin.getLogger().info(
                        "Unable to compare versions "
                                + currentVersion
                                + " and "
                                + latestVersion
                                + ". Releases: "
                                + RELEASES_URL);
                return;
            }

            if (latest.get().compareTo(current.get()) > 0) {
                plugin.getLogger().info(
                        "A new LevelTools release is available: "
                                + latestVersion
                                + " (running "
                                + currentVersion
                                + ").");
                plugin.getLogger().info("Download it from: " + RELEASES_URL);
            } else {
                plugin.getLogger().info("No new updates found.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            plugin.getLogger().info("Update check interrupted.");
        } catch (IOException e) {
            plugin.getLogger().info("Unable to check for updates: " + e.getMessage());
        }
    }

    public String getLastCheckedVersion() {
        return lastCheckedVersion;
    }

    private record StableVersion(int major, int minor, int patch)
            implements Comparable<StableVersion> {
        private static Optional<StableVersion> parse(@NotNull String value) {
            final Matcher matcher = STABLE_VERSION_PATTERN.matcher(value);
            if (!matcher.matches()) {
                return Optional.empty();
            }

            return Optional.of(new StableVersion(
                    Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))));
        }

        @Override
        public int compareTo(@NotNull StableVersion other) {
            int result = Integer.compare(major, other.major);
            if (result != 0) {
                return result;
            }

            result = Integer.compare(minor, other.minor);
            if (result != 0) {
                return result;
            }

            return Integer.compare(patch, other.patch);
        }
    }
}
