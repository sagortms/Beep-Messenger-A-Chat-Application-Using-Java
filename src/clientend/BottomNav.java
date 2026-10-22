package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

final class BottomNav extends JComponent {
    private final String[] names;
    private final int[] badges;
    private final IntConsumer onSelect;
    private int selected;

    BottomNav(String[] names, IntConsumer onSelect) {
        this.names = names;
        this.badges = new int[names.length];
        this.onSelect = onSelect;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = Math.min(BottomNav.this.names.length - 1, e.getX() * BottomNav.this.names.length / Math.max(1, getWidth()));
                select(i);
                BottomNav.this.onSelect.accept(i);
            }
        });
    }

    void select(int i) {
        selected = i;
        repaint();
    }

    void setBadge(int index, int count) {
        if (badges[index] != count) {
            badges[index] = count;
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.font(11, Font.BOLD));
        return new Dimension(300, 14 + 26 + fm.getHeight() + 4);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.bg);
        g.fillRect(0, 0, w, h);
        g.setColor(Theme.divider);
        g.drawLine(0, 0, w, 0);
        int cell = w / names.length;
        g.setFont(Theme.font(11, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < names.length; i++) {
            boolean on = i == selected;
            Color c = on ? Theme.primary : Theme.muted;
            int cx = i * cell + cell / 2;
            int iconTop = 8;
            g.setColor(c);
            drawIcon(g, i, cx, iconTop, on);
            g.setColor(c);
            g.drawString(names[i], cx - fm.stringWidth(names[i]) / 2, iconTop + 26 + 4 + fm.getAscent());
            if (badges[i] > 0) {
                String s = badges[i] > 99 ? "99+" : String.valueOf(badges[i]);
                g.setFont(Theme.font(10, Font.BOLD));
                FontMetrics bf = g.getFontMetrics();
                int bh = bf.getHeight();
                int bw = Math.max(bh, bf.stringWidth(s) + 8);
                int bx = cx + 6;
                int by = iconTop - 4;
                g.setColor(Theme.danger);
                g.fillRoundRect(bx, by, bw, bh, bh, bh);
                g.setColor(Color.WHITE);
                g.drawString(s, bx + (bw - bf.stringWidth(s)) / 2, by + bf.getAscent());
                g.setFont(Theme.font(11, Font.BOLD));
            }
        }
        g.dispose();
    }

    private void drawIcon(Graphics2D g, int index, int cx, int top, boolean filled) {
        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int x = cx - 12;
        int y = top + 1;
        switch (index) {
            case 0: { // chat bubble
                Shape bubble = new java.awt.geom.RoundRectangle2D.Float(x, y + 1, 24, 17, 9, 9);
                java.awt.geom.Path2D tail = new java.awt.geom.Path2D.Float();
                tail.moveTo(x + 6, y + 16);
                tail.lineTo(x + 4, y + 23);
                tail.lineTo(x + 12, y + 17);
                if (filled) {
                    g.fill(bubble);
                    g.fill(tail);
                } else {
                    g.draw(bubble);
                    g.draw(tail);
                }
                break;
            }
            case 1: { // people
                if (filled) {
                    g.fillOval(x + 7, y, 10, 10);
                    g.fill(new java.awt.geom.Arc2D.Float(x + 1, y + 12, 22, 20, 0, 180, java.awt.geom.Arc2D.PIE));
                } else {
                    g.drawOval(x + 7, y, 10, 10);
                    g.draw(new java.awt.geom.Arc2D.Float(x + 2, y + 13, 20, 18, 0, 180, java.awt.geom.Arc2D.OPEN));
                }
                break;
            }
            case 2: { // feed card with lines
                Shape card = new java.awt.geom.RoundRectangle2D.Float(x + 1, y + 1, 22, 21, 6, 6);
                if (filled) {
                    g.fill(card);
                    g.setColor(Theme.bg);
                } else {
                    g.draw(card);
                }
                g.drawLine(x + 6, y + 8, x + 18, y + 8);
                g.drawLine(x + 6, y + 14, x + 14, y + 14);
                break;
            }
            default: // menu
                g.drawLine(x + 2, y + 4, x + 22, y + 4);
                g.drawLine(x + 2, y + 11, x + 22, y + 11);
                g.drawLine(x + 2, y + 18, x + 22, y + 18);
                break;
        }
    }
}
