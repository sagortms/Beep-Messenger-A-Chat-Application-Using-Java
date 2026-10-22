package clientend;

import javax.swing.*;
import java.awt.*;

final class AccountPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final HintField fullName = new HintField("Full name");
    private final HintField username = new HintField("Username");
    private final HintField phone = new HintField("Phone number");
    private final HintField email = new HintField("Email address");
    private final HintField bio = new HintField("A short bio (optional)");
    private final HintField.Password oldPass = new HintField.Password("Current password");
    private final HintField.Password newPass = new HintField.Password("New password (at least 6 characters)");
    private final HintField.Password newPass2 = new HintField.Password("Repeat new password");
    private final TextRow profileNote = TextRow.note(" ", WrapText.Mode.MUTED);
    private final TextRow passwordNote = TextRow.note(" ", WrapText.Mode.MUTED);

    AccountPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        Theme.style(this, "panel");
        add(new PageHeader("Account", host::back), BorderLayout.NORTH);

        fullName.setText(model.fullName);
        username.setText("@" + model.username);
        username.setEditable(false);
        username.setForeground(Theme.muted);
        phone.setText(model.phone);
        email.setText(model.email);
        bio.setText(model.bio);

        FlexList form = new FlexList().maxWidth(480).padding(8, 18, 24, 18);
        form.add(new Banner());
        form.add(UI.fieldLabel("Full name"));
        form.add(fullName);
        form.add(UI.fieldLabel("Username (cannot be changed)"));
        form.add(username);
        form.add(UI.fieldLabel("Phone number"));
        form.add(phone);
        form.add(UI.fieldLabel("Email"));
        form.add(email);
        form.add(UI.fieldLabel("Bio"));
        form.add(bio);
        form.add(UI.spacer(14));
        FlatButton save = new FlatButton("Save changes", FlatButton.Kind.PRIMARY);
        save.addActionListener(e -> {
            profileNote.bodyMode(WrapText.Mode.MUTED);
            profileNote.setBody("Saving...");
            model.updateProfile(fullName.getText(), phone.getText(), email.getText(), bio.getText());
        });
        form.add(save);
        form.add(profileNote);

        form.add(UI.formSection("CHANGE PASSWORD"));
        form.add(oldPass);
        form.add(UI.spacer(8));
        form.add(newPass);
        form.add(UI.spacer(8));
        form.add(newPass2);
        form.add(UI.spacer(14));
        FlatButton change = new FlatButton("Update password", FlatButton.Kind.SECONDARY);
        change.addActionListener(e -> changePassword());
        form.add(change);
        form.add(passwordNote);

        add(UI.scroll(form), BorderLayout.CENTER);
        model.addListener(this);
    }

    private void changePassword() {
        String a = new String(oldPass.getPassword());
        String b = new String(newPass.getPassword());
        String c = new String(newPass2.getPassword());
        if (a.isEmpty() || b.isEmpty()) {
            passwordNote.bodyMode(WrapText.Mode.ERROR);
            passwordNote.setBody("Fill in your current and new password.");
        } else if (!b.equals(c)) {
            passwordNote.bodyMode(WrapText.Mode.ERROR);
            passwordNote.setBody("The new passwords do not match.");
        } else {
            passwordNote.bodyMode(WrapText.Mode.MUTED);
            passwordNote.setBody("Updating...");
            model.changePassword(a, b);
        }
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "profile_ok":
                profileNote.bodyMode(WrapText.Mode.SUCCESS);
                profileNote.setBody("Profile saved.");
                repaint();
                break;
            case "profile_fail":
                profileNote.bodyMode(WrapText.Mode.ERROR);
                profileNote.setBody((String) data);
                break;
            case "password_ok":
                passwordNote.bodyMode(WrapText.Mode.SUCCESS);
                passwordNote.setBody("Password updated.");
                oldPass.setText("");
                newPass.setText("");
                newPass2.setText("");
                break;
            case "password_fail":
                passwordNote.bodyMode(WrapText.Mode.ERROR);
                passwordNote.setBody((String) data);
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }

    private final class Banner extends JComponent implements FlexList.WidthAware {
        @Override
        public int heightFor(int width) {
            return 130;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            int size = 72;
            UI.avatar(g, model.fullName, model.username, (getWidth() - size) / 2, 14, size);
            g.setFont(Theme.font(17, Font.BOLD));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.text);
            String name = UI.ellipsize(model.fullName, fm, getWidth() - 20);
            g.drawString(name, (getWidth() - fm.stringWidth(name)) / 2, 14 + size + 8 + fm.getAscent());
            g.dispose();
        }
    }
}
