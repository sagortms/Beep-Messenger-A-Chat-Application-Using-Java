package serverend;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

// everything is saved as plain text files in the data folder. free text is base64 so a | in it cant break a line
public class Database {
    public static final class User {
        public final String username;
        public String fullName, phone, email, bio, salt, hash;
        public long created;

        User(String username) {
            this.username = username;
        }
    }

    public static final class Msg {
        public long id;
        public String from, to, kind, text;
        public long time, size;
    }

    public static final class Post {
        public long id;
        public String author, vis, text;
        public long time;
    }

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9_.]{3,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern PHONE = Pattern.compile("^\\+?[0-9]{7,15}$");

    private final File dir;
    private final File filesDir;
    private final Map<String, User> users = new LinkedHashMap<>();
    private final Map<String, Long> links = new LinkedHashMap<>();
    private final Map<String, Long> requests = new LinkedHashMap<>();
    private final Map<String, List<Msg>> conversations = new LinkedHashMap<>();
    private final Map<Long, Msg> messageById = new LinkedHashMap<>();
    private final List<Post> posts = new ArrayList<>();
    private final Map<String, Long> lastRead = new LinkedHashMap<>();
    private long nextMessageId = 1;
    private long nextPostId = 1;
    private final SecureRandom random = new SecureRandom();

    public Database(File dir) throws IOException {
        this.dir = dir;
        this.filesDir = new File(dir, "files");
        if (!filesDir.isDirectory() && !filesDir.mkdirs()) {
            throw new IOException("Cannot create data folder " + filesDir);
        }
        load();
        if (users.isEmpty()) {
            seedDemoUsers();
        }
    }

    private void seedDemoUsers() {
        register("Mutant Freeman", "mutant", "01912345678", "mutant@example.com", "mutant1234");
        register("Neon Saab", "neon", "01712345689", "neon@example.com", "neon1234");
    }

    public synchronized String register(String fullName, String username, String phone, String email, String password) {
        fullName = clean(fullName);
        username = clean(username).toLowerCase(Locale.ROOT);
        phone = normalizePhone(phone);
        email = clean(email);
        String problem = validateProfile(fullName, phone, email);
        if (problem != null) {
            return problem;
        }
        if (!USERNAME.matcher(username).matches()) {
            return "Username must be 3-20 characters: letters, numbers, dot or underscore.";
        }
        if (password == null || password.length() < 6) {
            return "Password must be at least 6 characters.";
        }
        if (users.containsKey(username)) {
            return "That username is already taken.";
        }
        for (User u : users.values()) {
            if (u.phone.equals(phone)) {
                return "That phone number is already registered.";
            }
            if (u.email.equalsIgnoreCase(email)) {
                return "That email address is already registered.";
            }
        }
        User u = new User(username);
        u.fullName = fullName;
        u.phone = phone;
        u.email = email;
        u.bio = "";
        u.salt = newSalt();
        u.hash = hash(password, u.salt);
        u.created = System.currentTimeMillis();
        users.put(username, u);
        saveUsers();
        return null;
    }

    private String validateProfile(String fullName, String phone, String email) {
        if (fullName.length() < 2 || fullName.length() > 40) {
            return "Full name must be 2-40 characters.";
        }
        if (!PHONE.matcher(phone).matches()) {
            return "Enter a valid phone number (7-15 digits).";
        }
        if (!EMAIL.matcher(email).matches() || email.length() > 80) {
            return "Enter a valid email address.";
        }
        return null;
    }

    public synchronized User authenticate(String username, String password) {
        User u = users.get(clean(username).toLowerCase(Locale.ROOT));
        if (u == null || password == null || !u.hash.equals(hash(password, u.salt))) {
            return null;
        }
        return u;
    }

    public synchronized User getUser(String username) {
        return users.get(clean(username).toLowerCase(Locale.ROOT));
    }

    public synchronized String updateProfile(String username, String fullName, String phone, String email, String bio) {
        User u = users.get(username);
        if (u == null) {
            return "Account not found.";
        }
        fullName = clean(fullName);
        phone = normalizePhone(phone);
        email = clean(email);
        bio = clean(bio);
        String problem = validateProfile(fullName, phone, email);
        if (problem != null) {
            return problem;
        }
        if (bio.length() > 120) {
            return "Bio can be at most 120 characters.";
        }
        for (User other : users.values()) {
            if (other == u) {
                continue;
            }
            if (other.phone.equals(phone)) {
                return "That phone number is already registered.";
            }
            if (other.email.equalsIgnoreCase(email)) {
                return "That email address is already registered.";
            }
        }
        u.fullName = fullName;
        u.phone = phone;
        u.email = email;
        u.bio = bio;
        saveUsers();
        return null;
    }

    public synchronized String changePassword(String username, String oldPassword, String newPassword) {
        User u = users.get(username);
        if (u == null || !u.hash.equals(hash(oldPassword, u.salt))) {
            return "Current password is wrong.";
        }
        if (newPassword == null || newPassword.length() < 6) {
            return "New password must be at least 6 characters.";
        }
        u.salt = newSalt();
        u.hash = hash(newPassword, u.salt);
        saveUsers();
        return null;
    }

    public synchronized List<User> search(String me, String query) {
        String q = clean(query).toLowerCase(Locale.ROOT);
        String phoneQuery = normalizePhone(query);
        List<User> found = new ArrayList<>();
        if (q.length() < 2) {
            return found;
        }
        for (User u : users.values()) {
            if (u.username.equals(me)) {
                continue;
            }
            boolean match = u.username.contains(q)
                    || u.fullName.toLowerCase(Locale.ROOT).contains(q)
                    || u.email.equalsIgnoreCase(q)
                    || (phoneQuery.length() >= 7 && u.phone.equals(phoneQuery));
            if (match) {
                found.add(u);
                if (found.size() >= 30) {
                    break;
                }
            }
        }
        return found;
    }

    private static String pair(String a, String b) {
        return a.compareTo(b) < 0 ? a + "|" + b : b + "|" + a;
    }

    public synchronized boolean areFriends(String a, String b) {
        return links.containsKey(pair(a, b));
    }

    public synchronized List<User> friendsOf(String user) {
        List<User> list = new ArrayList<>();
        for (String key : links.keySet()) {
            String[] p = key.split("\\|");
            String other = p[0].equals(user) ? p[1] : p[1].equals(user) ? p[0] : null;
            if (other != null && users.containsKey(other)) {
                list.add(users.get(other));
            }
        }
        return list;
    }

    public synchronized long friendSince(String a, String b) {
        Long t = links.get(pair(a, b));
        return t == null ? 0 : t;
    }

    public synchronized String relation(String me, String other) {
        if (areFriends(me, other)) {
            return "friend";
        }
        if (requests.containsKey(me + "|" + other)) {
            return "sent";
        }
        if (requests.containsKey(other + "|" + me)) {
            return "received";
        }
        return "none";
    }

    // returns SENT, CONNECTED (they already asked us) or ERR:reason
    public synchronized String sendRequest(String from, String to) {
        if (from.equals(to)) {
            return "ERR:You cannot send a Beep Request to yourself.";
        }
        if (!users.containsKey(to)) {
            return "ERR:User not found.";
        }
        if (areFriends(from, to)) {
            return "ERR:You are already connected.";
        }
        if (requests.containsKey(from + "|" + to)) {
            return "ERR:Beep Request already sent.";
        }
        if (requests.containsKey(to + "|" + from)) {
            acceptRequest(from, to);
            return "CONNECTED";
        }
        requests.put(from + "|" + to, System.currentTimeMillis());
        saveRequests();
        return "SENT";
    }

    public synchronized boolean acceptRequest(String me, String from) {
        if (requests.remove(from + "|" + me) == null) {
            return false;
        }
        requests.remove(me + "|" + from);
        links.put(pair(me, from), System.currentTimeMillis());
        saveRequests();
        saveLinks();
        return true;
    }

    public synchronized boolean removeRequest(String from, String to) {
        boolean removed = requests.remove(from + "|" + to) != null;
        if (removed) {
            saveRequests();
        }
        return removed;
    }

    public synchronized boolean removeFriend(String a, String b) {
        boolean removed = links.remove(pair(a, b)) != null;
        if (removed) {
            saveLinks();
        }
        return removed;
    }

    public synchronized List<Object[]> incomingRequests(String me) {
        List<Object[]> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : requests.entrySet()) {
            String[] p = e.getKey().split("\\|");
            if (p[1].equals(me)) {
                list.add(new Object[]{p[0], e.getValue()});
            }
        }
        Collections.reverse(list);
        return list;
    }

    public synchronized List<Object[]> outgoingRequests(String me) {
        List<Object[]> list = new ArrayList<>();
        for (Map.Entry<String, Long> e : requests.entrySet()) {
            String[] p = e.getKey().split("\\|");
            if (p[0].equals(me)) {
                list.add(new Object[]{p[1], e.getValue()});
            }
        }
        Collections.reverse(list);
        return list;
    }

    public synchronized Msg addMessage(String from, String to, String kind, String text, long size, byte[] fileBytes) throws IOException {
        Msg m = new Msg();
        m.id = nextMessageId++;
        m.from = from;
        m.to = to;
        m.time = System.currentTimeMillis();
        m.kind = kind;
        m.text = text;
        m.size = size;
        if (fileBytes != null) {
            Files.write(new File(filesDir, String.valueOf(m.id)).toPath(), fileBytes);
        }
        index(m);
        String line = m.id + "|" + m.from + "|" + m.to + "|" + m.time + "|" + m.kind + "|" + enc(m.text) + "|" + m.size;
        appendLine(new File(dir, "messages.txt"), line);
        return m;
    }

    private void index(Msg m) {
        String key = pair(m.from, m.to);
        List<Msg> list = conversations.get(key);
        if (list == null) {
            list = new ArrayList<>();
            conversations.put(key, list);
        }
        list.add(m);
        messageById.put(m.id, m);
        if (m.id >= nextMessageId) {
            nextMessageId = m.id + 1;
        }
    }

    public synchronized List<Msg> history(String a, String b, int limit) {
        List<Msg> list = conversations.get(pair(a, b));
        if (list == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(Math.max(0, list.size() - limit), list.size()));
    }

    public synchronized Msg getMessage(long id) {
        return messageById.get(id);
    }

    public synchronized byte[] readFile(long id) throws IOException {
        return Files.readAllBytes(new File(filesDir, String.valueOf(id)).toPath());
    }

    public synchronized void markRead(String user, String peer) {
        List<Msg> list = conversations.get(pair(user, peer));
        if (list == null || list.isEmpty()) {
            return;
        }
        long last = list.get(list.size() - 1).id;
        Long old = lastRead.get(user + "|" + peer);
        if (old == null || old < last) {
            lastRead.put(user + "|" + peer, last);
            saveReads();
        }
    }

    public synchronized List<Object[]> chatSummaries(String user) {
        List<Object[]> rows = new ArrayList<>();
        for (User f : friendsOf(user)) {
            List<Msg> list = conversations.get(pair(user, f.username));
            if (list == null || list.isEmpty()) {
                continue;
            }
            Long read = lastRead.get(user + "|" + f.username);
            long readId = read == null ? 0 : read;
            int unread = 0;
            for (Msg m : list) {
                if (m.from.equals(f.username) && m.id > readId) {
                    unread++;
                }
            }
            rows.add(new Object[]{f.username, list.get(list.size() - 1), unread});
        }
        return rows;
    }

    public synchronized Post addPost(String author, String vis, String text) {
        Post p = new Post();
        p.id = nextPostId++;
        p.author = author;
        p.vis = vis;
        p.text = text;
        p.time = System.currentTimeMillis();
        posts.add(p);
        savePosts();
        return p;
    }

    public synchronized boolean deletePost(long id, String author) {
        for (int i = 0; i < posts.size(); i++) {
            Post p = posts.get(i);
            if (p.id == id && p.author.equals(author)) {
                posts.remove(i);
                savePosts();
                return true;
            }
        }
        return false;
    }

    public synchronized boolean canSee(String viewer, Post p) {
        if (p.author.equals(viewer) || "public".equals(p.vis)) {
            return true;
        }
        return "beepers".equals(p.vis) && areFriends(viewer, p.author);
    }

    public synchronized List<Post> feedFor(String viewer, int limit) {
        List<Post> list = new ArrayList<>();
        for (int i = posts.size() - 1; i >= 0 && list.size() < limit; i--) {
            if (canSee(viewer, posts.get(i))) {
                list.add(posts.get(i));
            }
        }
        return list;
    }

    private static String clean(String s) {
        return s == null ? "" : s.trim();
    }

    private static String normalizePhone(String s) {
        return clean(s).replaceAll("[\\s\\-()]", "");
    }

    private static String enc(String s) {
        return Base64.getEncoder().encodeToString((s == null ? "" : s).getBytes(StandardCharsets.UTF_8));
    }

    private static String dec(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    private String newSalt() {
        byte[] b = new byte[16];
        random.nextBytes(b);
        return Base64.getEncoder().encodeToString(b);
    }

    private static String hash(String password, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec((password == null ? "" : password).toCharArray(), Base64.getDecoder().decode(salt), 10000, 256);
            byte[] key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(key);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private void load() throws IOException {
        for (String line : readLines("users.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 8) continue;
            User u = new User(p[0]);
            u.fullName = dec(p[1]);
            u.phone = p[2];
            u.email = dec(p[3]);
            u.bio = dec(p[4]);
            u.salt = p[5];
            u.hash = p[6];
            u.created = Long.parseLong(p[7]);
            users.put(u.username, u);
        }
        for (String line : readLines("links.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 3) links.put(p[0] + "|" + p[1], Long.parseLong(p[2]));
        }
        for (String line : readLines("requests.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 3) requests.put(p[0] + "|" + p[1], Long.parseLong(p[2]));
        }
        for (String line : readLines("messages.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 7) continue;
            Msg m = new Msg();
            m.id = Long.parseLong(p[0]);
            m.from = p[1];
            m.to = p[2];
            m.time = Long.parseLong(p[3]);
            m.kind = p[4];
            m.text = dec(p[5]);
            m.size = Long.parseLong(p[6]);
            index(m);
        }
        for (String line : readLines("posts.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length < 5) continue;
            Post post = new Post();
            post.id = Long.parseLong(p[0]);
            post.author = p[1];
            post.time = Long.parseLong(p[2]);
            post.vis = p[3];
            post.text = dec(p[4]);
            posts.add(post);
            nextPostId = Math.max(nextPostId, post.id + 1);
        }
        for (String line : readLines("reads.txt")) {
            String[] p = line.split("\\|", -1);
            if (p.length >= 3) lastRead.put(p[0] + "|" + p[1], Long.parseLong(p[2]));
        }
    }

    private List<String> readLines(String name) throws IOException {
        File f = new File(dir, name);
        List<String> lines = new ArrayList<>();
        if (!f.exists()) {
            return lines;
        }
        for (String line : Files.readAllLines(f.toPath(), StandardCharsets.UTF_8)) {
            if (!line.trim().isEmpty()) {
                lines.add(line);
            }
        }
        return lines;
    }

    private void saveUsers() {
        List<String> lines = new ArrayList<>();
        for (User u : users.values()) {
            lines.add(u.username + "|" + enc(u.fullName) + "|" + u.phone + "|" + enc(u.email) + "|" + enc(u.bio)
                    + "|" + u.salt + "|" + u.hash + "|" + u.created);
        }
        writeLines("users.txt", lines);
    }

    private void saveLinks() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> e : links.entrySet()) {
            lines.add(e.getKey() + "|" + e.getValue());
        }
        writeLines("links.txt", lines);
    }

    private void saveRequests() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> e : requests.entrySet()) {
            lines.add(e.getKey() + "|" + e.getValue());
        }
        writeLines("requests.txt", lines);
    }

    private void savePosts() {
        List<String> lines = new ArrayList<>();
        for (Post p : posts) {
            lines.add(p.id + "|" + p.author + "|" + p.time + "|" + p.vis + "|" + enc(p.text));
        }
        writeLines("posts.txt", lines);
    }

    private void saveReads() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<String, Long> e : lastRead.entrySet()) {
            lines.add(e.getKey() + "|" + e.getValue());
        }
        writeLines("reads.txt", lines);
    }

    // write to a temp file first so a crash cant leave a half written file
    private void writeLines(String name, List<String> lines) {
        try {
            File target = new File(dir, name);
            File tmp = new File(dir, name + ".tmp");
            Files.write(tmp.toPath(), lines, StandardCharsets.UTF_8);
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("[server] Could not save " + name + ": " + e.getMessage());
        }
    }

    private void appendLine(File f, String line) throws IOException {
        Files.write(f.toPath(), (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
    }
}
