package clientend;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.ColorUIResource;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;

// light/dark colours and text size. every component has a role, refresh() re-applies them all
final class Theme {
    static final String FAMILY = "SansSerif";

    static boolean dark;
    static double scale = 1.0;

    static Color bg, surface, surface2, text, muted, primary, onPrimary, divider;
    static Color bubbleMine, bubbleMineText, bubbleOther, bubbleOtherText, danger, success, hover;

    private Theme() {
    }

    static void apply(boolean darkMode, double textScale) {
        dark = darkMode;
        scale = textScale;
        if (dark) {
            bg = new Color(0x18191A);
            surface = new Color(0x242526);
            surface2 = new Color(0x3A3B3C);
            text = new Color(0xE4E6EB);
            muted = new Color(0xA8ABB0);
            primary = new Color(0x2D88FF);
            divider = new Color(0x3E4042);
            bubbleOther = new Color(0x3A3B3C);
            bubbleOtherText = new Color(0xE4E6EB);
            hover = new Color(0x2A2B2D);
            danger = new Color(0xFF5C70);
            success = new Color(0x45BD62);
        } else {
            bg = Color.WHITE;
            surface = new Color(0xF0F2F5);
            surface2 = new Color(0xE4E6EB);
            text = new Color(0x050505);
            muted = new Color(0x65676B);
            primary = new Color(0x0A7CFF);
            divider = new Color(0xE4E6EB);
            bubbleOther = new Color(0xE9EBEE);
            bubbleOtherText = new Color(0x050505);
            hover = new Color(0xF5F6F7);
            danger = new Color(0xD93025);
            success = new Color(0x1E8E3E);
        }
        onPrimary = Color.WHITE;
        bubbleMine = primary;
        bubbleMineText = Color.WHITE;

        // dialogs and popup menus are built fresh each time, so they pick these up
        ColorUIResource b = new ColorUIResource(bg), t = new ColorUIResource(text);
        String[] backgrounds = {"Panel.background", "OptionPane.background", "PopupMenu.background", "MenuItem.background",
                "Button.background", "TextField.background", "ScrollPane.background"};
        for (String k : backgrounds) {
            UIManager.put(k, b);
        }
        String[] foregrounds = {"Label.foreground", "OptionPane.messageForeground", "OptionPane.foreground", "MenuItem.foreground",
                "Button.foreground", "TextField.foreground", "PopupMenu.foreground"};
        for (String k : foregrounds) {
            UIManager.put(k, t);
        }
        UIManager.put("MenuItem.selectionBackground", new ColorUIResource(surface2));
        UIManager.put("MenuItem.selectionForeground", t);
        UIManager.put("PopupMenu.border", BorderFactory.createLineBorder(divider));
        UIManager.put("MenuItem.font", font(13, Font.PLAIN));
        UIManager.put("OptionPane.messageFont", font(13, Font.PLAIN));
        UIManager.put("OptionPane.buttonFont", font(13, Font.PLAIN));
        UIManager.put("Label.font", font(13, Font.PLAIN));
        UIManager.put("Button.font", font(13, Font.PLAIN));
    }

    static Font font(float size, int style) {
        return new Font(FAMILY, style, Math.round((float) (size * scale)));
    }

    static <T extends JComponent> T style(T c, String role) {
        c.putClientProperty("role", role);
        applyRole(c, role);
        return c;
    }

    static void applyRole(JComponent c, String role) {
        switch (role) {
            case "panel":
                c.setOpaque(true);
                c.setBackground(bg);
                break;
            case "surface":
                c.setOpaque(true);
                c.setBackground(surface);
                break;
            case "clear":
                c.setOpaque(false);
                break;
            case "body":
                label(c, text, 14, Font.PLAIN);
                break;
            case "bold":
                label(c, text, 14, Font.BOLD);
                break;
            case "title":
                label(c, text, 26, Font.BOLD);
                break;
            case "heading":
                label(c, text, 17, Font.BOLD);
                break;
            case "muted":
                label(c, muted, 12, Font.PLAIN);
                break;
            case "mutedBody":
                label(c, muted, 14, Font.PLAIN);
                break;
            case "error":
                label(c, danger, 12, Font.PLAIN);
                break;
            case "success":
                label(c, success, 12, Font.PLAIN);
                break;
            case "link":
                label(c, primary, 12, Font.BOLD);
                break;
            case "input":
            case "inputClear":
                input(c, role.equals("input"));
                break;
            case "wrap":
                input(c, false);
                break;
            default:
                break;
        }
    }

    private static void label(JComponent c, Color fg, float size, int style) {
        c.setOpaque(false);
        c.setForeground(fg);
        c.setFont(font(size, style));
    }

    private static void input(JComponent c, boolean filled) {
        c.setOpaque(filled);
        c.setBackground(surface);
        c.setForeground(text);
        c.setFont(font(14, Font.PLAIN));
        if (c instanceof JTextComponent) {
            JTextComponent t = (JTextComponent) c;
            t.setCaretColor(text);
            t.setSelectionColor(primary);
            t.setSelectedTextColor(Color.WHITE);
            t.setDisabledTextColor(muted);
        }
    }

    static void refresh(Component root) {
        walk(root);
        if (root instanceof JComponent) {
            ((JComponent) root).revalidate();
        }
        root.repaint();
    }

    private static void walk(Component c) {
        if (c instanceof JComponent) {
            JComponent jc = (JComponent) c;
            Object role = jc.getClientProperty("role");
            if (role instanceof String) {
                applyRole(jc, (String) role);
            }
            if (jc instanceof Restyle) {
                ((Restyle) jc).restyle();
            }
            jc.invalidate();
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) {
                walk(child);
            }
        }
    }

    interface Restyle {
        void restyle();
    }
}
