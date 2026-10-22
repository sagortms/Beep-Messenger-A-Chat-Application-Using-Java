package clientend;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

final class Settings {
    static final String[] SIZE_NAMES = {"Small", "Medium", "Large", "X-Large"};
    static final double[] SIZE_SCALES = {0.9, 1.0, 1.15, 1.3};

    static boolean dark = false;
    static int textLevel = 1;
    static boolean enterToSend = true;
    static boolean sound = true;
    static String host = "127.0.0.1";
    static int port = 8080;

    private Settings() {
    }

    static double scale() {
        return SIZE_SCALES[textLevel];
    }

    private static File file() {
        return new File(new File(System.getProperty("user.home"), ".beep_messenger"), "client.properties");
    }

    static void load() {
        Properties p = new Properties();
        File f = file();
        if (!f.exists()) {
            return;
        }
        try (FileInputStream in = new FileInputStream(f)) {
            p.load(in);
            dark = "dark".equals(p.getProperty("theme", "light"));
            textLevel = Math.max(0, Math.min(SIZE_SCALES.length - 1, Integer.parseInt(p.getProperty("textLevel", "1"))));
            enterToSend = Boolean.parseBoolean(p.getProperty("enterToSend", "true"));
            sound = Boolean.parseBoolean(p.getProperty("sound", "true"));
            host = p.getProperty("host", host);
            port = Integer.parseInt(p.getProperty("port", String.valueOf(port)));
        } catch (IOException | NumberFormatException e) {
            System.err.println("Could not read settings, using defaults: " + e.getMessage());
        }
    }

    static void save() {
        Properties p = new Properties();
        p.setProperty("theme", dark ? "dark" : "light");
        p.setProperty("textLevel", String.valueOf(textLevel));
        p.setProperty("enterToSend", String.valueOf(enterToSend));
        p.setProperty("sound", String.valueOf(sound));
        p.setProperty("host", host);
        p.setProperty("port", String.valueOf(port));
        File f = file();
        f.getParentFile().mkdirs();
        try (FileOutputStream out = new FileOutputStream(f)) {
            p.store(out, "Beep Messenger client settings");
        } catch (IOException e) {
            System.err.println("Could not save settings: " + e.getMessage());
        }
    }
}
