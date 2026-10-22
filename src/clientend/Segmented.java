package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

final class Segmented extends JComponent {
    private final String[] labels;
    private final IntConsumer onChange;
    private int selected;
    private float fontSize = 13;

    Segmented(String[] labels, int selected, IntConsumer onChange) {
        this.labels = labels;
        this.selected = selected;
        this.onChange = onChange;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int i = Math.min(Segmented.this.labels.length - 1, Math.max(0, e.getX() * Segmented.this.labels.length / Math.max(1, getWidth())));
                setSelected(i);
                if (Segmented.this.onChange != null) {
                    Segmented.this.onChange.accept(i);
                }
            }
        });
    }

    Segmented size(float pt) {
        this.fontSize = pt;
        return this;
    }

    int getSelected() {
        return selected;
    }

    void setSelected(int i) {
        if (i != selected) {
            selected = i;
            repaint();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(Theme.font(fontSize, Font.BOLD));
        int widest = 0;
        for (String s : labels) {
            widest = Math.max(widest, fm.stringWidth(s));
        }
        return new Dimension((widest + 28) * labels.length, fm.getHeight() + 16);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(40, getPreferredSize().height);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        int w = getWidth();
        int h = getHeight();
        g.setColor(Theme.surface);
        g.fillRoundRect(0, 0, w, h, h, h);
        int cell = w / labels.length;
        g.setFont(Theme.font(fontSize, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        for (int i = 0; i < labels.length; i++) {
            int x = i * cell;
            int cw = i == labels.length - 1 ? w - x : cell;
            if (i == selected) {
                g.setColor(Theme.primary);
                g.fillRoundRect(x + 3, 3, cw - 6, h - 6, h - 6, h - 6);
                g.setColor(Theme.onPrimary);
            } else {
                g.setColor(Theme.muted);
            }
            String s = UI.ellipsize(labels[i], fm, cw - 8);
            g.drawString(s, x + (cw - fm.stringWidth(s)) / 2, (h - fm.getHeight()) / 2 + fm.getAscent());
        }
        g.dispose();
    }
}
