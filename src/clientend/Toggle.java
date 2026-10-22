package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

final class Toggle extends JComponent {
    private boolean on;
    private final Consumer<Boolean> onChange;

    Toggle(boolean on, Consumer<Boolean> onChange) {
        this.on = on;
        this.onChange = onChange;
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Toggle.this.on = !Toggle.this.on;
                repaint();
                if (Toggle.this.onChange != null) {
                    Toggle.this.onChange.accept(Toggle.this.on);
                }
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(46, 28);
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        int w = getWidth();
        int h = getHeight();
        int tw = 42;
        int th = 24;
        int x = (w - tw) / 2;
        int y = (h - th) / 2;
        g.setColor(on ? Theme.primary : Theme.surface2);
        g.fillRoundRect(x, y, tw, th, th, th);
        g.setColor(Color.WHITE);
        int knob = th - 6;
        g.fillOval(on ? x + tw - knob - 3 : x + 3, y + 3, knob, knob);
        g.dispose();
    }
}
