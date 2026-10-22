package clientend;

import javax.swing.*;

// start ServerSide first, then this. optional args: serverHost serverPort
public class ClientSide {
    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Beep Messenger");
        Settings.load();
        if (args.length > 0) {
            Settings.host = args[0];
        }
        if (args.length > 1) {
            try {
                Settings.port = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
            }
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            UI.installMacShortcuts();
            Theme.apply(Settings.dark, Settings.scale());
            new MainFrame();
        });
    }
}
