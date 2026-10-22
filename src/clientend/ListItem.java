package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

final class ListItem extends JComponent implements FlexList.WidthAware {
    private static final int AVATAR = 48;

    private final String name;
    private final String key;
    private String title = "";
    private String subtitle = "";
    private String rightText;
    private int badge;
    private boolean online;
    private boolean bold;
    private boolean hover;
    private Runnable onClick;
    private Supplier<JPopupMenu> menu;
    private final List<FlatButton> actions = new ArrayList<>();

    ListItem(String name, String key) {
        this.name = name;
        this.key = key;
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
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && onClick != null) {
                    onClick.run();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                popup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                popup(e);
            }

            private void popup(MouseEvent e) {
                if (e.isPopupTrigger() && menu != null) {
                    menu.get().show(ListItem.this, e.getX(), e.getY());
                }
            }
        });
    }

    ListItem title(String t) {
        this.title = t;
        return this;
    }

    ListItem subtitle(String t) {
        this.subtitle = t;
        return this;
    }

    ListItem rightText(String t) {
        this.rightText = t;
        return this;
    }

    ListItem badge(int n) {
        this.badge = n;
        return this;
    }

    ListItem online(boolean b) {
        this.online = b;
        return this;
    }

    ListItem bold(boolean b) {
        this.bold = b;
        return this;
    }

    ListItem onClick(Runnable r) {
        this.onClick = r;
        return this;
    }

    ListItem menu(Supplier<JPopupMenu> m) {
        this.menu = m;
        return this;
    }

    ListItem action(FlatButton b) {
        actions.add(b);
        add(b);
        return this;
    }

    private Font titleFont() {
        return Theme.font(15, Font.BOLD);
    }

    private Font subFont() {
        return Theme.font(13, bold ? Font.BOLD : Font.PLAIN);
    }

    @Override
    public int heightFor(int width) {
        FontMetrics tf = getFontMetrics(titleFont());
        FontMetrics sf = getFontMetrics(subFont());
        return Math.max(AVATAR, tf.getHeight() + sf.getHeight() + 2) + 20;
    }

    @Override
    public void doLayout() {
        int x = getWidth() - 14;
        for (int i = actions.size() - 1; i >= 0; i--) {
            FlatButton b = actions.get(i);
            Dimension d = b.getPreferredSize();
            x -= d.width;
            b.setBounds(x, (getHeight() - d.height) / 2, d.width, d.height);
            x -= 8;
        }
    }

    private int actionsWidth() {
        int w = 0;
        for (FlatButton b : actions) {
            w += b.getPreferredSize().width + 8;
        }
        return w;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        int w = getWidth();
        int h = getHeight();
        if (hover) {
            g.setColor(Theme.hover);
            g.fillRect(0, 0, w, h);
        }
        int ax = 16;
        int ay = (h - AVATAR) / 2;
        UI.avatar(g, name, key, ax, ay, AVATAR);
        if (online) {
            UI.onlineDot(g, ax, ay, AVATAR);
        }

        int textX = ax + AVATAR + 12;
        int right = w - 14 - actionsWidth();
        if (rightText != null) {
            g.setFont(Theme.font(11, bold ? Font.BOLD : Font.PLAIN));
            FontMetrics rf = g.getFontMetrics();
            int rw = rf.stringWidth(rightText);
            g.setColor(bold ? Theme.primary : Theme.muted);
            g.drawString(rightText, right - rw, h / 2 - 4);
            right -= rw + 10;
        }
        int badgeW = 0;
        if (badge > 0) {
            String s = badge > 99 ? "99+" : String.valueOf(badge);
            g.setFont(Theme.font(11, Font.BOLD));
            FontMetrics bf = g.getFontMetrics();
            int bh = bf.getHeight() + 2;
            badgeW = Math.max(bh, bf.stringWidth(s) + 12);
            int bx = w - 14 - actionsWidth() - badgeW;
            int by = h / 2 + 2;
            g.setColor(Theme.primary);
            g.fillRoundRect(bx, by, badgeW, bh, bh, bh);
            g.setColor(Color.WHITE);
            g.drawString(s, bx + (badgeW - bf.stringWidth(s)) / 2, by + (bh - bf.getHeight()) / 2 + bf.getAscent());
            badgeW += 8;
        }

        FontMetrics tf = g.getFontMetrics(titleFont());
        FontMetrics sf = g.getFontMetrics(subFont());
        int blockH = tf.getHeight() + sf.getHeight() + 2;
        int top = (h - blockH) / 2;
        g.setFont(titleFont());
        g.setColor(Theme.text);
        g.drawString(UI.ellipsize(title, tf, right - textX), textX, top + tf.getAscent());
        g.setFont(subFont());
        g.setColor(bold ? Theme.text : Theme.muted);
        g.drawString(UI.ellipsize(subtitle, sf, w - 14 - actionsWidth() - badgeW - textX), textX, top + tf.getHeight() + 2 + sf.getAscent());
        g.dispose();
    }
}
