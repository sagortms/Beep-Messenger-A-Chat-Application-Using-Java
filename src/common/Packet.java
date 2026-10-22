package common;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

// every string is sent as length + bytes, so commas or newlines in a message cant break anything
public final class Packet {
    public static final int MAX_FILE_BYTES = 25 * 1024 * 1024;
    public static final int MAX_MESSAGE_CHARS = 4000;
    public static final int MAX_POST_CHARS = 3000;

    private static final int MAX_STRING_BYTES = 1024 * 1024;
    private static final int MAX_ARGS = 50000;

    public final String type;
    public final String[] args;
    public final byte[] data;

    public Packet(String type, String... args) {
        this(type, null, args);
    }

    private Packet(String type, byte[] data, String[] args) {
        this.type = type;
        this.args = args == null ? new String[0] : args;
        this.data = data;
    }

    public static Packet withData(String type, byte[] data, String... args) {
        return new Packet(type, data, args);
    }

    public String arg(int i) {
        return i >= 0 && i < args.length && args[i] != null ? args[i] : "";
    }

    public void writeTo(DataOutputStream out) throws IOException {
        writeString(out, type);
        out.writeInt(args.length);
        for (String a : args) {
            writeString(out, a);
        }
        if (data == null) {
            out.writeInt(-1);
        } else {
            out.writeInt(data.length);
            out.write(data);
        }
        out.flush();
    }

    public static Packet readFrom(DataInputStream in) throws IOException {
        String type = readString(in);
        int count = in.readInt();
        if (count < 0 || count > MAX_ARGS) {
            throw new IOException("Bad packet: argument count " + count);
        }
        String[] args = new String[count];
        for (int i = 0; i < count; i++) {
            args[i] = readString(in);
        }
        int len = in.readInt();
        byte[] data = null;
        if (len >= 0) {
            if (len > MAX_FILE_BYTES + 1024) {
                throw new IOException("Bad packet: payload too large");
            }
            data = new byte[len];
            in.readFully(data);
        }
        return new Packet(type, data, args);
    }

    private static void writeString(DataOutputStream out, String s) throws IOException {
        byte[] b = (s == null ? "" : s).getBytes(StandardCharsets.UTF_8);
        out.writeInt(b.length);
        out.write(b);
    }

    private static String readString(DataInputStream in) throws IOException {
        int len = in.readInt();
        if (len < 0 || len > MAX_STRING_BYTES) {
            throw new IOException("Bad packet: string length " + len);
        }
        byte[] b = new byte[len];
        in.readFully(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
