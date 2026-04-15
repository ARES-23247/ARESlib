package org.areslib.telemetry;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Background Daemon that scans for completed .wpilog files on the local filesystem or USB drive,
 * checks for an active internet connection, and automatically uploads them to a GitHub Release
 * endpoint. Once successfully uploaded (HTTP 201), the local log is deleted to prevent the 8GB
 * Control Hub memory from bricking.
 */
public class AresLogUploader {

  private static final ScheduledExecutorService EXECUTOR =
      Executors.newSingleThreadScheduledExecutor();
  private static final AtomicBoolean IS_UPLOADING = new AtomicBoolean(false);
  private static final String TARGET_REPO = "ARES-23247/ARESLib-Logs";
  private static final String DEFAULT_LOG_DIR = "/sdcard/FIRST/logs";
  private static final String PAT_FILE_PATH = "/sdcard/FIRST/deploy/github_pat.txt";

  private static final long UPLOAD_INITIAL_DELAY_SEC = 5;
  private static final long UPLOAD_PERIOD_SEC = 30;
  private static final long FILE_MODIFIED_DEBOUNCE_MS = 10000;
  private static final int UPLOAD_BUFFER_SIZE = 8192;

  @SuppressWarnings("FutureReturnValueIgnored")
  public static void startDaemon() {
    if (IS_UPLOADING.compareAndSet(false, true)) {
      var unused =
          EXECUTOR.scheduleWithFixedDelay(
              AresLogUploader::scanAndUpload,
              UPLOAD_INITIAL_DELAY_SEC,
              UPLOAD_PERIOD_SEC,
              TimeUnit.SECONDS);
    }
  }

  private static void scanAndUpload() {
    File patFile = new File(PAT_FILE_PATH);
    if (!patFile.exists()) return;

    File logDir = new File(resolveOptimalLogDirectory());
    if (!logDir.exists() || logDir.listFiles() == null) return;

    String patToken;
    try {
      patToken =
          new String(
                  Files.readAllBytes(Paths.get(PAT_FILE_PATH)),
                  java.nio.charset.StandardCharsets.UTF_8)
              .trim();
    } catch (IOException e) {
      return;
    }

    File[] files = logDir.listFiles((dir, name) -> name.endsWith(".wpilog"));
    if (files == null || files.length == 0) return;

    for (File f : files) {
      // Don't upload files modified in the last `FILE_MODIFIED_DEBOUNCE_MS` to avoid uploading live
      // active logs.
      if (System.currentTimeMillis() - f.lastModified() < FILE_MODIFIED_DEBOUNCE_MS) {
        continue;
      }

      boolean success = uploadAssetToRelease(f, patToken);
      if (success) {
        com.qualcomm.robotcore.util.RobotLog.i(
            "AresLogUploader: Successfully uploaded " + f.getName());
        try {
          Files.delete(f.toPath());
        } catch (IOException e) {
          com.qualcomm.robotcore.util.RobotLog.e(
              "AresLogUploader: Failed to delete uploaded log " + f.getName());
        }
      }
    }
  }

  private static boolean uploadAssetToRelease(File file, String token) {
    try {
      // Step 1: Create a Release
      URL createReleaseUrl = new URL("https://api.github.com/repos/" + TARGET_REPO + "/releases");
      HttpURLConnection releaseConn = (HttpURLConnection) createReleaseUrl.openConnection();
      releaseConn.setRequestMethod("POST");
      releaseConn.setRequestProperty("Authorization", "token " + token);
      releaseConn.setRequestProperty("Accept", "application/vnd.github.v3+json");
      String releaseBody =
          "{\"tag_name\":\"log-"
              + System.currentTimeMillis()
              + "\",\"name\":\"AutoUpload-"
              + file.getName()
              + "\"}";
      try (OutputStream os = releaseConn.getOutputStream()) {
        os.write(releaseBody.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      }
      int releaseCode = releaseConn.getResponseCode();
      if (releaseCode != HttpURLConnection.HTTP_CREATED) {
        return false;
      }

      // Hack: For pure simplicity in Java 8 / HttpURLConnection without pulling heavy JSON parsers,
      // We will actually just extract the ID using basic string slicing on the response body for
      // `upload_url`.
      // Actually, since we know the tag name immediately, we can use the upload endpoint directly:
      // The release upload URL is
      // https://uploads.github.com/repos/:owner/:repo/releases/:release_id/assets?name=xyz
      // But getting the release_id is tricky without JSON parser.
      // Wait, we have json-simple in ARESLib dependencies!
      // (com.googlecode.json-simple:json-simple:1.1.1)
      // Let's use Gson which is also in dependencies: com.google.code.gson:gson:2.10.1
      java.io.InputStreamReader reader =
          new java.io.InputStreamReader(
              releaseConn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8);
      com.google.gson.JsonObject responseObj =
          com.google.gson.JsonParser.parseReader(reader).getAsJsonObject();
      long releaseId = responseObj.get("id").getAsLong();

      URL uploadUrl =
          new URL(
              "https://uploads.github.com/repos/"
                  + TARGET_REPO
                  + "/releases/"
                  + releaseId
                  + "/assets?name="
                  + file.getName());
      HttpURLConnection uploadConn = (HttpURLConnection) uploadUrl.openConnection();
      uploadConn.setRequestMethod("POST");
      uploadConn.setRequestProperty("Authorization", "token " + token);
      uploadConn.setRequestProperty("Content-Type", "application/octet-stream");
      uploadConn.setRequestProperty("Accept", "application/vnd.github.v3+json");
      uploadConn.setDoOutput(true);

      try (OutputStream os = uploadConn.getOutputStream();
          FileInputStream fis = new FileInputStream(file)) {
        byte[] buffer = new byte[UPLOAD_BUFFER_SIZE];
        int length;
        while ((length = fis.read(buffer)) > 0) {
          os.write(buffer, 0, length);
        }
      }

      return uploadConn.getResponseCode() == HttpURLConnection.HTTP_CREATED;
    } catch (Exception e) {
      return false;
    }
  }

  private static String resolveOptimalLogDirectory() {
    String[] usbPaths = {
      "/storage/usbotg/logs", "/usb_storage/logs", "/storage/usb0/logs", "/mnt/media_rw/logs"
    };
    for (String path : usbPaths) {
      File f = new File(path);
      if (f.getParentFile() != null && f.getParentFile().exists()) return path;
    }
    return DEFAULT_LOG_DIR;
  }
}
