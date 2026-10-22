package clientend;

import javax.swing.*;
import java.awt.*;

final class TextRow extends JComponent implements FlexList.WidthAware {
    private final JLabel heading;
    private final WrapText body;
    private final int padX;
    private final int padTop;
    private final int padBottom;

    TextRow(String headingText, String bodyText, int padX, int padTop, int padBottom) {
        this.padX = padX;
        this.padTop = padTop;
        this.padBottom = padBottom;
        setLayout(null);
        if (headingText != null) {
            heading = UI.label(headingText, "bold");
            add(heading);
        } else {
            heading = null;
        }
        body = new WrapText(bodyText);
        add(body);
    }

    static TextRow empty(String heading, String body) {
        return new TextRow(heading, body, 20, 28, 12);
    }

    static TextRow note(String text, WrapText.Mode mode) {
        TextRow r = new TextRow(null, text, 0, 4, 4);
        r.body.font(12, Font.PLAIN).mode(mode);
        return r;
    }

    TextRow padded() {
        return new TextRow(null, body.getText(), 16, 8, 16).bodyMode(WrapText.Mode.MUTED).bodySize(12);
    }

    TextRow bodyMode(WrapText.Mode mode) {
        body.mode(mode);
        return this;
    }

    TextRow bodySize(float pt) {
        body.font(pt, Font.PLAIN);
        return this;
    }

    void setBody(String text) {
        body.setText(text);
        revalidate();
    }

    private int headingHeight() {
        return heading == null ? 0 : heading.getPreferredSize().height + 4;
    }

    @Override
    public int heightFor(int width) {
        return padTop + headingHeight() + body.heightFor(width - 2 * padX) + padBottom;
    }

    @Override
    public void doLayout() {
        int w = getWidth() - 2 * padX;
        int y = padTop;
        if (heading != null) {
            heading.setBounds(padX, y, w, heading.getPreferredSize().height);
            y += headingHeight();
        }
        body.setBounds(padX, y, w, body.heightFor(w));
    }
}
