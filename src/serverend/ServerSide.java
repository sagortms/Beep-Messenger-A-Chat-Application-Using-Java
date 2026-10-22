package serverend;

import common.Packet;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// usage: ServerSide [port] [dataFolder]
public class ServerSide {
    private final Database db;
    private final ServerSocket serverSocket;
    private final Map<String, Set<ServerClientHandler>> sessions = new HashMap<>();
    private volatile boolean running = true;

    public ServerSide(int port, File dataDir) throws IOException {
        this.db = new Database(dataDir);
        this.serverSocket = new ServerSocket(port);
        System.out.println("Beep server started on port " + port);
        System.out.println("Data folder: " + dataDir.getAbsolutePath());
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        File dir = new File(args.length > 1 ? args[1] : "data");
        new ServerSide(port, dir).run();
    }

    // each accepted socket is closed by its handler when the client leaves
    @SuppressWarnings("resource")
    public void run() {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                startHandler(socket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        }
    }

    private void startHandler(Socket socket) {
        try {
            new ServerClientHandler(this, socket).start();
        } catch (IOException e) {
            System.err.println("Could not start handler: " + e.getMessage());
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void close() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    Database db() {
        return db;
    }

    synchronized void addSession(String user, ServerClientHandler h) {
        Set<ServerClientHandler> set = sessions.get(user);
        if (set == null) {
            set = new HashSet<>();
            sessions.put(user, set);
        }
        set.add(h);
    }

    // true if that was the last session of the user
    synchronized boolean removeSession(String user, ServerClientHandler h) {
        Set<ServerClientHandler> set = sessions.get(user);
        if (set == null) {
            return false;
        }
        set.remove(h);
        if (set.isEmpty()) {
            sessions.remove(user);
            return true;
        }
        return false;
    }

    synchronized boolean sessionCountIsOne(String user) {
        Set<ServerClientHandler> set = sessions.get(user);
        return set != null && set.size() == 1;
    }

    synchronized boolean isOnline(String user) {
        return sessions.containsKey(user);
    }

    synchronized List<String> onlineUsers() {
        return new ArrayList<>(sessions.keySet());
    }

    void sendTo(String user, Packet p) {
        List<ServerClientHandler> targets;
        synchronized (this) {
            Set<ServerClientHandler> set = sessions.get(user);
            targets = set == null ? new ArrayList<ServerClientHandler>() : new ArrayList<>(set);
        }
        for (ServerClientHandler h : targets) {
            h.send(p);
        }
    }
}
