package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class FlatButton extends JButton {
    enum Kind {PRIMARY, SECONDARY, TEXT, DANGER}

    private final Kind kind;
    private boolean hover;
    private boolean pressed;
    private String icon;
    private float fontSize = 14;
    private boolean pill;

    FlatButton(String text, Kind kind) {
        super(text);
        this.kind = kind;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder());
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    FlatButton icon(String name) {
        this.icon = name;
        return this;
    }

    FlatButton size(float pt) {
        this.fontSize = pt;
        return this;
    }

    FlatButton pill() {
        this.pill = true;
        return this;
    }

    private Font textFont() {
        return Theme.font(fontSize, Font.BOLD);
    }

    @Override
    public Dimension getPreferredSize() {
        if (icon != null) {
            int s = (int) (36 * Math.max(1.0, Math.min(1.2, Theme.scale)));
            return new Dimension(s, s);
        }
        FontMetrics fm = getFontMetrics(textFont());
        int padX = kind == Kind.TEXT ? 8 : 16;
        int padY = kind == Kind.TEXT ? 6 : 9;
        return new Dimension(fm.stringWidth(getText()) + padX * 2, fm.getHeight() + padY * 2);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        int w = getWidth();
        int h = getHeight();
        Color fill = null;
        Color fg;
        switch (kind) {
            case PRIMARY:
                fill = Theme.primary;
                fg = Theme.onPrimary;
                break;
            case SECONDARY:
                fill = Theme.surface2;
                fg = Theme.text;
                break;
            case DANGER:
                fg = Theme.danger;
                break;
            default:
                fg = Theme.primary;
                break;
        }
        if (!isEnabled()) {
            g.setComposite(AlphaComposite.SrcOver.derive(0.45f));
        }
        int arc = pill || icon != null ? h : 12;
        if (fill != null) {
            g.setColor(pressed ? fill.darker() : hover ? brighten(fill) : fill);
            g.fillRoundRect(0, 0, w, h, arc, arc);
        } else if (hover || pressed) {
            g.setColor(Theme.surface);
            g.fillRoundRect(0, 0, w, h, arc, arc);
        }
        g.setColor(fg);
        if (icon != null) {
            drawIcon(g, w, h);
        } else {
            g.setFont(textFont());
            FontMetrics fm = g.getFontMetrics();
            String s = UI.ellipsize(getText(), fm, w - 8);
            g.drawString(s, (w - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }

    private static Color brighten(Color c) {
        return Theme.dark ? new Color(Math.min(255, c.getRed() + 18), Math.min(255, c.getGreen() + 18), Math.min(255, c.getBlue() + 18))
                : c.darker();
    }

    private void drawIcon(Graphics2D g, int w, int h) {
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int cx = w / 2;
        int cy = h / 2;
        int r = Math.max(5, Math.min(w, h) / 5);
        if ("back".equals(icon)) {
            g.drawLine(cx + r / 2, cy - r, cx - r / 2, cy);
            g.drawLine(cx - r / 2, cy, cx + r / 2, cy + r);
        } else if ("plus".equals(icon)) {
            g.drawLine(cx - r, cy, cx + r, cy);
            g.drawLine(cx, cy - r, cx, cy + r);
        } else if ("more".equals(icon)) {
            g.fillOval(cx - 2, cy - r - 2, 4, 4);
            g.fillOval(cx - 2, cy - 2, 4, 4);
            g.fillOval(cx - 2, cy + r - 2, 4, 4);
        } else if ("send".equals(icon)) {
            Polygon plane = new Polygon();
            plane.addPoint(cx - r - 1, cy - r);
            plane.addPoint(cx + r + 2, cy);
            plane.addPoint(cx - r - 1, cy + r);
            plane.addPoint(cx - r / 2, cy);
            g.fillPolygon(plane);
        }
    }
}
