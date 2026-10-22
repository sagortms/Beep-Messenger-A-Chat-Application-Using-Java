package clientend;

import javax.swing.*;
import java.awt.*;

// read only text you can select and copy (used for bubbles, posts, help)
final class WrapText extends JTextArea implements Theme.Restyle {
    enum Mode {PLAIN, MINE, OTHER, MUTED, ERROR, SUCCESS}

    private static final JTextArea MEASURER = new JTextArea();

    static {
        MEASURER.setLineWrap(true);
        MEASURER.setWrapStyleWord(true);
        MEASURER.setBorder(null);
        MEASURER.setMargin(new Insets(0, 0, 0, 0));
    }

    private float baseSize = 14;
    private int style = Font.PLAIN;
    private Mode mode = Mode.PLAIN;

    WrapText(String text) {
        super(text);
        setEditable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(null);
        setMargin(new Insets(0, 0, 0, 0));
        Theme.style(this, "wrap");
        restyle();
        UI.installEditMenu(this);
    }

    WrapText font(float size, int fontStyle) {
        this.baseSize = size;
        this.style = fontStyle;
        restyle();
        return this;
    }

    WrapText mode(Mode m) {
        this.mode = m;
        restyle();
        return this;
    }

    @Override
    public void restyle() {
        setFont(Theme.font(baseSize, style));
        switch (mode) {
            case MINE:
                setForeground(Theme.bubbleMineText);
                setSelectionColor(new Color(255, 255, 255, 90));
                break;
            case OTHER:
                setForeground(Theme.bubbleOtherText);
                break;
            case MUTED:
                setForeground(Theme.muted);
                break;
            case ERROR:
                setForeground(Theme.danger);
                break;
            case SUCCESS:
                setForeground(Theme.success);
                break;
            default:
                break;
        }
    }

    int heightFor(int width) {
        return measure(getText(), getFont(), width);
    }

    static int measure(String text, Font font, int width) {
        MEASURER.setFont(font);
        MEASURER.setText(text);
        MEASURER.setSize(Math.max(1, width), Short.MAX_VALUE);
        return MEASURER.getPreferredSize().height;
    }

    int naturalWidth() {
        FontMetrics fm = getFontMetrics(getFont());
        int widest = 0;
        for (String line : getText().split("\n", -1)) {
            widest = Math.max(widest, fm.stringWidth(line));
        }
        return widest + 4;
    }
}
