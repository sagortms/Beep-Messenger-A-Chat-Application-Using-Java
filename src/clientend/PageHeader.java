package clientend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

final class PageHeader extends JPanel {
    private final JLabel titleLabel;

    PageHeader(String title, Runnable onBack) {
        super(new BorderLayout(4, 0));
        Theme.style(this, "panel");
        setBorder(new EmptyBorder(6, 6, 6, 12));
        if (onBack != null) {
            FlatButton back = new FlatButton("", FlatButton.Kind.TEXT).icon("back");
            back.setToolTipText("Back");
            back.addActionListener(e -> onBack.run());
            add(back, BorderLayout.WEST);
        } else {
            setBorder(new EmptyBorder(6, 16, 6, 12));
        }
        titleLabel = UI.label(title, "heading");
        add(titleLabel, BorderLayout.CENTER);
    }

    void setCenter(Component c) {
        remove(titleLabel);
        add(c, BorderLayout.CENTER);
    }

    void setEast(Component c) {
        add(c, BorderLayout.EAST);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(Theme.divider);
        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
    }
}
