package clientend;

import javax.swing.*;
import java.awt.*;

final class ClientWelcomePage extends JPanel {
    ClientWelcomePage() {
        super(new GridBagLayout());
        Theme.style(this, "panel");
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        Theme.style(col, "panel");
        Logo logo = new Logo(84);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel name = UI.label("Beep Messenger", "title");
        name.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tag = UI.label("Message the people you are connected with", "muted");
        tag.setAlignmentX(Component.CENTER_ALIGNMENT);
        col.add(logo);
        col.add(Box.createVerticalStrut(14));
        col.add(name);
        col.add(Box.createVerticalStrut(4));
        col.add(tag);
        add(col);
    }
}
