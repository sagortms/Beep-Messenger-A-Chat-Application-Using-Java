package clientend;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class SettingsPage extends JPanel {
    SettingsPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        Theme.style(this, "panel");
        add(new PageHeader("Settings", host::back), BorderLayout.NORTH);

        FlexList form = new FlexList().maxWidth(480).padding(4, 18, 24, 18);

        form.add(UI.formSection("APPEARANCE"));
        form.add(UI.fieldLabel("Mode"));
        form.add(new Segmented(new String[]{"Light", "Dark"}, Settings.dark ? 1 : 0, i -> {
            Settings.dark = i == 1;
            Settings.save();
            host.restyleAll();
        }));
        form.add(UI.fieldLabel("Text size"));
        form.add(new Segmented(Settings.SIZE_NAMES, Settings.textLevel, i -> {
            Settings.textLevel = i;
            Settings.save();
            host.restyleAll();
        }));
        form.add(UI.spacer(8));
        form.add(new TextRow(null, "This is how text will look. The quick brown fox jumps over the lazy dog.", 2, 4, 4)
                .bodyMode(WrapText.Mode.MUTED).bodySize(14));

        form.add(UI.formSection("CHAT"));
        form.add(new ToggleRow("Press Enter to send", "Off: Enter adds a new line, Ctrl/Cmd+Enter sends", Settings.enterToSend, on -> {
            Settings.enterToSend = on;
            Settings.save();
        }));
        form.add(new ToggleRow("Message sound", "Play a beep when a message arrives in another chat", Settings.sound, on -> {
            Settings.sound = on;
            Settings.save();
        }));

        form.add(UI.formSection("CONNECTION"));
        form.add(UI.fieldLabel("Server address (host:port)"));
        final HintField server = new HintField("127.0.0.1:8080");
        server.setText(Settings.host + ":" + Settings.port);
        form.add(server);
        final TextRow note = TextRow.note("Used the next time you log in.", WrapText.Mode.MUTED);
        FlatButton save = new FlatButton("Save address", FlatButton.Kind.SECONDARY);
        save.addActionListener(e -> {
            if (applyServer(server.getText())) {
                note.bodyMode(WrapText.Mode.SUCCESS);
                note.setBody("Saved. It will be used the next time you log in.");
            } else {
                note.bodyMode(WrapText.Mode.ERROR);
                note.setBody("Enter the address like 192.168.0.10:8080");
            }
        });
        form.add(UI.spacer(10));
        form.add(save);
        form.add(note);

        add(UI.scroll(form), BorderLayout.CENTER);
    }

    static boolean applyServer(String text) {
        String t = text.trim();
        if (t.isEmpty()) {
            return false;
        }
        String host = t;
        int port = 8080;
        int colon = t.lastIndexOf(':');
        if (colon >= 0) {
            host = t.substring(0, colon).trim();
            try {
                port = Integer.parseInt(t.substring(colon + 1).trim());
            } catch (NumberFormatException e) {
                return false;
            }
        }
        if (host.isEmpty() || port < 1 || port > 65535) {
            return false;
        }
        Settings.host = host;
        Settings.port = port;
        Settings.save();
        return true;
    }

    private static final class ToggleRow extends JComponent implements FlexList.WidthAware {
        private final TextRow text;
        private final Toggle toggle;

        ToggleRow(String title, String hint, boolean on, Consumer<Boolean> onChange) {
            setLayout(null);
            text = new TextRow(title, hint, 2, 8, 8).bodyMode(WrapText.Mode.MUTED).bodySize(12);
            toggle = new Toggle(on, onChange);
            add(text);
            add(toggle);
        }

        @Override
        public int heightFor(int width) {
            return Math.max(text.heightFor(width - 60), 44);
        }

        @Override
        public void doLayout() {
            int w = getWidth();
            text.setBounds(0, 0, w - 58, text.heightFor(w - 58));
            Dimension d = toggle.getPreferredSize();
            toggle.setBounds(w - d.width, (getHeight() - d.height) / 2, d.width, d.height);
        }
    }
}
