package clientend;

import javax.swing.*;
import java.awt.*;

final class HintArea extends JTextArea {
    private final String hint;

    HintArea(String hint) {
        this.hint = hint;
        setLineWrap(true);
        setWrapStyleWord(true);
        setBorder(null);
        setMargin(new Insets(0, 0, 0, 0));
        Theme.style(this, "inputClear");
        UI.installEditMenu(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        HintField.Look.hint(g, this, hint);
    }
}
