package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

final class ClientLoginPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final HintField username = new HintField("Username");
    private final HintField.Password password = new HintField.Password("Password");
    private final FlatButton login = new FlatButton("Log in", FlatButton.Kind.PRIMARY);
    private final FlatButton server = new FlatButton("", FlatButton.Kind.TEXT).size(12);
    private final TextRow note;

    ClientLoginPage(ClientModel model, Runnable onSignup, String message, String prefillUser) {
        super(new BorderLayout());
        this.model = model;
        Theme.style(this, "panel");

        note = TextRow.note(message == null ? " " : message, message == null ? WrapText.Mode.MUTED : WrapText.Mode.SUCCESS);
        if (prefillUser != null) {
            username.setText(prefillUser);
        }

        FlexList form = new FlexList().maxWidth(400).padding(80, 28, 28, 28);
        form.add(new Logo(68));
        form.add(centered(UI.label("Beep Messenger", "title")));
        form.add(UI.spacer(4));
        form.add(centered(UI.label("Log in to continue", "muted")));
        form.add(UI.spacer(22));
        form.add(username);
        form.add(UI.spacer(10));
        form.add(password);
        form.add(note);
        form.add(UI.spacer(4));
        form.add(login);
        form.add(UI.spacer(10));
        FlatButton create = new FlatButton("Create new account", FlatButton.Kind.SECONDARY);
        create.addActionListener(e -> onSignup.run());
        form.add(create);
        form.add(UI.spacer(14));
        form.add(centered(server));
        updateServerLabel();
        server.addActionListener(e -> changeServer());

        add(UI.scroll(form), BorderLayout.CENTER);

        ActionListener submit = e -> submit();
        login.addActionListener(submit);
        username.addActionListener(submit);
        password.addActionListener(submit);
        model.addListener(this);
        addHierarchyListener(e -> {
            if (isShowing()) {
                SwingUtilities.invokeLater(() -> (username.getText().isEmpty() ? username : password).requestFocusInWindow());
            }
        });
    }

    private static JComponent centered(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        Theme.style(p, "panel");
        p.add(c);
        return p;
    }

    private void updateServerLabel() {
        server.setText("Server: " + Settings.host + ":" + Settings.port + "  (change)");
        server.revalidate();
        server.repaint();
    }

    private void changeServer() {
        String input = JOptionPane.showInputDialog(this, "Server address (host:port)", Settings.host + ":" + Settings.port);
        if (input != null) {
            if (SettingsPage.applyServer(input)) {
                updateServerLabel();
            } else {
                JOptionPane.showMessageDialog(this, "Enter the address like 192.168.0.10:8080");
            }
        }
    }

    private void submit() {
        if (!login.isEnabled()) {
            return;
        }
        String u = username.getText().trim();
        String p = new String(password.getPassword());
        if (u.isEmpty() || p.isEmpty()) {
            showNote("Enter your username and password.", WrapText.Mode.ERROR);
            return;
        }
        login.setEnabled(false);
        showNote("Connecting...", WrapText.Mode.MUTED);
        model.login(u, p);
    }

    private void showNote(String text, WrapText.Mode mode) {
        note.bodyMode(mode);
        note.setBody(text);
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "login_fail":
            case "connect_fail":
                login.setEnabled(true);
                showNote((String) data, WrapText.Mode.ERROR);
                password.setText("");
                password.requestFocusInWindow();
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }
}
