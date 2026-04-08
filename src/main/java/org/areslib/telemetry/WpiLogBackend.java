package org.areslib.telemetry;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Backend for logging telemetry directly to a .wpilog file. Creates AdvantageScope compatible
 * on-disk logs.
 */
public class WpiLogBackend implements AresLoggerBackend {
  private final Map<String, Integer> keyToId = new HashMap<>();
  private int nextEntryId = 1; // ID 0 is reserved for controls
  private FileChannel channel;
  private final long startTimeMicrosec;
  private ByteBuffer encodeBuffer;
  private FileOutputStream fileOutputStream;

  /**
   * Initializes the WPILog backend and creates the initial log file.
   *
   * @param directoryPath The directory path to store the .wpilog file.
   */
  public WpiLogBackend(String directoryPath) {
    startTimeMicrosec = System.nanoTime() / 1000L;
    encodeBuffer = ByteBuffer.allocate(1024 * 64).order(ByteOrder.LITTLE_ENDIAN);

    File dir = new File(directoryPath);
    if (!dir.exists()) {
      dir.mkdirs();
    }

    File logFile = new File(dir, "log_" + System.currentTimeMillis() + ".wpilog");
    try {
      fileOutputStream = new FileOutputStream(logFile);
      channel = fileOutputStream.getChannel();

      // Header requires specific 12 bytes
      byte[] header =
          new byte[] {
            0x57,
            0x50,
            0x49,
            0x4c,
            0x4f,
            0x47, // WPILOG
            0x00,
            0x01, // v1.0
            0x00,
            0x00,
            0x00,
            0x00 // 0 extra bytes
          };
      ByteBuffer hBuf = ByteBuffer.wrap(header);
      channel.write(hBuf);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
      channel = null;
    }
  }

  private void writeRecordHeader(int entryId, int payloadSize, long timestamp) throws IOException {
    encodeBuffer.clear();
    encodeBuffer.put((byte) 0x7F); // 4-byte ID, 4-byte size, 8-byte timestamp
    encodeBuffer.putInt(entryId);
    encodeBuffer.putInt(payloadSize);
    encodeBuffer.putLong(timestamp);
  }

  private void writeStartRecord(String key, String type, int entryId, long timestamp)
      throws IOException {
    byte[] nameBytes = key.getBytes(StandardCharsets.UTF_8);
    byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);

    int payloadSize = 1 + 4 + 4 + nameBytes.length + 4 + typeBytes.length + 4;

    writeRecordHeader(0, payloadSize, timestamp);

    encodeBuffer.put((byte) 0); // Control type Start
    encodeBuffer.putInt(entryId);
    encodeBuffer.putInt(nameBytes.length);
    encodeBuffer.put(nameBytes);
    encodeBuffer.putInt(typeBytes.length);
    encodeBuffer.put(typeBytes);
    encodeBuffer.putInt(0); // Metadata size

    encodeBuffer.flip();
    channel.write(encodeBuffer);
  }

  private int getOrCreateEntry(String key, String type) {
    if (keyToId.containsKey(key)) {
      return keyToId.get(key);
    }
    int id = nextEntryId++;
    keyToId.put(key, id);
    try {
      writeStartRecord(key, type, id, getTimestamp());
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
    return id;
  }

  private long getTimestamp() {
    return (System.nanoTime() / 1000L) - startTimeMicrosec;
  }

  @Override
  public void putNumber(String key, double value) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "double");
    try {
      writeRecordHeader(id, 8, getTimestamp());
      encodeBuffer.putDouble(value);
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putNumberArray(String key, double[] values) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "double[]");
    int payloadSize = values.length * 8;
    try {
      if (encodeBuffer.capacity() < 17 + payloadSize) {
        encodeBuffer =
            ByteBuffer.allocate(Math.max(encodeBuffer.capacity() * 2, 17 + payloadSize))
                .order(ByteOrder.LITTLE_ENDIAN);
      }
      writeRecordHeader(id, payloadSize, getTimestamp());
      for (double val : values) {
        encodeBuffer.putDouble(val);
      }
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putString(String key, String value) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "string");
    byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
    try {
      if (encodeBuffer.capacity() < 17 + strBytes.length) {
        encodeBuffer =
            ByteBuffer.allocate(Math.max(encodeBuffer.capacity() * 2, 17 + strBytes.length))
                .order(ByteOrder.LITTLE_ENDIAN);
      }
      writeRecordHeader(id, strBytes.length, getTimestamp());
      encodeBuffer.put(strBytes);
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putBoolean(String key, boolean value) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "boolean");
    try {
      writeRecordHeader(id, 1, getTimestamp());
      encodeBuffer.put(value ? (byte) 1 : (byte) 0);
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putBooleanArray(String key, boolean[] values) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "boolean[]");
    int payloadSize = values.length;
    try {
      if (encodeBuffer.capacity() < 17 + payloadSize) {
        encodeBuffer =
            ByteBuffer.allocate(Math.max(encodeBuffer.capacity() * 2, 17 + payloadSize))
                .order(ByteOrder.LITTLE_ENDIAN);
      }
      writeRecordHeader(id, payloadSize, getTimestamp());
      for (boolean val : values) {
        encodeBuffer.put(val ? (byte) 1 : (byte) 0);
      }
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putStringArray(String key, String[] values) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, "string[]");

    // Calculate payload size: 4 bytes for array length + (4 bytes length + utf8 length) per string
    int payloadSize = 4;
    byte[][] utf8Strings = new byte[values.length][];
    for (int i = 0; i < values.length; i++) {
      utf8Strings[i] = values[i].getBytes(StandardCharsets.UTF_8);
      payloadSize += 4 + utf8Strings[i].length;
    }

    try {
      if (encodeBuffer.capacity() < 17 + payloadSize) {
        encodeBuffer =
            ByteBuffer.allocate(Math.max(encodeBuffer.capacity() * 2, 17 + payloadSize))
                .order(ByteOrder.LITTLE_ENDIAN);
      }
      writeRecordHeader(id, payloadSize, getTimestamp());
      encodeBuffer.putInt(values.length);
      for (byte[] strBytes : utf8Strings) {
        encodeBuffer.putInt(strBytes.length);
        encodeBuffer.put(strBytes);
      }
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void putStruct(String key, String typeString, byte[] data) {
    if (channel == null) return;
    int id = getOrCreateEntry(key, typeString);
    try {
      if (encodeBuffer.capacity() < 17 + data.length) {
        encodeBuffer =
            ByteBuffer.allocate(Math.max(encodeBuffer.capacity() * 2, 17 + data.length))
                .order(ByteOrder.LITTLE_ENDIAN);
      }
      writeRecordHeader(id, data.length, getTimestamp());
      encodeBuffer.put(data);
      encodeBuffer.flip();
      channel.write(encodeBuffer);
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }

  @Override
  public void update() {
    // Written synchronously; no need to flush per cycle.
  }

  /**
   * Closes the underlying log file, flushing any buffered data. Should be called from {@code
   * AresCommandOpMode.stop()} to prevent data loss if the OpMode is force-stopped on the Control
   * Hub.
   */
  public void close() {
    try {
      if (channel != null && channel.isOpen()) {
        channel.force(true);
        channel.close();
      }
      if (fileOutputStream != null) {
        fileOutputStream.close();
      }
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
  }
}
