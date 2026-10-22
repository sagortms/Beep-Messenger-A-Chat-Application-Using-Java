package clientend;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

class HintField extends JTextField {
    private final String hint;

    HintField(String hint) {
        this.hint = hint;
        Look.setup(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Look.background(g, this);
        super.paintComponent(g);
        Look.hint(g, this, hint);
    }

    static final class Password extends JPasswordField {
        private final String hint;

        Password(String hint) {
            this.hint = hint;
            Look.setup(this);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Look.background(g, this);
            super.paintComponent(g);
            Look.hint(g, this, hint);
        }
    }

    static final class Look {
        private Look() {
        }

        static void setup(final JTextComponent c) {
            Theme.style(c, "inputClear");
            c.setBorder(new EmptyBorder(11, 14, 11, 14));
            c.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    c.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    c.repaint();
                }
            });
            UI.installEditMenu(c);
        }

        static void background(Graphics g0, JTextComponent c) {
            Graphics2D g = UI.aa(g0);
            g.setColor(Theme.surface);
            g.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 14, 14);
            if (c.hasFocus()) {
                g.setColor(Theme.primary);
                g.setStroke(new BasicStroke(1.6f));
                g.drawRoundRect(1, 1, c.getWidth() - 3, c.getHeight() - 3, 13, 13);
            }
            g.dispose();
        }

        static void hint(Graphics g0, JTextComponent c, String hint) {
            if (hint == null || c.getDocument().getLength() > 0) {
                return;
            }
            Graphics2D g = UI.aa(g0);
            g.setColor(Theme.muted);
            g.setFont(c.getFont());
            Insets in = c.getInsets();
            FontMetrics fm = g.getFontMetrics();
            int baseline = c instanceof JTextArea
                    ? in.top + fm.getAscent()
                    : in.top + fm.getAscent() + (c.getHeight() - in.top - in.bottom - fm.getHeight()) / 2;
            g.drawString(hint, in.left, baseline);
            g.dispose();
        }
    }
}
