package org.areslib.telemetry;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Socket-based real-time telemetry streaming backend.
 * Streams AdvantageScope compatible packets over a TCP socket for remote viewing.
 */
public class RlogServerBackend implements AresLoggerBackend {
    private final Map<String, Integer> keyToId = new HashMap<>();
    private final List<byte[]> startRecordsCache = new ArrayList<>();
    private final Map<Integer, byte[]> schemaDataCache = new HashMap<>();
    private final List<SocketClient> clients = new CopyOnWriteArrayList<>();
    
    private int nextEntryId = 1; 
    private final long startTimeMicrosec;
    private ByteBuffer cycleBuffer;

    private class SocketClient {
        Socket socket;
        OutputStream out;

        SocketClient(Socket socket) throws IOException {
            this.socket = socket;
            this.out = socket.getOutputStream();
        }
        
        void sendFramed(byte[] payload) throws IOException {
            ByteBuffer frame = ByteBuffer.allocate(4 + payload.length).order(ByteOrder.BIG_ENDIAN);
            frame.putInt(payload.length);
            frame.put(payload);
            out.write(frame.array());
            out.flush();
        }
    }

    /**
     * Initializes an Rlog socket server.
     * @param port The TCP port to listen on.
     */
    public RlogServerBackend(int port) {
        startTimeMicrosec = System.nanoTime() / 1000L;
        cycleBuffer = ByteBuffer.allocate(1024 * 512).order(ByteOrder.BIG_ENDIAN);
        cycleBuffer.put((byte) 0); // New Timestamp control byte
        cycleBuffer.putDouble(getTimestamp());
        
        Thread serverThread = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("Rlog Server listening on port " + port);
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
                    
                    synchronized(startRecordsCache) {
                        for (byte[] startRec : startRecordsCache) {
                            if (initBuf.remaining() < startRec.length) {
                                ByteBuffer newBuf = ByteBuffer.allocate(initBuf.capacity() * 2).order(ByteOrder.BIG_ENDIAN);
                                initBuf.flip();
                                newBuf.put(initBuf);
                                initBuf = newBuf;
                            }
                            initBuf.put(startRec);
                        }
                    }

                    // Replay cached schema data so the AdvantageScope struct parser doesn't drop greyed out data
                    synchronized(schemaDataCache) {
                        for (Map.Entry<Integer, byte[]> schema : schemaDataCache.entrySet()) {
                            byte[] structBytes = schema.getValue();
                            int recordSize = 1 + 2 + 2 + structBytes.length;
                            if (initBuf.remaining() < recordSize) {
                                ByteBuffer newBuf = ByteBuffer.allocate(initBuf.capacity() * 2).order(ByteOrder.BIG_ENDIAN);
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
                    
                    sc.sendFramed(initPayload);
                    clients.add(sc);
                    System.out.println("AdvantageScope Connected to RLOG Stream!");
                    
                    // Discard incoming heartbeat bytes
                    Thread readerThread = new Thread(() -> {
                        try {
                            InputStream in = clientSocket.getInputStream();
                            byte[] dummy = new byte[1024];
                            while (in.read(dummy) != -1) {}
                        } catch (Exception ignored) {}
                        clients.remove(sc);
                        try { clientSocket.close(); } catch (Exception ignored) {}
                    });
                    readerThread.setDaemon(true);
                    readerThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    private void ensureCapacity(int neededBytes) {
        if (cycleBuffer.remaining() < neededBytes) {
            ByteBuffer newBuf = ByteBuffer.allocate(Math.max(cycleBuffer.capacity() * 2, cycleBuffer.position() + neededBytes)).order(ByteOrder.BIG_ENDIAN);
            cycleBuffer.flip();
            newBuf.put(cycleBuffer);
            cycleBuffer = newBuf;
        }
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
        
        synchronized(startRecordsCache) {
            startRecordsCache.add(cacheBuf.array());
        }
        
        return id;
    }

    private double getTimestamp() {
        return ((System.nanoTime() / 1000L) - startTimeMicrosec) / 1000000.0;
    }

    @Override
    public void putNumber(String key, double value) {
        int id = getOrCreateEntry(key, "double");
        ensureCapacity(1 + 2 + 2 + 8);
        cycleBuffer.put((byte) 2);
        cycleBuffer.putShort((short) id);
        cycleBuffer.putShort((short) 8); // value length
        cycleBuffer.putDouble(value);
    }

    @Override
    public void putNumberArray(String key, double[] values) {
        int id = getOrCreateEntry(key, "double[]");
        int payloadSize = values.length * 8;
        ensureCapacity(1 + 2 + 2 + payloadSize);
        
        cycleBuffer.put((byte) 2);
        cycleBuffer.putShort((short) id);
        cycleBuffer.putShort((short) payloadSize);
        for (double val : values) {
            cycleBuffer.putDouble(val);
        }
    }

    @Override
    public void putString(String key, String value) {
        int id = getOrCreateEntry(key, "string");
        byte[] strBytes = value.getBytes(StandardCharsets.UTF_8);
        ensureCapacity(1 + 2 + 2 + strBytes.length);
        
        cycleBuffer.put((byte) 2);
        cycleBuffer.putShort((short) id);
        cycleBuffer.putShort((short) strBytes.length);
        cycleBuffer.put(strBytes);
    }

    @Override
    public void putStruct(String key, String typeString, byte[] structBytes) {
        int id = getOrCreateEntry(key, typeString);
        
        if (key.startsWith(".schema/")) {
            synchronized(schemaDataCache) {
                schemaDataCache.put(id, structBytes.clone());
            }
        }
        
        ensureCapacity(1 + 2 + 2 + structBytes.length);
        cycleBuffer.put((byte) 2);
        cycleBuffer.putShort((short) id);
        cycleBuffer.putShort((short) structBytes.length);
        cycleBuffer.put(structBytes);
    }

    @Override
    public void update() {
        if (cycleBuffer.position() == 9) {
            // Empty cycle (1 byte prefix + 8 byte timestamp), just replace timestamp
            cycleBuffer.clear();
            cycleBuffer.put((byte) 0);
            cycleBuffer.putDouble(getTimestamp());
            return;
        }
        
        byte[] payload = new byte[cycleBuffer.position()];
        cycleBuffer.flip();
        cycleBuffer.get(payload);
        
        for (SocketClient sc : clients) {
            try {
                sc.sendFramed(payload);
            } catch (IOException e) {
                try { sc.socket.close(); } catch (Exception ignored) {}
                clients.remove(sc);
            }
        }
        
        cycleBuffer.clear();
        cycleBuffer.put((byte) 0);
        cycleBuffer.putDouble(getTimestamp());
    }
}
