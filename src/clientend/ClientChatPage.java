package clientend;

import common.Packet;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ClientChatPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private static final long GROUP_GAP_MS = 5 * 60 * 1000L;

    private final ClientModel model;
    private final AppHost host;
    private final String peer;
    private final FlexList list = new FlexList().padding(8, 0, 8, 0);
    private final JScrollPane scroll = UI.scroll(list);
    private final HintArea input = new HintArea("Type a message");
    private final JScrollPane inputScroll = UI.transparentScroll(input);
    private final JPanel bar = new JPanel(new BorderLayout(8, 0));
    private final Title title = new Title();
    private final Set<Long> rendered = new HashSet<>();
    private ClientModel.Msg lastMsg;
    private MessageRow lastRow;
    private TextRow emptyHint;
    private boolean stick = true;

    ClientChatPage(ClientModel model, AppHost host, String peer) {
        super(new BorderLayout());
        this.model = model;
        this.host = host;
        this.peer = peer;
        Theme.style(this, "panel");

        PageHeader header = new PageHeader("", host::back);
        header.setCenter(title);
        FlatButton more = new FlatButton("", FlatButton.Kind.TEXT).icon("more");
        more.setToolTipText("More");
        more.addActionListener(e -> {
            ClientModel.Person p = model.friends.get(peer);
            if (p != null) {
                UI.beeperMenu(this, model, p).show(more, more.getWidth() - 10, more.getHeight());
            }
        });
        header.setEast(more);
        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(buildBar(), BorderLayout.SOUTH);
        installFileDrop();
        installScrollTracking();

        model.addListener(this);
        model.openChat(peer);
        renderAll();
    }

    String peer() {
        return peer;
    }

    private JComponent buildBar() {
        Theme.style(bar, "panel");
        bar.setBorder(new EmptyBorder(8, 8, 10, 10));

        FlatButton attach = new FlatButton("", FlatButton.Kind.TEXT).icon("plus");
        attach.setToolTipText("Send a file (or drag one into this window)");
        attach.addActionListener(e -> chooseFiles());

        JPanel box = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g0) {
                Graphics2D g = UI.aa(g0);
                g.setColor(Theme.surface);
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.dispose();
            }
        };
        box.setOpaque(false);
        box.setBorder(new EmptyBorder(9, 14, 9, 8));
        box.add(inputScroll, BorderLayout.CENTER);

        FlatButton send = new FlatButton("", FlatButton.Kind.TEXT).icon("send");
        send.setToolTipText("Send");
        send.addActionListener(e -> send());

        bar.add(attach, BorderLayout.WEST);
        bar.add(box, BorderLayout.CENTER);
        bar.add(send, BorderLayout.EAST);

        InputMap im = input.getInputMap();
        ActionMap am = input.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "enter");
        im.put(KeyStroke.getKeyStroke("shift ENTER"), "newline");
        im.put(KeyStroke.getKeyStroke("ctrl ENTER"), "send");
        im.put(KeyStroke.getKeyStroke("meta ENTER"), "send");
        am.put("enter", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (Settings.enterToSend) {
                    send();
                } else {
                    input.replaceSelection("\n");
                }
            }
        });
        am.put("newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                input.replaceSelection("\n");
            }
        });
        am.put("send", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                send();
            }
        });
        UI.onChange(input, () -> SwingUtilities.invokeLater(this::resizeInput));
        inputScroll.setPreferredSize(new Dimension(10, input.getFontMetrics(input.getFont()).getHeight()));
        return bar;
    }

    private void resizeInput() {
        int lineH = input.getFontMetrics(input.getFont()).getHeight();
        int w = inputScroll.getViewport().getExtentSize().width;
        if (w <= 0) {
            w = 240;
        }
        int h = WrapText.measure(input.getText().isEmpty() ? " " : input.getText(), input.getFont(), w);
        h = Math.max(lineH, Math.min(h, lineH * 5));
        Dimension d = new Dimension(10, h);
        if (!d.equals(inputScroll.getPreferredSize())) {
            inputScroll.setPreferredSize(d);
            bar.revalidate();
        }
    }

    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        if (text.length() > Packet.MAX_MESSAGE_CHARS) {
            host.toast("Message is too long (max " + Packet.MAX_MESSAGE_CHARS + " characters).");
            return;
        }
        stick = true;
        model.sendText(peer, text);
        input.setText("");
        resizeInput();
    }

    private void chooseFiles() {
        FileDialog fd = new FileDialog(host.frame(), "Choose a file to send", FileDialog.LOAD);
        fd.setMultipleMode(true);
        fd.setVisible(true);
        File[] files = fd.getFiles();
        if (files != null) {
            for (File f : files) {
                stick = true;
                model.sendFile(peer, f);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void installFileDrop() {
        TransferHandler drop = new TransferHandler() {
            @Override
            public boolean canImport(TransferSupport support) {
                return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
            }

            @Override
            public boolean importData(TransferSupport support) {
                if (!canImport(support)) {
                    return false;
                }
                try {
                    List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    for (File f : files) {
                        stick = true;
                        model.sendFile(peer, f);
                    }
                    return true;
                } catch (UnsupportedFlavorException | IOException e) {
                    return false;
                }
            }
        };
        setTransferHandler(drop);
        list.setTransferHandler(drop);
        scroll.setTransferHandler(drop);
        scroll.getViewport().setTransferHandler(drop);
    }

    private void renderAll() {
        list.removeAll();
        rendered.clear();
        lastMsg = null;
        lastRow = null;
        emptyHint = null;
        List<ClientModel.Msg> msgs = model.messagesWith(peer);
        for (ClientModel.Msg m : msgs) {
            appendRow(m);
        }
        if (msgs.isEmpty() && model.historyLoaded(peer)) {
            emptyHint = TextRow.empty(null, "No messages yet. Say hi to " + model.displayName(peer) + "!");
            list.add(emptyHint);
        }
        list.revalidate();
        list.repaint();
        scrollToBottomLater();
    }

    private void appendRow(ClientModel.Msg m) {
        if (!rendered.add(m.id)) {
            return;
        }
        if (emptyHint != null) {
            list.remove(emptyHint);
            emptyHint = null;
        }
        boolean sameDay = lastMsg != null && UI.sameDay(lastMsg.time, m.time);
        if (!sameDay) {
            list.add(new DateRow(m.time));
        }
        boolean grouped = sameDay && lastRow != null && lastMsg.from.equals(m.from) && m.time - lastMsg.time < GROUP_GAP_MS;
        if (grouped) {
            lastRow.setShowTime(false);
        }
        MessageRow row = new MessageRow(m, grouped);
        list.add(row);
        lastRow = row;
        lastMsg = m;
    }

    private void scrollToBottomLater() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar b = scroll.getVerticalScrollBar();
            b.setValue(b.getMaximum());
            SwingUtilities.invokeLater(() -> b.setValue(b.getMaximum()));
        });
    }

    private boolean nearBottom() {
        JScrollBar b = scroll.getVerticalScrollBar();
        return b.getValue() + b.getVisibleAmount() >= b.getMaximum() - 80;
    }

    // "stick" only changes when the user scrolls, not when new messages make the list longer
    private void installScrollTracking() {
        scroll.addMouseWheelListener((MouseWheelEvent e) -> SwingUtilities.invokeLater(() -> stick = nearBottom()));
        scroll.getVerticalScrollBar().addAdjustmentListener(e -> {
            if (e.getValueIsAdjusting()) {
                stick = nearBottom();
            }
        });
        list.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (stick) {
                    scrollToBottomLater();
                }
            }
        });
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "message": {
                ClientModel.Msg m = (ClientModel.Msg) data;
                String other = m.from.equals(model.username) ? m.to : m.from;
                if (other.equals(peer)) {
                    if (m.from.equals(model.username)) {
                        stick = true;
                    }
                    appendRow(m);
                    list.revalidate();
                    list.repaint();
                    if (stick) {
                        scrollToBottomLater();
                    }
                }
                break;
            }
            case "history":
                if (peer.equals(data)) {
                    renderAll();
                }
                break;
            case "presence":
                title.repaint();
                break;
            case "friends":
                title.repaint();
                if (!model.friends.containsKey(peer)) {
                    host.back();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void dispose() {
        model.removeListener(this);
        model.closeChat(peer);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        SwingUtilities.invokeLater(input::requestFocusInWindow);
    }

    private void saveFile(final ClientModel.Msg m) {
        host.toast("Downloading " + m.text + "...");
        model.requestFile(m.id, p -> SwingUtilities.invokeLater(() -> {
            FileDialog fd = new FileDialog(host.frame(), "Save file", FileDialog.SAVE);
            fd.setFile(m.text);
            fd.setVisible(true);
            if (fd.getFile() == null) {
                return;
            }
            File target = new File(fd.getDirectory(), fd.getFile());
            try {
                Files.write(target.toPath(), p.data);
                host.toast("Saved " + target.getName());
            } catch (IOException e) {
                host.toast("Could not save the file: " + e.getMessage());
            }
        }));
    }

    private final class Title extends JComponent {
        @Override
        public Dimension getPreferredSize() {
            return new Dimension(200, 44);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            int size = 38;
            int y = (getHeight() - size) / 2;
            ClientModel.Person p = model.friends.get(peer);
            String name = model.displayName(peer);
            UI.avatar(g, name, peer, 0, y, size);
            if (p != null && p.online) {
                UI.onlineDot(g, 0, y, size);
            }
            Font nf = Theme.font(15, Font.BOLD);
            Font sf = Theme.font(12, Font.PLAIN);
            FontMetrics nm = g.getFontMetrics(nf);
            FontMetrics sm = g.getFontMetrics(sf);
            int top = (getHeight() - nm.getHeight() - sm.getHeight()) / 2;
            int x = size + 10;
            g.setFont(nf);
            g.setColor(Theme.text);
            g.drawString(UI.ellipsize(name, nm, getWidth() - x), x, top + nm.getAscent());
            g.setFont(sf);
            g.setColor(Theme.muted);
            g.drawString(p != null && p.online ? "Active now" : "@" + peer, x, top + nm.getHeight() + sm.getAscent());
            g.dispose();
        }
    }

    private static final class DateRow extends JComponent implements FlexList.WidthAware {
        private final String text;

        DateRow(long time) {
            this.text = UI.dayLabel(time);
        }

        @Override
        public int heightFor(int width) {
            return getFontMetrics(Theme.font(12, Font.PLAIN)).getHeight() + 20;
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            g.setFont(Theme.font(12, Font.PLAIN));
            FontMetrics fm = g.getFontMetrics();
            g.setColor(Theme.muted);
            g.drawString(text, (getWidth() - fm.stringWidth(text)) / 2, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }

    private final class MessageRow extends JComponent implements FlexList.WidthAware {
        private static final int PAD_X = 13;
        private static final int PAD_Y = 8;
        private static final int SIDE = 12;
        private static final int ARC = 20;

        private final ClientModel.Msg msg;
        private final boolean mine;
        private final boolean file;
        private final int gapTop;
        private boolean showTime = true;
        private WrapText text;
        private FlatButton save;

        MessageRow(ClientModel.Msg msg, boolean grouped) {
            this.msg = msg;
            this.mine = msg.from.equals(model.username);
            this.file = "F".equals(msg.kind);
            this.gapTop = grouped ? 2 : 8;
            setLayout(null);
            if (file) {
                save = new FlatButton("Save", FlatButton.Kind.SECONDARY).size(12);
                save.addActionListener(e -> saveFile(msg));
                add(save);
            } else {
                text = new WrapText(msg.text).mode(mine ? WrapText.Mode.MINE : WrapText.Mode.OTHER);
                add(text);
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mousePressed(MouseEvent e) {
                        popup(e);
                    }

                    @Override
                    public void mouseReleased(MouseEvent e) {
                        popup(e);
                    }

                    private void popup(MouseEvent e) {
                        if (e.isPopupTrigger()) {
                            JPopupMenu menu = new JPopupMenu();
                            JMenuItem copy = new JMenuItem("Copy message");
                            copy.addActionListener(a -> UI.copyToClipboard(msg.text));
                            menu.add(copy);
                            menu.show(MessageRow.this, e.getX(), e.getY());
                        }
                    }
                });
            }
        }

        void setShowTime(boolean show) {
            this.showTime = show;
            revalidate();
            repaint();
        }

        private Font timeFont() {
            return Theme.font(11, Font.PLAIN);
        }

        private int[] geometry(int w) {
            int maxBubble = Math.max(140, (int) (w * 0.78));
            int bw;
            int bh;
            int tw = 0;
            int th = 0;
            if (file) {
                FontMetrics nf = getFontMetrics(Theme.font(14, Font.BOLD));
                FontMetrics sf = getFontMetrics(Theme.font(12, Font.PLAIN));
                bw = Math.min(maxBubble, Math.max(190, nf.stringWidth(msg.text) + 2 * PAD_X));
                bh = PAD_Y + nf.getHeight() + 2 + sf.getHeight() + 8 + save.getPreferredSize().height + PAD_Y;
            } else {
                int innerMax = maxBubble - 2 * PAD_X;
                tw = Math.min(innerMax, text.naturalWidth());
                th = text.heightFor(tw);
                bw = tw + 2 * PAD_X;
                bh = th + 2 * PAD_Y;
            }
            int bx = mine ? w - SIDE - bw : SIDE;
            return new int[]{bx, bw, bh, tw, th};
        }

        @Override
        public int heightFor(int width) {
            int[] g = geometry(width);
            return gapTop + g[2] + (showTime ? getFontMetrics(timeFont()).getHeight() + 3 : 0) + 2;
        }

        @Override
        public void doLayout() {
            int[] g = geometry(getWidth());
            if (file) {
                Dimension d = save.getPreferredSize();
                save.setBounds(g[0] + PAD_X, gapTop + g[2] - PAD_Y - d.height, d.width, d.height);
            } else {
                text.setBounds(g[0] + PAD_X, gapTop + PAD_Y, g[3], g[4]);
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            int[] geo = geometry(getWidth());
            g.setColor(mine ? Theme.bubbleMine : Theme.bubbleOther);
            g.fillRoundRect(geo[0], gapTop, geo[1], geo[2], ARC, ARC);
            if (file) {
                FontMetrics nf = g.getFontMetrics(Theme.font(14, Font.BOLD));
                FontMetrics sf = g.getFontMetrics(Theme.font(12, Font.PLAIN));
                Color fg = mine ? Theme.bubbleMineText : Theme.bubbleOtherText;
                g.setColor(fg);
                g.setFont(Theme.font(14, Font.BOLD));
                g.drawString(UI.ellipsize(msg.text, nf, geo[1] - 2 * PAD_X), geo[0] + PAD_X, gapTop + PAD_Y + nf.getAscent());
                g.setFont(Theme.font(12, Font.PLAIN));
                g.drawString(UI.size(msg.size), geo[0] + PAD_X, gapTop + PAD_Y + nf.getHeight() + 2 + sf.getAscent());
            }
            if (showTime) {
                g.setFont(timeFont());
                g.setColor(Theme.muted);
                FontMetrics tf = g.getFontMetrics();
                String t = UI.time(msg.time);
                int x = mine ? geo[0] + geo[1] - tf.stringWidth(t) - 2 : geo[0] + 2;
                g.drawString(t, x, gapTop + geo[2] + 3 + tf.getAscent());
            }
            g.dispose();
        }
    }
}
