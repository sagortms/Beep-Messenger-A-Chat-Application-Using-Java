package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class MenuRow extends JComponent implements FlexList.WidthAware {
    private final String text;
    private final boolean danger;
    private final Runnable onClick;
    private int badge;
    private boolean hover;

    MenuRow(String text, boolean danger, Runnable onClick) {
        this.text = text;
        this.danger = danger;
        this.onClick = onClick;
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
                MenuRow.this.onClick.run();
            }
        });
    }

    MenuRow badge(int n) {
        this.badge = n;
        return this;
    }

    @Override
    public int heightFor(int width) {
        return getFontMetrics(Theme.font(15, Font.PLAIN)).getHeight() + 30;
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
        g.setFont(Theme.font(15, Font.PLAIN));
        FontMetrics fm = g.getFontMetrics();
        g.setColor(danger ? Theme.danger : Theme.text);
        g.drawString(text, 16, (h - fm.getHeight()) / 2 + fm.getAscent());
        int right = w - 18;
        if (!danger) {
            g.setColor(Theme.muted);
            g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawLine(right - 5, h / 2 - 6, right, h / 2);
            g.drawLine(right, h / 2, right - 5, h / 2 + 6);
            right -= 18;
        }
        if (badge > 0) {
            String s = String.valueOf(badge);
            g.setFont(Theme.font(11, Font.BOLD));
            FontMetrics bf = g.getFontMetrics();
            int bh = bf.getHeight() + 2;
            int bw = Math.max(bh, bf.stringWidth(s) + 12);
            g.setColor(Theme.danger);
            g.fillRoundRect(right - bw, (h - bh) / 2, bw, bh, bh, bh);
            g.setColor(Color.WHITE);
            g.drawString(s, right - bw + (bw - bf.stringWidth(s)) / 2, (h - bh) / 2 + (bh - bf.getHeight()) / 2 + bf.getAscent());
        }
        g.dispose();
    }
}
