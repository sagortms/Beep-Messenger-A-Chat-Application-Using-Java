package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

final class Logo extends JComponent implements FlexList.WidthAware {
    private final int size;

    Logo(int size) {
        this.size = size;
    }

    static void draw(Graphics2D g, int x, int y, int size) {
        g.setColor(new Color(0x0A7CFF));
        g.fillRoundRect(x, y, size, size, size * 28 / 100, size * 28 / 100);
        g.setColor(Color.WHITE);
        int bw = size * 58 / 100;
        int bh = size * 40 / 100;
        int bx = x + (size - bw) / 2;
        int by = y + size * 24 / 100;
        g.fillRoundRect(bx, by, bw, bh, bh, bh);
        Polygon tail = new Polygon();
        tail.addPoint(bx + bw * 22 / 100, by + bh - 2);
        tail.addPoint(bx + bw * 14 / 100, by + bh + size * 12 / 100);
        tail.addPoint(bx + bw * 45 / 100, by + bh - 2);
        g.fillPolygon(tail);
        g.setColor(new Color(0x0A7CFF));
        int d = Math.max(3, size * 7 / 100);
        int cy = by + bh / 2 - d / 2;
        g.fillOval(bx + bw / 2 - d / 2 - d * 2, cy, d, d);
        g.fillOval(bx + bw / 2 - d / 2, cy, d, d);
        g.fillOval(bx + bw / 2 - d / 2 + d * 2, cy, d, d);
    }

    static BufferedImage image(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = UI.aa(img.getGraphics());
        draw(g, 0, 0, size);
        g.dispose();
        return img;
    }

    @Override
    public int heightFor(int width) {
        return size + 8;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(size, size);
    }

    @Override
    protected void paintComponent(Graphics g0) {
        Graphics2D g = UI.aa(g0);
        draw(g, (getWidth() - size) / 2, (getHeight() - size) / 2, size);
        g.dispose();
    }
}
