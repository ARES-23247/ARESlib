package org.areslib.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Socket-based real-time telemetry streaming backend. Streams AdvantageScope compatible packets
 * over a TCP socket for remote viewing.
 *
 * <p><b>Protocol limitation:</b> Individual data record payloads are encoded as unsigned 16-bit
 * lengths (max 65,535 bytes). Fields exceeding this limit will be truncated and a warning logged.
 */
public class RlogServerBackend implements AresLoggerBackend {
  private final Map<String, Integer> keyToId = new HashMap<>();
  private final List<byte[]> startRecordsCache = new ArrayList<>();
  private final Map<Integer, byte[]> schemaDataCache = new HashMap<>();
  private final List<SocketClient> clients = new CopyOnWriteArrayList<>();
  private final BlockingQueue<LogFrame> sendQueue = new ArrayBlockingQueue<>(100);
  private final List<byte[]> byteArrayPool = new ArrayList<>();
  private final List<LogFrame> framePool = new ArrayList<>();

  private int nextEntryId = 1;
  private final long startTimeMicrosec;
  private ByteBuffer cycleBuffer;
  private final ByteBuffer stringEncodingBuffer =
      ByteBuffer.allocate(8192).order(ByteOrder.BIG_ENDIAN);
  private final CharsetEncoder utf8Encoder = StandardCharsets.UTF_8.newEncoder();

  /** Lock object for all cycleBuffer access — prevents physics thread data races. */
  private final Object cycleLock = new Object();

  /** Stored reference to close on OpMode restart, preventing port leaks. */
  private ServerSocket serverSocket;

  private Thread serverThread;

  private static class LogFrame {
    byte[] data;
    int length;
  }

  private static class SocketClient {
    Socket socket;
    OutputStream out;

    SocketClient(Socket socket) throws IOException {
      this.socket = socket;
      this.out = socket.getOutputStream();
    }

    void sendFramed(LogFrame frame) throws IOException {
      ByteBuffer buf = ByteBuffer.allocate(4 + frame.length).order(ByteOrder.BIG_ENDIAN);
      buf.putInt(frame.length);
      buf.put(frame.data, 0, frame.length);
      out.write(buf.array());
      out.flush();
    }
  }

  /**
   * Initializes an Rlog socket server.
   *
   * @param port The TCP port to listen on.
   */
  public RlogServerBackend(int port) {
    startTimeMicrosec = System.nanoTime() / 1000L;
    cycleBuffer = ByteBuffer.allocate(1024 * 512).order(ByteOrder.BIG_ENDIAN);
    cycleBuffer.put((byte) 0); // New Timestamp control byte
    cycleBuffer.putDouble(getTimestamp());

    Thread serverThread =
        new Thread(
            () -> {
              try {
                serverSocket = new ServerSocket(port);
                com.qualcomm.robotcore.util.RobotLog.i("Rlog Server listening on port " + port);
                while (!Thread.currentThread().isInterrupted()) {
                  Socket clientSocket = serverSocket.accept();
                  clientSocket.setTcpNoDelay(true);
                  SocketClient sc = new SocketClient(clientSocket);

                  // Replay all known Start records inside an initial frame
                  // Structure: [Version 2] [0 Control] [Timestamp] [Records]
                  ByteBuffer initBuf = ByteBuffer.allocate(1024 * 512).order(ByteOrder.BIG_ENDIAN);
                  initBuf.put((byte) 2); // Version 2
                  initBuf.put((byte) 0); // Timestamp control byte
                  initBuf.putDouble(getTimestamp());

                  synchronized (startRecordsCache) {
                    for (byte[] startRec : startRecordsCache) {
                      if (initBuf.remaining() < startRec.length) {
                        ByteBuffer newBuf =
                            ByteBuffer.allocate(initBuf.capacity() * 2).order(ByteOrder.BIG_ENDIAN);
                        initBuf.flip();
                        newBuf.put(initBuf);
                        initBuf = newBuf;
                      }
                      initBuf.put(startRec);
                    }
                  }

                  // Replay cached schema data so the AdvantageScope struct parser doesn't drop
                  // greyed out data
                  synchronized (schemaDataCache) {
                    for (Map.Entry<Integer, byte[]> schema : schemaDataCache.entrySet()) {
                      byte[] structBytes = schema.getValue();
                      int recordSize = 1 + 2 + 2 + structBytes.length;
                      if (initBuf.remaining() < recordSize) {
                        ByteBuffer newBuf =
                            ByteBuffer.allocate(initBuf.capacity() * 2).order(ByteOrder.BIG_ENDIAN);
                        initBuf.flip();
                        newBuf.put(initBuf);
                        initBuf = newBuf;
                      }
                      initBuf.put((byte) 2); // Data record
                      initBuf.putShort(schema.getKey().shortValue()); // ID
                      initBuf.putShort((short) structBytes.length);
                      initBuf.put(structBytes);
                    }
                  }

                  byte[] initPayload = new byte[initBuf.position()];
                  initBuf.flip();
                  initBuf.get(initPayload);

                  sc.sendFramed(
                      new LogFrame() {
                        {
                          data = initPayload;
                          length = initPayload.length;
                        }
                      });
                  clients.add(sc);
                  com.qualcomm.robotcore.util.RobotLog.i(
                      "AdvantageScope Connected to RLOG Stream!");

                  // Discard incoming heartbeat bytes
                  Thread readerThread =
                      new Thread(
                          () -> {
                            try {
                              InputStream in = clientSocket.getInputStream();
                              byte[] dummy = new byte[1024];
                              while (in.read(dummy) != -1) {
                                Thread.sleep(1);
                              }
                            } catch (Exception ignored) {
                              com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(ignored));
                            }
                            clients.remove(sc);
                            try {
                              clientSocket.close();
                            } catch (Exception ignored) {
                              com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(ignored));
                            }
                          });
                  readerThread.setDaemon(true);
                  readerThread.start();
                }
              } catch (IOException e) {
                com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
              }
            });
    serverThread.setDaemon(true);
    serverThread.start();

    Thread senderThread =
        new Thread(
            () -> {
              while (!Thread.currentThread().isInterrupted()) {
                try {
                  LogFrame frame = sendQueue.take();
                  List<SocketClient> failedClients = null;
                  for (SocketClient sc : clients) {
                    try {
                      sc.sendFramed(frame);
                    } catch (IOException e) {
                      try {
                        sc.socket.close();
                      } catch (Exception ignored) {
                        com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(ignored));
                      }
                      if (failedClients == null) failedClients = new ArrayList<>();
                      failedClients.add(sc);
                    }
                  }
                  if (failedClients != null) {
                    clients.removeAll(failedClients);
                  }

                  // Return to pool
                  synchronized (byteArrayPool) {
                    byteArrayPool.add(frame.data);
                  }
                  synchronized (framePool) {
                    framePool.add(frame);
                  }
                } catch (InterruptedException e) {
                  Thread.currentThread().interrupt();
                }
              }
            });
    senderThread.setDaemon(true);
    senderThread.start();
  }

  private void ensureCapacity(int neededBytes) {
    if (cycleBuffer.remaining() < neededBytes) {
      ByteBuffer newBuf =
          ByteBuffer.allocate(
                  Math.max(cycleBuffer.capacity() * 2, cycleBuffer.position() + neededBytes))
              .order(ByteOrder.BIG_ENDIAN);
      cycleBuffer.flip();
      newBuf.put(cycleBuffer);
      cycleBuffer = newBuf;
    }
  }

  /** Maximum payload size encodable in 2 bytes (unsigned short). */
  private static final int MAX_PAYLOAD_SIZE = 0xFFFF;

  /**
   * Clamps a payload size to the unsigned-short range and warns on overflow. This prevents silent
   * data corruption on the RLOG wire protocol.
   */
  private int safePayloadLength(String key, int payloadSize) {
    if (payloadSize > MAX_PAYLOAD_SIZE) {
      com.qualcomm.robotcore.util.RobotLog.e(
          "RLOG WARNING: Payload for '"
              + key
              + "' is "
              + payloadSize
              + " bytes, exceeding the 65535-byte protocol limit. Data will be truncated.");
      return MAX_PAYLOAD_SIZE;
    }
    return payloadSize;
  }

  private int getOrCreateEntry(String key, String type) {
    if (keyToId.containsKey(key)) {
      return keyToId.get(key);
    }
    int id = nextEntryId++;
    keyToId.put(key, id);

    byte[] nameBytes = key.getBytes(StandardCharsets.UTF_8);
    byte[] typeBytes = type.getBytes(StandardCharsets.UTF_8);

    int recordSize = 1 + 2 + 2 + nameBytes.length + 2 + typeBytes.length;
    ensureCapacity(recordSize);

    // Write start record to current cycle
    cycleBuffer.put((byte) 1);
    cycleBuffer.putShort((short) id);
    cycleBuffer.putShort((short) nameBytes.length);
    cycleBuffer.put(nameBytes);
    cycleBuffer.putShort((short) typeBytes.length);
    cycleBuffer.put(typeBytes);

    // Cache it for new clients
    ByteBuffer cacheBuf = ByteBuffer.allocate(recordSize).order(ByteOrder.BIG_ENDIAN);
    cacheBuf.put((byte) 1);
    cacheBuf.putShort((short) id);
    cacheBuf.putShort((short) nameBytes.length);
    cacheBuf.put(nameBytes);
    cacheBuf.putShort((short) typeBytes.length);
    cacheBuf.put(typeBytes);

    synchronized (startRecordsCache) {
      startRecordsCache.add(cacheBuf.array());
    }

    return id;
  }

  private double getTimestamp() {
    return ((System.nanoTime() / 1000L) - startTimeMicrosec) / 1000000.0;
  }

  @Override
  public void putNumber(String key, double value) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "double");
      ensureCapacity(1 + 2 + 2 + 8);
      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) 8); // value length
      cycleBuffer.putDouble(value);
    }
  }

  @Override
  public void putNumberArray(String key, double[] values) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "double[]");
      int payloadSize = safePayloadLength(key, values.length * 8);
      ensureCapacity(1 + 2 + 2 + payloadSize);

      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) payloadSize);
      int maxDoubles = payloadSize / 8;
      for (int i = 0; i < maxDoubles; i++) {
        cycleBuffer.putDouble(values[i]);
      }
    }
  }

  @Override
  public void putString(String key, String value) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "string");
      stringEncodingBuffer.clear();
      byte[] rawBytes =
          value.getBytes(
              StandardCharsets.UTF_8); // String.getBytes still allocates, but we'll minimize it
      int safeLen = safePayloadLength(key, rawBytes.length);
      ensureCapacity(1 + 2 + 2 + safeLen);

      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) safeLen);
      cycleBuffer.put(rawBytes, 0, safeLen);
    }
  }

  @Override
  public void putBoolean(String key, boolean value) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "boolean");
      ensureCapacity(1 + 2 + 2 + 1);
      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) 1);
      cycleBuffer.put(value ? (byte) 1 : (byte) 0);
    }
  }

  @Override
  public void putBooleanArray(String key, boolean[] values) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "boolean[]");
      int payloadSize = safePayloadLength(key, values.length);
      ensureCapacity(1 + 2 + 2 + payloadSize);

      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) payloadSize);
      for (int i = 0; i < payloadSize; i++) {
        cycleBuffer.put(values[i] ? (byte) 1 : (byte) 0);
      }
    }
  }

  @Override
  public void putStringArray(String key, String[] values) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, "string[]");

      // We'll calculate the total payload size first.
      // This is slightly inefficient (two passes) but avoids ANY allocations.
      int totalPayloadSize = 4; // Array length header
      for (String s : values) {
        totalPayloadSize += 4 + s.length() * 3; // Worst case UTF-8 overhead
      }

      ensureCapacity(1 + 2 + 2 + totalPayloadSize);

      cycleBuffer.put((byte) 2); // Record type
      cycleBuffer.putShort((short) id);
      int lengthPlaceholderPos = cycleBuffer.position();
      cycleBuffer.putShort((short) 0); // Placeholder for payload length

      int payloadStart = cycleBuffer.position();
      cycleBuffer.putInt(values.length);

      for (String s : values) {
        stringEncodingBuffer.clear();
        utf8Encoder.reset();
        utf8Encoder.encode(java.nio.CharBuffer.wrap(s), stringEncodingBuffer, true);
        utf8Encoder.flush(stringEncodingBuffer);
        stringEncodingBuffer.flip();

        int strLen = stringEncodingBuffer.remaining();
        cycleBuffer.putInt(strLen);
        cycleBuffer.put(stringEncodingBuffer);
      }

      int payloadEnd = cycleBuffer.position();
      int actualPayloadSize = payloadEnd - payloadStart;
      int safePayloadSize = safePayloadLength(key, actualPayloadSize);

      // Backfill the length
      cycleBuffer.putShort(lengthPlaceholderPos, (short) safePayloadSize);

      // If we truncated, we need to reset the position
      if (actualPayloadSize > safePayloadSize) {
        cycleBuffer.position(payloadStart + safePayloadSize);
      }
    }
  }

  @Override
  public void putStruct(String key, String typeString, byte[] structBytes) {
    synchronized (cycleLock) {
      int id = getOrCreateEntry(key, typeString);

      if (key.startsWith(".schema/")) {
        synchronized (schemaDataCache) {
          schemaDataCache.put(id, structBytes.clone());
        }
      }

      int safeLen = safePayloadLength(key, structBytes.length);
      ensureCapacity(1 + 2 + 2 + safeLen);
      cycleBuffer.put((byte) 2);
      cycleBuffer.putShort((short) id);
      cycleBuffer.putShort((short) safeLen);
      cycleBuffer.put(structBytes, 0, safeLen);
    }
  }

  @Override
  public void update() {
    synchronized (cycleLock) {
      int pos = cycleBuffer.position();
      if (pos <= 9) {
        // Empty cycle (1 byte prefix + 8 byte timestamp), just replace timestamp
        cycleBuffer.clear();
        cycleBuffer.put((byte) 0);
        cycleBuffer.putDouble(getTimestamp());
        return;
      }

      LogFrame frame;
      synchronized (framePool) {
        if (framePool.isEmpty()) {
          frame = new LogFrame();
        } else {
          frame = framePool.remove(framePool.size() - 1);
        }
      }

      synchronized (byteArrayPool) {
        if (byteArrayPool.isEmpty()) {
          frame.data = new byte[Math.max(pos, 8192)];
        } else {
          frame.data = byteArrayPool.remove(byteArrayPool.size() - 1);
          if (frame.data.length < pos) {
            frame.data = new byte[pos];
          }
        }
      }
      frame.length = pos;

      cycleBuffer.flip();
      cycleBuffer.get(frame.data, 0, pos);

      if (!sendQueue.offer(frame)) {
        synchronized (byteArrayPool) {
          byteArrayPool.add(frame.data);
        }
        synchronized (framePool) {
          framePool.add(frame);
        }
        sendQueue.poll(); // drop oldest
      }

      cycleBuffer.clear();
      cycleBuffer.put((byte) 0);
      cycleBuffer.putDouble(getTimestamp());
    }
  }

  /**
   * Closes the RLOG server, releasing the bound port and disconnecting all clients. Prevents {@link
   * java.net.BindException} on OpMode restart.
   */
  @Override
  public void close() {
    try {
      if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
      }
    } catch (IOException e) {
      com.qualcomm.robotcore.util.RobotLog.e(String.valueOf(e));
    }
    if (serverThread != null) {
      serverThread.interrupt();
    }
    for (SocketClient sc : clients) {
      try {
        sc.socket.close();
      } catch (IOException ignored) {
        // Ignored during shutdown
      }
    }
    clients.clear();
  }
}
