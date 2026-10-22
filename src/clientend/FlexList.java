package clientend;

import javax.swing.*;
import java.awt.*;

// vertical list where row height depends on width (wrapped text), so each row is asked for its height
final class FlexList extends JPanel implements Scrollable {
    interface WidthAware {
        int heightFor(int width);
    }

    private int maxWidth = Integer.MAX_VALUE;

    FlexList() {
        super(null);
        setLayout(new Layout());
        Theme.style(this, "panel");
    }

    FlexList maxWidth(int w) {
        this.maxWidth = w;
        return this;
    }

    FlexList padding(int top, int left, int bottom, int right) {
        setBorder(javax.swing.BorderFactory.createEmptyBorder(top, left, bottom, right));
        return this;
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return Math.max(40, visibleRect.height - 40);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    private static final class Layout implements LayoutManager {
        @Override
        public void addLayoutComponent(String name, Component comp) {
        }

        @Override
        public void removeLayoutComponent(Component comp) {
        }

        private static int widthOf(Container p) {
            Container parent = p.getParent();
            if (parent instanceof JViewport) {
                int vw = ((JViewport) parent).getExtentSize().width;
                if (vw > 0) {
                    return vw;
                }
            }
            return p.getWidth() > 0 ? p.getWidth() : 360;
        }

        private static int heightOf(Component c, int w) {
            if (c instanceof WidthAware) {
                return ((WidthAware) c).heightFor(w);
            }
            return c.getPreferredSize().height;
        }

        @Override
        public Dimension preferredLayoutSize(Container p) {
            Insets in = p.getInsets();
            int w = Math.min(widthOf(p) - in.left - in.right, ((FlexList) p).maxWidth);
            int h = in.top + in.bottom;
            for (Component c : p.getComponents()) {
                if (c.isVisible()) {
                    h += heightOf(c, w);
                }
            }
            return new Dimension(widthOf(p), h);
        }

        @Override
        public Dimension minimumLayoutSize(Container p) {
            return new Dimension(0, 0);
        }

        @Override
        public void layoutContainer(Container p) {
            Insets in = p.getInsets();
            int avail = p.getWidth() - in.left - in.right;
            int w = Math.min(avail, ((FlexList) p).maxWidth);
            int x = in.left + (avail - w) / 2;
            int y = in.top;
            for (Component c : p.getComponents()) {
                if (!c.isVisible()) {
                    continue;
                }
                int h = heightOf(c, w);
                c.setBounds(x, y, w, h);
                y += h;
            }
        }
    }
}
