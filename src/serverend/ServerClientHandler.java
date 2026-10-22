package serverend;

import common.Packet;
import serverend.Database.Msg;
import serverend.Database.Post;
import serverend.Database.User;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerClientHandler extends Thread {
    private final ServerSide server;
    private final Database db;
    private final Socket socket;
    private final DataInputStream in;
    private final DataOutputStream out;
    private String user;

    public ServerClientHandler(ServerSide server, Socket socket) throws IOException {
        super("client-" + socket.getRemoteSocketAddress());
        this.server = server;
        this.db = server.db();
        this.socket = socket;
        this.in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        this.out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    @Override
    public void run() {
        try {
            while (true) {
                Packet p = Packet.readFrom(in);
                try {
                    dispatch(p);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    send(new Packet("ERROR", "Something went wrong on the server."));
                }
            }
        } catch (EOFException e) {
            // normal disconnect
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.out.println("Connection problem (" + (user == null ? "not logged in" : user) + "): " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void disconnect() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
        if (user != null) {
            String name = user;
            user = null;
            System.out.println("Client disconnected: " + name);
            if (server.removeSession(name, this)) {
                broadcastPresence(name, false);
            }
        }
    }

    // other clients' threads push packets here, so it has to be synchronized
    void send(Packet p) {
        synchronized (out) {
            try {
                p.writeTo(out);
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void dispatch(Packet p) throws IOException {
        if (user == null) {
            if (p.type.equals("LOGIN")) {
                handleLogin(p);
            } else if (p.type.equals("SIGNUP")) {
                handleSignup(p);
            } else {
                send(new Packet("ERROR", "Please log in first."));
            }
            return;
        }
        switch (p.type) {
            case "LOGOUT":
                disconnect();
                break;
            case "SEARCH":
                handleSearch(p.arg(0));
                break;
            case "BEEP_REQUEST":
                handleBeepRequest(p.arg(0));
                break;
            case "BEEP_ACCEPT":
                handleBeepAccept(p.arg(0));
                break;
            case "BEEP_DECLINE":
                handleBeepDecline(p.arg(0));
                break;
            case "BEEP_CANCEL":
                handleBeepCancel(p.arg(0));
                break;
            case "BEEP_REMOVE":
                handleBeepRemove(p.arg(0));
                break;
            case "MSG_SEND":
                handleMessage(p.arg(0), p.arg(1));
                break;
            case "FILE_SEND":
                handleFileSend(p);
                break;
            case "FILE_GET":
                handleFileGet(p.arg(0));
                break;
            case "HISTORY":
                handleHistory(p.arg(0));
                break;
            case "MARK_READ":
                if (db.areFriends(user, p.arg(0))) {
                    db.markRead(user, p.arg(0));
                }
                break;
            case "POST_CREATE":
                handlePostCreate(p.arg(0), p.arg(1));
                break;
            case "POST_DELETE":
                handlePostDelete(p.arg(0));
                break;
            case "FEED_GET":
                sendFeed();
                break;
            case "PROFILE_UPDATE":
                handleProfileUpdate(p);
                break;
            case "PASSWORD_CHANGE":
                handlePasswordChange(p.arg(0), p.arg(1));
                break;
            default:
                send(new Packet("ERROR", "Unknown command: " + p.type));
        }
    }

    private void handleSignup(Packet p) {
        String problem = db.register(p.arg(0), p.arg(1), p.arg(2), p.arg(3), p.arg(4));
        if (problem == null) {
            System.out.println("New account: " + p.arg(1).trim().toLowerCase());
            send(new Packet("SIGNUP_OK"));
        } else {
            send(new Packet("SIGNUP_FAIL", problem));
        }
    }

    private void handleLogin(Packet p) {
        User u = db.authenticate(p.arg(0), p.arg(1));
        if (u == null) {
            send(new Packet("LOGIN_FAIL", "Wrong username or password."));
            return;
        }
        user = u.username;
        server.addSession(user, this);
        System.out.println("Logged in: " + user);
        send(new Packet("LOGIN_OK", u.username, u.fullName, u.phone, u.email, u.bio));
        sendFriends(user);
        sendRequests(user);
        sendChats(user);
        sendFeed();
        if (server.sessionCountIsOne(user)) {
            broadcastPresence(user, true);
        }
    }

    private void handleProfileUpdate(Packet p) {
        String problem = db.updateProfile(user, p.arg(0), p.arg(1), p.arg(2), p.arg(3));
        if (problem != null) {
            send(new Packet("PROFILE_FAIL", problem));
            return;
        }
        User u = db.getUser(user);
        send(new Packet("PROFILE_OK", u.fullName, u.phone, u.email, u.bio));
        for (User f : db.friendsOf(user)) {
            sendFriends(f.username);
        }
    }

    private void handlePasswordChange(String oldPassword, String newPassword) {
        String problem = db.changePassword(user, oldPassword, newPassword);
        send(problem == null ? new Packet("PASSWORD_OK") : new Packet("PASSWORD_FAIL", problem));
    }

    private void handleSearch(String query) {
        List<String> args = new ArrayList<>();
        args.add(query);
        for (User u : db.search(user, query)) {
            args.add(u.username);
            args.add(u.fullName);
            args.add(db.relation(user, u.username));
        }
        send(new Packet("SEARCH_RESULT", args.toArray(new String[0])));
    }

    private void handleBeepRequest(String to) {
        String result = db.sendRequest(user, to);
        if (result.startsWith("ERR:")) {
            send(new Packet("ERROR", result.substring(4)));
            return;
        }
        if (result.equals("CONNECTED")) {
            connectionChanged(user, to);
            return;
        }
        sendRequests(user);
        sendRequests(to);
        server.sendTo(to, new Packet("NOTICE", db.getUser(user).fullName + " sent you a Beep Request."));
    }

    private void handleBeepAccept(String from) {
        if (!db.acceptRequest(user, from)) {
            send(new Packet("ERROR", "That request is no longer available."));
            sendRequests(user);
            return;
        }
        connectionChanged(user, from);
    }

    private void handleBeepDecline(String from) {
        if (db.removeRequest(from, user)) {
            sendRequests(user);
            sendRequests(from);
        }
    }

    private void handleBeepCancel(String to) {
        if (db.removeRequest(user, to)) {
            sendRequests(user);
            sendRequests(to);
        }
    }

    private void handleBeepRemove(String other) {
        if (db.removeFriend(user, other)) {
            sendFriends(user);
            sendFriends(other);
            sendChats(user);
            sendChats(other);
            server.sendTo(other, new Packet("BEEP_REMOVED", user));
            send(new Packet("BEEP_REMOVED", other));
        }
    }

    private void connectionChanged(String a, String b) {
        for (String name : new String[]{a, b}) {
            sendFriends(name);
            sendRequests(name);
            sendChats(name);
        }
        server.sendTo(a, new Packet("NOTICE", "You and " + db.getUser(b).fullName + " are now connected."));
        server.sendTo(b, new Packet("NOTICE", "You and " + db.getUser(a).fullName + " are now connected."));
    }

    private void sendFriends(String name) {
        List<String> args = new ArrayList<>();
        for (User f : db.friendsOf(name)) {
            args.add(f.username);
            args.add(f.fullName);
            args.add(server.isOnline(f.username) ? "1" : "0");
            args.add(String.valueOf(db.friendSince(name, f.username)));
        }
        server.sendTo(name, new Packet("FRIENDS", args.toArray(new String[0])));
    }

    private void sendRequests(String name) {
        List<String> in = new ArrayList<>();
        for (Object[] r : db.incomingRequests(name)) {
            in.add((String) r[0]);
            in.add(db.getUser((String) r[0]).fullName);
            in.add(String.valueOf(r[1]));
        }
        List<String> outList = new ArrayList<>();
        for (Object[] r : db.outgoingRequests(name)) {
            outList.add((String) r[0]);
            outList.add(db.getUser((String) r[0]).fullName);
            outList.add(String.valueOf(r[1]));
        }
        server.sendTo(name, new Packet("REQ_IN", in.toArray(new String[0])));
        server.sendTo(name, new Packet("REQ_OUT", outList.toArray(new String[0])));
    }

    private void sendChats(String name) {
        List<String> args = new ArrayList<>();
        for (Object[] row : db.chatSummaries(name)) {
            Msg last = (Msg) row[1];
            args.add((String) row[0]);
            args.add(last.from);
            args.add(String.valueOf(last.time));
            args.add(last.kind);
            args.add(last.text);
            args.add(String.valueOf(row[2]));
        }
        server.sendTo(name, new Packet("CHATS", args.toArray(new String[0])));
    }

    private void broadcastPresence(String name, boolean online) {
        for (User f : db.friendsOf(name)) {
            server.sendTo(f.username, new Packet("PRESENCE", name, online ? "1" : "0"));
        }
    }

    private void handleMessage(String to, String text) {
        text = text == null ? "" : text.trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > Packet.MAX_MESSAGE_CHARS) {
            send(new Packet("ERROR", "Message is too long (max " + Packet.MAX_MESSAGE_CHARS + " characters)."));
            return;
        }
        if (!db.areFriends(user, to)) {
            send(new Packet("ERROR", "You can only message your beepers."));
            return;
        }
        try {
            deliver(db.addMessage(user, to, "T", text, 0, null));
        } catch (IOException e) {
            send(new Packet("ERROR", "Message could not be saved."));
        }
    }

    private void handleFileSend(Packet p) {
        String to = p.arg(0);
        String name = safeFileName(p.arg(1));
        if (p.data == null || p.data.length == 0) {
            send(new Packet("ERROR", "The file is empty."));
            return;
        }
        if (p.data.length > Packet.MAX_FILE_BYTES) {
            send(new Packet("ERROR", "File is too large (max 25 MB)."));
            return;
        }
        if (!db.areFriends(user, to)) {
            send(new Packet("ERROR", "You can only send files to your beepers."));
            return;
        }
        try {
            deliver(db.addMessage(user, to, "F", name, p.data.length, p.data));
        } catch (IOException e) {
            send(new Packet("ERROR", "File could not be saved."));
        }
    }

    private void handleFileGet(String idText) throws IOException {
        Msg m = null;
        try {
            m = db.getMessage(Long.parseLong(idText));
        } catch (NumberFormatException ignored) {
        }
        if (m == null || !"F".equals(m.kind) || !(m.from.equals(user) || m.to.equals(user))) {
            send(new Packet("ERROR", "File not found."));
            return;
        }
        send(Packet.withData("FILE_DATA", db.readFile(m.id), String.valueOf(m.id), m.text));
    }

    private void deliver(Msg m) {
        Packet packet = new Packet("MSG_NEW", String.valueOf(m.id), m.from, m.to, String.valueOf(m.time), m.kind, m.text, String.valueOf(m.size));
        server.sendTo(m.from, packet);
        server.sendTo(m.to, packet);
    }

    private void handleHistory(String peer) {
        if (!db.areFriends(user, peer)) {
            send(new Packet("ERROR", "You are not connected with that user."));
            return;
        }
        List<String> args = new ArrayList<>();
        args.add(peer);
        for (Msg m : db.history(user, peer, 500)) {
            args.add(String.valueOf(m.id));
            args.add(m.from);
            args.add(String.valueOf(m.time));
            args.add(m.kind);
            args.add(m.text);
            args.add(String.valueOf(m.size));
        }
        send(new Packet("HISTORY", args.toArray(new String[0])));
    }

    private static String safeFileName(String name) {
        String n = name == null ? "" : name.replace('\\', '/');
        n = n.substring(n.lastIndexOf('/') + 1).trim();
        if (n.isEmpty()) {
            n = "file";
        }
        return n.length() > 120 ? n.substring(n.length() - 120) : n;
    }

    private void handlePostCreate(String vis, String text) {
        text = text == null ? "" : text.trim();
        if (!(vis.equals("public") || vis.equals("beepers") || vis.equals("private"))) {
            vis = "public";
        }
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > Packet.MAX_POST_CHARS) {
            send(new Packet("ERROR", "Post is too long (max " + Packet.MAX_POST_CHARS + " characters)."));
            return;
        }
        Post post = db.addPost(user, vis, text);
        Packet packet = postPacket("POST_NEW", post);
        for (String viewer : server.onlineUsers()) {
            if (db.canSee(viewer, post)) {
                server.sendTo(viewer, packet);
            }
        }
    }

    private void handlePostDelete(String idText) {
        try {
            long id = Long.parseLong(idText);
            if (db.deletePost(id, user)) {
                for (String viewer : server.onlineUsers()) {
                    server.sendTo(viewer, new Packet("POST_DELETED", idText));
                }
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void sendFeed() {
        List<String> args = new ArrayList<>();
        for (Post p : db.feedFor(user, 200)) {
            args.add(String.valueOf(p.id));
            args.add(p.author);
            args.add(db.getUser(p.author).fullName);
            args.add(String.valueOf(p.time));
            args.add(p.vis);
            args.add(p.text);
        }
        send(new Packet("FEED", args.toArray(new String[0])));
    }

    private Packet postPacket(String type, Post p) {
        return new Packet(type, String.valueOf(p.id), p.author, db.getUser(p.author).fullName,
                String.valueOf(p.time), p.vis, p.text);
    }
}
