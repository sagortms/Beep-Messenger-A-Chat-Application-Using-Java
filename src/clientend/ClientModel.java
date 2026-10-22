package clientend;

import common.Packet;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

// what the client knows (beepers, chats, requests, feed). pages read this and listen for events
final class ClientModel implements ClientConnection.Handler {
    interface Listener {
        void on(String event, Object data);
    }

    static final class Person {
        String username, fullName;
        boolean online;
        long since;
    }

    static final class Msg {
        long id, time, size;
        String from, to, kind, text;
    }

    static final class Chat {
        String lastFrom = "", lastKind = "T", lastText = "";
        long lastTime;
        int unread;
    }

    static final class Post {
        long id, time;
        String author, authorName, vis, text;
    }

    static final class Req {
        String username, fullName;
        long time;
    }

    static final class Found {
        String username, fullName, relation;
    }

    final ClientConnection conn = new ClientConnection(this);

    String username = "", fullName = "", phone = "", email = "", bio = "";
    final Map<String, Person> friends = new LinkedHashMap<>();
    final Map<String, Chat> chats = new HashMap<>();
    final List<Req> requestsIn = new ArrayList<>();
    final List<Req> requestsOut = new ArrayList<>();
    final List<Post> posts = new ArrayList<>();
    String searchQuery = "";
    final List<Found> searchResults = new ArrayList<>();
    String openChat;

    private final Map<String, List<Msg>> messages = new HashMap<>();
    private final Set<String> historyLoaded = new HashSet<>();
    private final Map<Long, Consumer<Packet>> pendingFiles = new HashMap<>();
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private boolean loggedIn;

    void addListener(Listener l) {
        listeners.add(l);
    }

    void removeListener(Listener l) {
        listeners.remove(l);
    }

    private void fire(String event, Object data) {
        for (Listener l : listeners) {
            l.on(event, data);
        }
    }

    boolean isLoggedIn() {
        return loggedIn;
    }

    void connectThen(final Packet first) {
        new Thread(() -> {
            try {
                if (!conn.isConnected()) {
                    conn.connect(Settings.host, Settings.port);
                }
                SwingUtilities.invokeLater(() -> conn.send(first));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> fire("connect_fail",
                        "Cannot reach the server at " + Settings.host + ":" + Settings.port + ". Is ServerSide running?"));
            }
        }, "beep-connect").start();
    }

    void login(String user, String pass) {
        connectThen(new Packet("LOGIN", user, pass));
    }

    void signup(String fullName, String user, String phone, String email, String pass) {
        connectThen(new Packet("SIGNUP", fullName, user, phone, email, pass));
    }

    void logout() {
        if (conn.isConnected()) {
            conn.send(new Packet("LOGOUT"));
        }
        // give the writer thread a moment to flush LOGOUT before the socket closes
        new Thread(() -> {
            try {
                Thread.sleep(150);
            } catch (InterruptedException ignored) {
            }
            conn.close();
        }).start();
        reset();
    }

    void reset() {
        loggedIn = false;
        username = fullName = phone = email = bio = "";
        friends.clear();
        chats.clear();
        requestsIn.clear();
        requestsOut.clear();
        posts.clear();
        searchResults.clear();
        searchQuery = "";
        messages.clear();
        historyLoaded.clear();
        pendingFiles.clear();
        openChat = null;
    }

    void search(String query) {
        searchQuery = query;
        conn.send(new Packet("SEARCH", query));
    }

    void sendRequest(String user) {
        conn.send(new Packet("BEEP_REQUEST", user));
    }

    void accept(String user) {
        conn.send(new Packet("BEEP_ACCEPT", user));
    }

    void decline(String user) {
        conn.send(new Packet("BEEP_DECLINE", user));
    }

    void cancelRequest(String user) {
        conn.send(new Packet("BEEP_CANCEL", user));
    }

    void removeBeeper(String user) {
        conn.send(new Packet("BEEP_REMOVE", user));
    }

    void sendText(String peer, String text) {
        conn.send(new Packet("MSG_SEND", peer, text));
    }

    void sendFile(final String peer, final File f) {
        if (!f.isFile()) {
            fire("error", "That is not a file.");
            return;
        }
        if (f.length() == 0) {
            fire("error", "The file is empty.");
            return;
        }
        if (f.length() > Packet.MAX_FILE_BYTES) {
            fire("error", "File is too large (max 25 MB).");
            return;
        }
        fire("notice", "Sending " + f.getName() + "...");
        new Thread(() -> {
            try {
                final byte[] bytes = Files.readAllBytes(f.toPath());
                SwingUtilities.invokeLater(() -> conn.send(Packet.withData("FILE_SEND", bytes, peer, f.getName())));
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> fire("error", "Could not read " + f.getName()));
            }
        }, "beep-file-read").start();
    }

    void requestFile(long id, Consumer<Packet> callback) {
        pendingFiles.put(id, callback);
        conn.send(new Packet("FILE_GET", String.valueOf(id)));
    }

    void createPost(String vis, String text) {
        conn.send(new Packet("POST_CREATE", vis, text));
    }

    void deletePost(long id) {
        conn.send(new Packet("POST_DELETE", String.valueOf(id)));
    }

    void refreshFeed() {
        conn.send(new Packet("FEED_GET"));
    }

    void updateProfile(String full, String phone, String email, String bio) {
        conn.send(new Packet("PROFILE_UPDATE", full, phone, email, bio));
    }

    void changePassword(String oldPassword, String newPassword) {
        conn.send(new Packet("PASSWORD_CHANGE", oldPassword, newPassword));
    }

    List<Msg> messagesWith(String peer) {
        List<Msg> list = messages.get(peer);
        return list == null ? new ArrayList<Msg>() : new ArrayList<>(list);
    }

    boolean historyLoaded(String peer) {
        return historyLoaded.contains(peer);
    }

    void openChat(String peer) {
        openChat = peer;
        if (!historyLoaded.contains(peer)) {
            conn.send(new Packet("HISTORY", peer));
        }
        markRead(peer);
    }

    void closeChat(String peer) {
        if (peer.equals(openChat)) {
            openChat = null;
        }
    }

    void markRead(String peer) {
        Chat c = chats.get(peer);
        if (c != null && c.unread > 0) {
            c.unread = 0;
            fire("chats", null);
        }
        conn.send(new Packet("MARK_READ", peer));
    }

    int unreadTotal() {
        int n = 0;
        for (Chat c : chats.values()) {
            n += c.unread;
        }
        return n;
    }

    String displayName(String user) {
        if (user.equals(username)) {
            return fullName;
        }
        Person p = friends.get(user);
        return p == null ? user : p.fullName;
    }

    @Override
    public void onClosed() {
        boolean was = loggedIn;
        loggedIn = false;
        fire("disconnected", was);
    }

    @Override
    public void onPacket(Packet p) {
        switch (p.type) {
            case "LOGIN_OK":
                loggedIn = true;
                username = p.arg(0);
                fullName = p.arg(1);
                phone = p.arg(2);
                email = p.arg(3);
                bio = p.arg(4);
                fire("login_ok", null);
                break;
            case "LOGIN_FAIL":
                fire("login_fail", p.arg(0));
                break;
            case "SIGNUP_OK":
                fire("signup_ok", null);
                break;
            case "SIGNUP_FAIL":
                fire("signup_fail", p.arg(0));
                break;
            case "FRIENDS":
                readFriends(p);
                break;
            case "REQ_IN":
                readRequests(p, requestsIn);
                break;
            case "REQ_OUT":
                readRequests(p, requestsOut);
                break;
            case "CHATS":
                readChats(p);
                break;
            case "PRESENCE": {
                Person f = friends.get(p.arg(0));
                if (f != null) {
                    f.online = "1".equals(p.arg(1));
                    fire("presence", f);
                }
                break;
            }
            case "MSG_NEW":
                readNewMessage(p);
                break;
            case "HISTORY":
                readHistory(p);
                break;
            case "FILE_DATA": {
                Consumer<Packet> cb = pendingFiles.remove(Long.parseLong(p.arg(0)));
                if (cb != null) {
                    cb.accept(p);
                }
                break;
            }
            case "FEED":
                readFeed(p);
                break;
            case "POST_NEW": {
                Post post = parsePost(p, 0);
                for (Post old : posts) {
                    if (old.id == post.id) {
                        return;
                    }
                }
                posts.add(0, post);
                fire("feed", null);
                if (!post.author.equals(username)) {
                    fire("notice", post.authorName + " shared a post.");
                }
                break;
            }
            case "POST_DELETED": {
                long id = Long.parseLong(p.arg(0));
                for (int i = 0; i < posts.size(); i++) {
                    if (posts.get(i).id == id) {
                        posts.remove(i);
                        fire("feed", null);
                        break;
                    }
                }
                break;
            }
            case "SEARCH_RESULT":
                if (p.arg(0).equals(searchQuery)) {
                    searchResults.clear();
                    for (int i = 1; i + 2 < p.args.length; i += 3) {
                        Found f = new Found();
                        f.username = p.args[i];
                        f.fullName = p.args[i + 1];
                        f.relation = p.args[i + 2];
                        searchResults.add(f);
                    }
                    fire("search", null);
                }
                break;
            case "BEEP_REMOVED": {
                String peer = p.arg(0);
                messages.remove(peer);
                historyLoaded.remove(peer);
                fire("friend_removed", peer);
                break;
            }
            case "PROFILE_OK":
                fullName = p.arg(0);
                phone = p.arg(1);
                email = p.arg(2);
                bio = p.arg(3);
                fire("profile_ok", null);
                break;
            case "PROFILE_FAIL":
                fire("profile_fail", p.arg(0));
                break;
            case "PASSWORD_OK":
                fire("password_ok", null);
                break;
            case "PASSWORD_FAIL":
                fire("password_fail", p.arg(0));
                break;
            case "NOTICE":
                fire("notice", p.arg(0));
                break;
            case "ERROR":
                fire("error", p.arg(0));
                break;
            default:
                break;
        }
    }

    private void readFriends(Packet p) {
        Map<String, Person> old = new HashMap<>(friends);
        friends.clear();
        for (int i = 0; i + 3 < p.args.length; i += 4) {
            Person f = new Person();
            f.username = p.args[i];
            f.fullName = p.args[i + 1];
            f.online = "1".equals(p.args[i + 2]);
            f.since = Long.parseLong(p.args[i + 3]);
            friends.put(f.username, f);
        }
        fire("friends", old);
    }

    private void readRequests(Packet p, List<Req> target) {
        target.clear();
        for (int i = 0; i + 2 < p.args.length; i += 3) {
            Req r = new Req();
            r.username = p.args[i];
            r.fullName = p.args[i + 1];
            r.time = Long.parseLong(p.args[i + 2]);
            target.add(r);
        }
        fire("requests", null);
    }

    private void readChats(Packet p) {
        chats.clear();
        for (int i = 0; i + 5 < p.args.length; i += 6) {
            Chat c = new Chat();
            c.lastFrom = p.args[i + 1];
            c.lastTime = Long.parseLong(p.args[i + 2]);
            c.lastKind = p.args[i + 3];
            c.lastText = p.args[i + 4];
            c.unread = p.args[i].equals(openChat) ? 0 : Integer.parseInt(p.args[i + 5]);
            chats.put(p.args[i], c);
        }
        fire("chats", null);
    }

    private static Msg parseMessage(String[] a, int i) {
        Msg m = new Msg();
        m.id = Long.parseLong(a[i]);
        m.from = a[i + 1];
        m.time = Long.parseLong(a[i + 2]);
        m.kind = a[i + 3];
        m.text = a[i + 4];
        m.size = Long.parseLong(a[i + 5]);
        return m;
    }

    private List<Msg> listFor(String peer) {
        List<Msg> list = messages.get(peer);
        if (list == null) {
            list = new ArrayList<>();
            messages.put(peer, list);
        }
        return list;
    }

    private void readNewMessage(Packet p) {
        Msg m = new Msg();
        m.id = Long.parseLong(p.arg(0));
        m.from = p.arg(1);
        m.to = p.arg(2);
        m.time = Long.parseLong(p.arg(3));
        m.kind = p.arg(4);
        m.text = p.arg(5);
        m.size = Long.parseLong(p.arg(6));
        boolean mine = m.from.equals(username);
        String peer = mine ? m.to : m.from;
        List<Msg> list = listFor(peer);
        for (Msg old : list) {
            if (old.id == m.id) {
                return;
            }
        }
        list.add(m);
        Chat c = chats.get(peer);
        if (c == null) {
            c = new Chat();
            chats.put(peer, c);
        }
        c.lastFrom = m.from;
        c.lastKind = m.kind;
        c.lastText = m.text;
        c.lastTime = m.time;
        if (!mine) {
            if (peer.equals(openChat)) {
                conn.send(new Packet("MARK_READ", peer));
            } else {
                c.unread++;
            }
        }
        fire("chats", null);
        fire("message", m);
    }

    private void readHistory(Packet p) {
        String peer = p.arg(0);
        List<Msg> list = listFor(peer);
        Set<Long> have = new HashSet<>();
        for (Msg m : list) {
            have.add(m.id);
        }
        for (int i = 1; i + 5 < p.args.length; i += 6) {
            Msg m = parseMessage(p.args, i);
            m.to = m.from.equals(username) ? peer : username;
            if (have.add(m.id)) {
                list.add(m);
            }
        }
        Collections.sort(list, new Comparator<Msg>() {
            @Override
            public int compare(Msg a, Msg b) {
                return Long.compare(a.id, b.id);
            }
        });
        historyLoaded.add(peer);
        fire("history", peer);
    }

    private static Post parsePost(Packet p, int i) {
        Post post = new Post();
        post.id = Long.parseLong(p.args[i]);
        post.author = p.args[i + 1];
        post.authorName = p.args[i + 2];
        post.time = Long.parseLong(p.args[i + 3]);
        post.vis = p.args[i + 4];
        post.text = p.args[i + 5];
        return post;
    }

    private void readFeed(Packet p) {
        posts.clear();
        for (int i = 0; i + 5 < p.args.length; i += 6) {
            posts.add(parsePost(p, i));
        }
        fire("feed", null);
    }
}
