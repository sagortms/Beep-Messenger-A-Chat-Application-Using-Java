package clientend;

import javax.swing.*;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

final class UI {
    private static final Color[] AVATAR_COLORS = {
            new Color(0x0A7CFF), new Color(0x7B4DFF), new Color(0xE0457B), new Color(0xE8710A),
            new Color(0x1E9E6A), new Color(0x00A3BF), new Color(0xB5651D), new Color(0x5C6BC0)};

    private UI() {
    }

    static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    static Color avatarColor(String key) {
        return AVATAR_COLORS[Math.abs((key == null ? "" : key).hashCode()) % AVATAR_COLORS.length];
    }

    static String initials(String name) {
        String[] parts = (name == null ? "" : name.trim()).split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) {
            return "?";
        }
        String first = parts[0].substring(0, parts[0].offsetByCodePoints(0, 1));
        if (parts.length == 1) {
            return first.toUpperCase();
        }
        String last = parts[parts.length - 1];
        return (first + last.substring(0, last.offsetByCodePoints(0, 1))).toUpperCase();
    }

    static void avatar(Graphics2D g, String name, String key, int x, int y, int size) {
        g.setColor(avatarColor(key));
        g.fillOval(x, y, size, size);
        g.setColor(Color.WHITE);
        g.setFont(Theme.font(size * 0.36f / (float) Theme.scale, Font.BOLD));
        FontMetrics fm = g.getFontMetrics();
        String s = initials(name);
        g.drawString(s, x + (size - fm.stringWidth(s)) / 2, y + (size - fm.getHeight()) / 2 + fm.getAscent());
    }

    static void onlineDot(Graphics2D g, int avatarX, int avatarY, int size) {
        int d = Math.max(10, size / 4);
        int x = avatarX + size - d;
        int y = avatarY + size - d;
        g.setColor(Theme.bg);
        g.fillOval(x - 2, y - 2, d + 4, d + 4);
        g.setColor(new Color(0x31A24C));
        g.fillOval(x, y, d, d);
    }

    static String ellipsize(String s, FontMetrics fm, int maxWidth) {
        if (s == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (fm.stringWidth(s) <= maxWidth) {
            return s;
        }
        String dots = "...";
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end)) + fm.stringWidth(dots) > maxWidth) {
            end--;
        }
        return s.substring(0, end).trim() + dots;
    }

    private static boolean sameDay(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    static String time(long ms) {
        return new SimpleDateFormat("h:mm a", Locale.ENGLISH).format(new Date(ms));
    }

    static String listTime(long ms) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ms);
        if (sameDay(now, then)) {
            return time(ms);
        }
        long days = (now.getTimeInMillis() - ms) / (24L * 3600 * 1000);
        if (days < 6) {
            return new SimpleDateFormat("EEE", Locale.ENGLISH).format(new Date(ms));
        }
        return new SimpleDateFormat("d MMM", Locale.ENGLISH).format(new Date(ms));
    }

    static String dayLabel(long ms) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ms);
        if (sameDay(now, then)) {
            return "Today";
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (sameDay(now, then)) {
            return "Yesterday";
        }
        return new SimpleDateFormat("EEE, d MMM yyyy", Locale.ENGLISH).format(new Date(ms));
    }

    static String postTime(long ms) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ms);
        if (sameDay(now, then)) {
            return "Today, " + time(ms);
        }
        return new SimpleDateFormat("d MMM yyyy, h:mm a", Locale.ENGLISH).format(new Date(ms));
    }

    static boolean sameDay(long a, long b) {
        Calendar x = Calendar.getInstance();
        Calendar y = Calendar.getInstance();
        x.setTimeInMillis(a);
        y.setTimeInMillis(b);
        return sameDay(x, y);
    }

    static String size(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format(Locale.ENGLISH, "%.1f KB", bytes / 1024.0);
        }
        return String.format(Locale.ENGLISH, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    static String preview(String kind, String text) {
        if ("F".equals(kind)) {
            return "Sent a file: " + text;
        }
        return text.replace('\n', ' ');
    }

    static JLabel label(String text, String role) {
        return Theme.style(new JLabel(text), role);
    }

    static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        Theme.style(sp, "panel");
        Theme.style(sp.getViewport(), "panel");
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.getVerticalScrollBar().setOpaque(false);
        return sp;
    }

    static JPanel tabHeader(String title, Component below) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        Theme.style(p, "panel");
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 10, 16));
        p.add(label(title, "title"), BorderLayout.NORTH);
        if (below != null) {
            p.add(below, BorderLayout.CENTER);
        }
        return p;
    }

    static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                r.run();
            }
        });
    }

    static JScrollPane transparentScroll(Component view) {
        JScrollPane sp = new JScrollPane(view, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setBorder(null);
        sp.setViewportBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        sp.getVerticalScrollBar().setOpaque(false);
        return sp;
    }

    static JLabel fieldLabel(String text) {
        JLabel l = label(text, "muted");
        l.setBorder(BorderFactory.createEmptyBorder(12, 2, 5, 0));
        return l;
    }

    static JLabel formSection(String text) {
        JLabel l = label(text, "muted");
        l.setFont(Theme.font(12, Font.BOLD));
        l.setBorder(BorderFactory.createEmptyBorder(22, 2, 4, 0));
        return l;
    }

    static JComponent spacer(int h) {
        return (JComponent) Box.createVerticalStrut(h);
    }

    static JPanel leftAligned(Component c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        Theme.style(p, "panel");
        p.add(c);
        return p;
    }

    static JLabel sectionLabel(String text) {
        JLabel l = label(text, "muted");
        l.setFont(Theme.font(12, Font.BOLD));
        l.setBorder(BorderFactory.createEmptyBorder(14, 16, 6, 16));
        return l;
    }

    static boolean confirm(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION;
    }

    static JPopupMenu beeperMenu(final Component parent, final ClientModel model, final ClientModel.Person p) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem remove = new JMenuItem("Remove " + p.fullName + " from beepers");
        remove.addActionListener(e -> {
            if (confirm(parent, "Remove " + p.fullName + " from your beepers?\nYou will not be able to message each other until you connect again.", "Remove beeper")) {
                model.removeBeeper(p.username);
            }
        });
        menu.add(remove);
        return menu;
    }

    static Frame frameOf(Component c) {
        Window w = SwingUtilities.getWindowAncestor(c);
        return w instanceof Frame ? (Frame) w : null;
    }

    static void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }

    static void installEditMenu(final JTextComponent c) {
        MouseAdapter popup = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                show(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                show(e);
            }

            private void show(MouseEvent e) {
                if (!e.isPopupTrigger()) {
                    return;
                }
                c.requestFocusInWindow();
                JPopupMenu menu = new JPopupMenu();
                boolean hasSelection = c.getSelectedText() != null;
                if (c.isEditable()) {
                    JMenuItem cut = new JMenuItem("Cut");
                    cut.setEnabled(hasSelection);
                    cut.addActionListener(a -> c.cut());
                    menu.add(cut);
                }
                JMenuItem copy = new JMenuItem(hasSelection || c.isEditable() ? "Copy" : "Copy message");
                copy.setEnabled(hasSelection || !c.isEditable());
                copy.addActionListener(a -> {
                    if (c.getSelectedText() != null) {
                        c.copy();
                    } else {
                        copyToClipboard(c.getText());
                    }
                });
                menu.add(copy);
                if (c.isEditable()) {
                    JMenuItem paste = new JMenuItem("Paste");
                    paste.addActionListener(a -> c.paste());
                    menu.add(paste);
                }
                JMenuItem all = new JMenuItem("Select all");
                all.addActionListener(a -> c.selectAll());
                menu.add(all);
                menu.show(c, e.getX(), e.getY());
            }
        };
        c.addMouseListener(popup);
    }

    // metal look and feel binds copy/paste to ctrl even on a mac, so add cmd as well
    static void installMacShortcuts() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) {
            return;
        }
        int meta = InputEvent.META_DOWN_MASK;
        String[] maps = {"TextField.focusInputMap", "TextArea.focusInputMap", "TextPane.focusInputMap",
                "EditorPane.focusInputMap", "FormattedTextField.focusInputMap", "PasswordField.focusInputMap"};
        for (String name : maps) {
            InputMap im = (InputMap) UIManager.get(name);
            if (im == null) {
                continue;
            }
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, meta), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, meta), DefaultEditorKit.selectAllAction);
            if (!name.startsWith("Password")) {
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, meta), DefaultEditorKit.copyAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, meta), DefaultEditorKit.cutAction);
            }
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, meta), DefaultEditorKit.beginLineAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, meta), DefaultEditorKit.endLineAction);
        }
    }
}
