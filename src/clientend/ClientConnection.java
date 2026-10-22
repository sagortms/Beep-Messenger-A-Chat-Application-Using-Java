package clientend;

import common.Packet;

import javax.swing.SwingUtilities;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// one reader thread and one writer thread, so a big file upload doesnt freeze the window
final class ClientConnection {
    interface Handler {
        void onPacket(Packet p);

        void onClosed();
    }

    private final Handler handler;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "beep-writer");
        t.setDaemon(true);
        return t;
    });
    private Socket socket;
    private DataOutputStream out;
    private volatile boolean closing;

    ClientConnection(Handler handler) {
        this.handler = handler;
    }

    // this blocks, so call it from a background thread.
    // the socket stays open on purpose, close() shuts it down later
    @SuppressWarnings("resource")
    void connect(String host, int port) throws IOException {
        close();
        Socket s = new Socket();
        DataInputStream input;
        DataOutputStream output;
        try {
            s.connect(new InetSocketAddress(host, port), 4000);
            s.setTcpNoDelay(true);
            input = new DataInputStream(new BufferedInputStream(s.getInputStream()));
            output = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()));
        } catch (IOException e) {
            s.close();
            throw e;
        }
        out = output;
        socket = s;
        closing = false;
        Thread reader = new Thread(() -> readLoop(s, input), "beep-reader");
        reader.setDaemon(true);
        reader.start();
    }

    boolean isConnected() {
        return socket != null && !socket.isClosed();
    }

    private void readLoop(Socket mine, DataInputStream in) {
        try {
            while (true) {
                final Packet p = Packet.readFrom(in);
                SwingUtilities.invokeLater(() -> {
                    if (socket == mine) {
                        handler.onPacket(p);
                    }
                });
            }
        } catch (IOException e) {
            if (socket == mine && !closing) {
                SwingUtilities.invokeLater(() -> {
                    if (socket == mine) {
                        socket = null;
                        handler.onClosed();
                    }
                });
            }
        }
    }

    void send(final Packet p) {
        final Socket mine = socket;
        final DataOutputStream stream = out;
        if (mine == null || mine.isClosed()) {
            return;
        }
        writer.execute(() -> {
            try {
                synchronized (stream) {
                    p.writeTo(stream);
                }
            } catch (IOException e) {
                try {
                    mine.close();
                } catch (IOException ignored) {
                }
            }
        });
    }

    void close() {
        closing = true;
        Socket s = socket;
        socket = null;
        if (s != null) {
            try {
                s.close();
            } catch (IOException ignored) {
            }
        }
    }
}
