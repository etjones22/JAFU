package dev.jafu.client.feature.general.updater;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.jafu.client.JafuClient;
import dev.jafu.client.module.JafuModules;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class AutoUpdater {
    private static final String REPOSITORY = "etjones22/JAFU";
    private static final String USER_AGENT = "JAFU-Updater";
    private static final String SNAPSHOT_TAG = "snapshot-latest";
    private static final String LEGACY_DEV_TAG = "dev-latest";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(20L);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(new UpdaterThreadFactory());
    private static final Queue<String> CHAT_MESSAGES = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean CHECK_RUNNING = new AtomicBoolean();
    private static final AtomicBoolean INSTALLER_REGISTERED = new AtomicBoolean();

    private static final Path UPDATE_DIR = FabricLoader.getInstance().getGameDir().resolve(".jafu-updates");
    private static final Path PENDING_PATH = UPDATE_DIR.resolve("pending.properties");

    private AutoUpdater() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(AutoUpdater::flushChatMessages);
        cleanupCompletedPendingUpdate();
        registerPendingInstallOnExit();

        CompletableFuture.runAsync(() -> {
            sleepQuietly(10_000L);
            checkInternal(false);
        }, EXECUTOR);
    }

    public static void checkNow(boolean notifyNoUpdate) {
        CompletableFuture.runAsync(() -> checkInternal(notifyNoUpdate), EXECUTOR);
    }

    public static void setChannel(UpdateChannel channel, boolean checkImmediately) {
        AutoUpdaterSettings.INSTANCE.setChannel(channel);
        notifyPlayer("JAFU updater channel set to " + channel.label() + ".");
        if (checkImmediately) {
            checkNow(true);
        }
    }

    public static void notifyPlayer(String message) {
        CHAT_MESSAGES.add(message);
    }

    private static void checkInternal(boolean notifyNoUpdate) {
        if (!JafuModules.isEnabled(JafuModules.AUTO_UPDATER)) {
            if (notifyNoUpdate) {
                notifyPlayer("JAFU auto-updater is disabled.");
            }
            return;
        }

        if (!CHECK_RUNNING.compareAndSet(false, true)) {
            if (notifyNoUpdate) {
                notifyPlayer("JAFU update check is already running.");
            }
            return;
        }

        try {
            Path activeJar = activeJarPath();
            if (!isJarFile(activeJar)) {
                if (notifyNoUpdate) {
                    notifyPlayer("JAFU updater is only available from an installed jar.");
                }
                return;
            }

            UpdateChannel channel = AutoUpdaterSettings.INSTANCE.channel();
            ReleaseInfo release = fetchRelease(channel);
            if (release == null) {
                if (notifyNoUpdate) {
                    notifyPlayer("No JAFU " + channel.label() + " release is available yet.");
                }
                return;
            }

            PendingUpdate pending = readPendingUpdate();
            if (pending != null && pending.releaseId() == release.releaseId()) {
                if (notifyNoUpdate) {
                    notifyPlayer("JAFU " + pending.version() + " is already downloaded and will install next launch.");
                }
                return;
            }

            Path stagedJar = downloadRelease(release);
            String candidateVersion = readJarVersion(stagedJar);
            String currentVersion = currentVersion();
            if (!shouldInstall(currentVersion, candidateVersion, channel)) {
                Files.deleteIfExists(stagedJar);
                if (notifyNoUpdate) {
                    notifyPlayer("JAFU is already on the latest " + channel.label() + " build.");
                }
                return;
            }

            writePendingUpdate(new PendingUpdate(release.releaseId(), candidateVersion, channel, stagedJar, activeJar));
            registerPendingInstallOnExit();
            notifyPlayer("JAFU update " + candidateVersion + " downloaded. It will be installed when you next launch Minecraft.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            if (notifyNoUpdate) {
                notifyPlayer("JAFU update check was interrupted.");
            }
        } catch (Exception exception) {
            if (notifyNoUpdate) {
                notifyPlayer("JAFU updater failed: " + readableError(exception));
            }
        } finally {
            CHECK_RUNNING.set(false);
        }
    }

    private static ReleaseInfo fetchRelease(UpdateChannel channel) throws IOException, InterruptedException {
        String endpoint = channel == UpdateChannel.SNAPSHOT
                ? "https://api.github.com/repos/" + REPOSITORY + "/releases/tags/" + SNAPSHOT_TAG
                : "https://api.github.com/repos/" + REPOSITORY + "/releases/latest";
        HttpResponse<String> response = HTTP_CLIENT.send(request(endpoint).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404 && channel == UpdateChannel.SNAPSHOT) {
            endpoint = "https://api.github.com/repos/" + REPOSITORY + "/releases/tags/" + LEGACY_DEV_TAG;
            response = HTTP_CLIENT.send(request(endpoint).build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        }
        if (response.statusCode() == 404) {
            return null;
        }
        requireSuccess(response.statusCode(), "release lookup");

        JsonObject release = JsonParser.parseString(response.body()).getAsJsonObject();
        long releaseId = release.get("id").getAsLong();
        String tag = textValue(release, "tag_name", channel.id());
        JsonObject asset = findJarAsset(release.getAsJsonArray("assets"), channel);
        if (asset == null) {
            return null;
        }

        String assetName = textValue(asset, "name", "jafu.jar");
        String downloadUrl = textValue(asset, "browser_download_url", "");
        if (downloadUrl.isBlank()) {
            return null;
        }
        return new ReleaseInfo(releaseId, tag, assetName, downloadUrl);
    }

    private static JsonObject findJarAsset(JsonArray assets, UpdateChannel channel) {
        if (assets == null) {
            return null;
        }

        JsonObject fallback = null;
        for (JsonElement element : assets) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject asset = element.getAsJsonObject();
            String name = textValue(asset, "name", "");
            String lowerName = name.toLowerCase();
            if (!lowerName.endsWith(".jar") || lowerName.contains("sources")) {
                continue;
            }

            if (channel == UpdateChannel.SNAPSHOT && ("jafu-snapshot.jar".equals(lowerName) || "jafu-dev.jar".equals(lowerName))) {
                return asset;
            }
            if (channel == UpdateChannel.STABLE && lowerName.startsWith("jafu-")) {
                return asset;
            }
            if (fallback == null) {
                fallback = asset;
            }
        }
        return fallback;
    }

    private static Path downloadRelease(ReleaseInfo release) throws IOException, InterruptedException {
        Files.createDirectories(UPDATE_DIR);
        Path target = UPDATE_DIR.resolve(safeFileName(release.tag() + "-" + release.releaseId() + ".jar"));
        Path temp = UPDATE_DIR.resolve(target.getFileName() + ".part");
        Files.deleteIfExists(temp);

        HttpResponse<InputStream> response = HTTP_CLIENT.send(request(release.downloadUrl()).build(), HttpResponse.BodyHandlers.ofInputStream());
        requireSuccess(response.statusCode(), "jar download");
        try (InputStream input = response.body()) {
            Files.copy(input, temp, StandardCopyOption.REPLACE_EXISTING);
        }

        String version = readJarVersion(temp);
        if (version.isBlank()) {
            throw new IOException("downloaded jar has no version");
        }

        try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .timeout(HTTP_TIMEOUT)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", USER_AGENT)
                .GET();
    }

    private static void requireSuccess(int statusCode, String operation) throws IOException {
        if (statusCode < 200 || statusCode >= 300) {
            throw new IOException(operation + " returned HTTP " + statusCode);
        }
    }

    private static String readJarVersion(Path jarPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            JarEntry metadataEntry = jar.getJarEntry("fabric.mod.json");
            if (metadataEntry == null) {
                throw new IOException("downloaded jar is missing fabric.mod.json");
            }

            try (Reader reader = new InputStreamReader(jar.getInputStream(metadataEntry), StandardCharsets.UTF_8)) {
                JsonObject metadata = JsonParser.parseReader(reader).getAsJsonObject();
                return textValue(metadata, "version", "");
            }
        }
    }

    private static String currentVersion() {
        return FabricLoader.getInstance()
                .getModContainer(JafuClient.MOD_ID)
                .map(ModContainer::getMetadata)
                .map(metadata -> metadata.getVersion().getFriendlyString())
                .orElse("0.0.0");
    }

    private static Path activeJarPath() throws IOException {
        return FabricLoader.getInstance()
                .getModContainer(JafuClient.MOD_ID)
                .flatMap(container -> container.getOrigin().getPaths().stream().findFirst())
                .orElseGet(() -> Path.of(JafuClient.class.getProtectionDomain().getCodeSource().getLocation().getPath()))
                .toAbsolutePath()
                .normalize();
    }

    private static boolean isJarFile(Path path) {
        return path != null && Files.isRegularFile(path) && path.getFileName().toString().toLowerCase().endsWith(".jar");
    }

    private static boolean shouldInstall(String currentVersion, String candidateVersion, UpdateChannel channel) {
        if (candidateVersion.equals(currentVersion)) {
            return false;
        }

        int coreComparison = compareVersionCore(candidateVersion, currentVersion);
        if (coreComparison > 0) {
            return true;
        }
        if (coreComparison == 0) {
            return channel == UpdateChannel.SNAPSHOT || !candidateVersion.equals(currentVersion);
        }
        return false;
    }

    private static int compareVersionCore(String left, String right) {
        String[] leftParts = versionCore(left).split("\\.");
        String[] rightParts = versionCore(right).split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            int leftPart = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightPart = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
            if (leftPart != rightPart) {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static String versionCore(String version) {
        int suffixStart = version.length();
        int plusIndex = version.indexOf('+');
        int dashIndex = version.indexOf('-');
        if (plusIndex >= 0) {
            suffixStart = Math.min(suffixStart, plusIndex);
        }
        if (dashIndex >= 0) {
            suffixStart = Math.min(suffixStart, dashIndex);
        }
        return version.substring(0, suffixStart);
    }

    private static int parseVersionPart(String part) {
        String digits = part.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void cleanupCompletedPendingUpdate() {
        PendingUpdate pending = readPendingUpdate();
        if (pending == null || !pending.version().equals(currentVersion())) {
            return;
        }

        try {
            Files.deleteIfExists(PENDING_PATH);
            Files.deleteIfExists(pending.stagedJar());
        } catch (IOException ignored) {
            // Cleanup is best-effort only.
        }
    }

    private static void registerPendingInstallOnExit() {
        PendingUpdate pending = readPendingUpdate();
        if (pending == null || INSTALLER_REGISTERED.getAndSet(true)) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            PendingUpdate latestPending = readPendingUpdate();
            if (latestPending != null) {
                launchInstaller(latestPending);
            }
        }, "JAFU Update Installer"));
    }

    private static void launchInstaller(PendingUpdate pending) {
        try {
            Files.createDirectories(UPDATE_DIR);
            if (isWindows()) {
                Path script = UPDATE_DIR.resolve("install-jafu-update.ps1");
                Files.writeString(script, windowsInstallerScript(), StandardCharsets.UTF_8);
                new ProcessBuilder(
                        "powershell.exe",
                        "-NoProfile",
                        "-ExecutionPolicy",
                        "Bypass",
                        "-WindowStyle",
                        "Hidden",
                        "-File",
                        script.toString(),
                        Long.toString(ProcessHandle.current().pid()),
                        pending.stagedJar().toString(),
                        pending.activeJar().toString(),
                        PENDING_PATH.toString()
                ).start();
            } else {
                Path script = UPDATE_DIR.resolve("install-jafu-update.sh");
                Files.writeString(script, unixInstallerScript(), StandardCharsets.UTF_8);
                new ProcessBuilder(
                        "/bin/sh",
                        script.toString(),
                        Long.toString(ProcessHandle.current().pid()),
                        pending.stagedJar().toString(),
                        pending.activeJar().toString(),
                        PENDING_PATH.toString()
                ).start();
            }
        } catch (IOException ignored) {
            // Leave the pending marker so a future launch can try again.
        }
    }

    private static String windowsInstallerScript() {
        return """
                param(
                    [Parameter(Mandatory=$true)][int]$MinecraftPid,
                    [Parameter(Mandatory=$true)][string]$Source,
                    [Parameter(Mandatory=$true)][string]$Target,
                    [Parameter(Mandatory=$true)][string]$Pending
                )
                try { Wait-Process -Id $MinecraftPid -ErrorAction SilentlyContinue } catch {}
                Start-Sleep -Milliseconds 750
                try {
                    Copy-Item -LiteralPath $Source -Destination $Target -Force
                    Remove-Item -LiteralPath $Pending -Force -ErrorAction SilentlyContinue
                } catch {}
                """;
    }

    private static String unixInstallerScript() {
        return """
                pid="$1"
                source="$2"
                target="$3"
                pending="$4"
                while kill -0 "$pid" 2>/dev/null; do
                    sleep 1
                done
                if cp "$source" "$target"; then
                    rm -f "$pending"
                fi
                """;
    }

    private static PendingUpdate readPendingUpdate() {
        if (!Files.isRegularFile(PENDING_PATH)) {
            return null;
        }

        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(PENDING_PATH)) {
            properties.load(reader);
            return new PendingUpdate(
                    Long.parseLong(properties.getProperty("releaseId", "0")),
                    properties.getProperty("version", ""),
                    UpdateChannel.fromId(properties.getProperty("channel", UpdateChannel.STABLE.id())),
                    Path.of(properties.getProperty("stagedJar", "")),
                    Path.of(properties.getProperty("activeJar", ""))
            );
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void writePendingUpdate(PendingUpdate pending) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("releaseId", Long.toString(pending.releaseId()));
        properties.setProperty("version", pending.version());
        properties.setProperty("channel", pending.channel().id());
        properties.setProperty("stagedJar", pending.stagedJar().toString());
        properties.setProperty("activeJar", pending.activeJar().toString());

        Files.createDirectories(PENDING_PATH.getParent());
        try (var writer = Files.newBufferedWriter(PENDING_PATH)) {
            properties.store(writer, "JAFU pending update");
        }
    }

    private static void flushChatMessages(MinecraftClient client) {
        if (client.player == null) {
            return;
        }

        String message;
        while ((message = CHAT_MESSAGES.poll()) != null) {
            client.player.sendMessage(Text.literal("[JAFU] " + message), false);
        }
    }

    private static String textValue(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static String safeFileName(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static String readableError(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private record ReleaseInfo(long releaseId, String tag, String assetName, String downloadUrl) {
    }

    private record PendingUpdate(long releaseId, String version, UpdateChannel channel, Path stagedJar, Path activeJar) {
    }

    private static final class UpdaterThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "JAFU Auto Updater");
            thread.setDaemon(true);
            return thread;
        }
    }
}
