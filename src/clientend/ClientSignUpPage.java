package clientend;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

final class ClientSignUpPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final Consumer<String> onDone;
    private final HintField fullName = new HintField("Full name");
    private final HintField username = new HintField("Username (letters, numbers, . or _)");
    private final HintField phone = new HintField("Phone number");
    private final HintField email = new HintField("Email address");
    private final HintField.Password password = new HintField.Password("Password (at least 6 characters)");
    private final HintField.Password confirm = new HintField.Password("Repeat password");
    private final FlatButton create = new FlatButton("Create account", FlatButton.Kind.PRIMARY);
    private final TextRow note = TextRow.note(" ", WrapText.Mode.MUTED);

    ClientSignUpPage(ClientModel model, Runnable onBack, Consumer<String> onDone) {
        super(new BorderLayout());
        this.model = model;
        this.onDone = onDone;
        Theme.style(this, "panel");
        add(new PageHeader("Create account", onBack), BorderLayout.NORTH);

        FlexList form = new FlexList().maxWidth(420).padding(16, 24, 24, 24);
        form.add(fullName);
        form.add(UI.spacer(10));
        form.add(username);
        form.add(UI.spacer(10));
        form.add(phone);
        form.add(UI.spacer(10));
        form.add(email);
        form.add(UI.spacer(10));
        form.add(password);
        form.add(UI.spacer(10));
        form.add(confirm);
        form.add(note);
        form.add(UI.spacer(4));
        form.add(create);
        form.add(UI.spacer(10));
        FlatButton back = new FlatButton("I already have an account", FlatButton.Kind.TEXT);
        back.addActionListener(e -> onBack.run());
        form.add(UI.leftAligned(back));
        add(UI.scroll(form), BorderLayout.CENTER);

        create.addActionListener(e -> submit());
        confirm.addActionListener(e -> submit());
        model.addListener(this);
        addHierarchyListener(e -> {
            if (isShowing()) {
                SwingUtilities.invokeLater(fullName::requestFocusInWindow);
            }
        });
    }

    private void submit() {
        if (!create.isEnabled()) {
            return;
        }
        String pass = new String(password.getPassword());
        if (fullName.getText().trim().isEmpty() || username.getText().trim().isEmpty() || phone.getText().trim().isEmpty()
                || email.getText().trim().isEmpty() || pass.isEmpty()) {
            showNote("Please fill in every field.", WrapText.Mode.ERROR);
        } else if (!pass.equals(new String(confirm.getPassword()))) {
            showNote("The two passwords do not match.", WrapText.Mode.ERROR);
        } else {
            create.setEnabled(false);
            showNote("Creating your account...", WrapText.Mode.MUTED);
            model.signup(fullName.getText(), username.getText(), phone.getText(), email.getText(), pass);
        }
    }

    private void showNote(String text, WrapText.Mode mode) {
        note.bodyMode(mode);
        note.setBody(text);
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "signup_ok":
                onDone.accept(username.getText().trim().toLowerCase());
                break;
            case "signup_fail":
            case "connect_fail":
                create.setEnabled(true);
                showNote((String) data, WrapText.Mode.ERROR);
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
