package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

final class FeedPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private static final String[] AUDIENCE_KEYS = {"public", "beepers", "private"};
    private static final String[] AUDIENCE_NAMES = {"Public", "Beepers", "Private"};

    private final ClientModel model;
    private final FlexList list = new FlexList();
    private final Composer composer;
    private int filter;

    FeedPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        Theme.style(this, "panel");
        Segmented filterBar = new Segmented(new String[]{"All", "Public", "Beepers", "Private"}, 0, i -> {
            filter = i;
            rebuild();
        });
        add(UI.tabHeader("Feed", filterBar), BorderLayout.NORTH);
        composer = new Composer();
        list.add(composer);
        add(UI.scroll(list), BorderLayout.CENTER);
        model.addListener(this);
        rebuild();
    }

    @Override
    public void on(String event, Object data) {
        if (event.equals("feed") || event.equals("profile_ok")) {
            rebuild();
        }
    }

    private boolean passes(ClientModel.Post p) {
        return filter == 0 || p.vis.equals(AUDIENCE_KEYS[filter - 1]);
    }

    private void rebuild() {
        for (int i = list.getComponentCount() - 1; i >= 1; i--) {
            list.remove(i);
        }
        composer.refreshHint();
        int shown = 0;
        for (ClientModel.Post p : model.posts) {
            if (passes(p)) {
                list.add(new PostCard(p));
                shown++;
            }
        }
        if (shown == 0) {
            list.add(TextRow.empty("Nothing here yet", filter == 0
                    ? "Posts from you and your beepers will show up here."
                    : "No " + AUDIENCE_NAMES[filter - 1].toLowerCase() + " posts to show."));
        }
        list.revalidate();
        list.repaint();
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }

    static String audienceName(String vis) {
        for (int i = 0; i < AUDIENCE_KEYS.length; i++) {
            if (AUDIENCE_KEYS[i].equals(vis)) {
                return AUDIENCE_NAMES[i];
            }
        }
        return vis;
    }

    private final class Composer extends JComponent implements FlexList.WidthAware {
        private static final int PAD = 14;
        private static final int AVATAR = 40;
        private final HintArea area = new HintArea("What's on your mind?");
        private final JScrollPane areaScroll = UI.transparentScroll(area);
        private final Segmented audience;
        private final FlatButton post = new FlatButton("Post", FlatButton.Kind.PRIMARY);
        private int audienceIndex;

        Composer() {
            setLayout(null);
            audience = new Segmented(AUDIENCE_NAMES, 0, i -> audienceIndex = i).size(12);
            post.addActionListener(e -> submit());
            area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "post");
            area.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.META_DOWN_MASK), "post");
            area.getActionMap().put("post", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    submit();
                }
            });
            add(areaScroll);
            add(audience);
            add(post);
        }

        void refreshHint() {
            repaint();
        }

        private void submit() {
            String text = area.getText().trim();
            if (text.isEmpty()) {
                return;
            }
            if (text.length() > common.Packet.MAX_POST_CHARS) {
                JOptionPane.showMessageDialog(this, "Posts can be at most " + common.Packet.MAX_POST_CHARS + " characters.");
                return;
            }
            model.createPost(AUDIENCE_KEYS[audienceIndex], text);
            area.setText("");
        }

        private int boxHeight() {
            return getFontMetrics(Theme.font(14, Font.PLAIN)).getHeight() * 3 + 22;
        }

        @Override
        public int heightFor(int width) {
            return PAD + boxHeight() + 10 + post.getPreferredSize().height + PAD + 8;
        }

        @Override
        public void doLayout() {
            int boxX = PAD + AVATAR + 10;
            int boxW = getWidth() - boxX - PAD;
            areaScroll.setBounds(boxX + 12, PAD + 10, boxW - 24, boxHeight() - 20);
            int y = PAD + boxHeight() + 10;
            Dimension pd = post.getPreferredSize();
            post.setBounds(getWidth() - PAD - pd.width, y, pd.width, pd.height);
            audience.setBounds(PAD, y, getWidth() - 2 * PAD - pd.width - 10, pd.height);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            int w = getWidth();
            UI.avatar(g, model.fullName, model.username, PAD, PAD, AVATAR);
            int boxX = PAD + AVATAR + 10;
            g.setColor(Theme.surface);
            g.fillRoundRect(boxX, PAD, w - boxX - PAD, boxHeight(), 18, 18);
            g.fillRect(0, getHeight() - 8, w, 8);
            g.dispose();
        }
    }

    private final class PostCard extends JComponent implements FlexList.WidthAware {
        private static final int PAD = 14;
        private static final int AVATAR = 40;
        private final ClientModel.Post post;
        private final WrapText body;
        private final FlatButton delete;

        PostCard(ClientModel.Post post) {
            this.post = post;
            setLayout(null);
            body = new WrapText(post.text).font(15, Font.PLAIN);
            add(body);
            if (post.author.equals(model.username)) {
                delete = new FlatButton("Delete", FlatButton.Kind.DANGER).size(12);
                delete.addActionListener(e -> {
                    if (UI.confirm(this, "Delete this post?", "Delete post")) {
                        model.deletePost(post.id);
                    }
                });
                add(delete);
            } else {
                delete = null;
            }
        }

        private int headerHeight() {
            return Math.max(AVATAR, getFontMetrics(Theme.font(14, Font.BOLD)).getHeight() + getFontMetrics(Theme.font(12, Font.PLAIN)).getHeight() + 2);
        }

        @Override
        public int heightFor(int width) {
            return PAD + headerHeight() + 10 + body.heightFor(width - 2 * PAD) + PAD + 8;
        }

        @Override
        public void doLayout() {
            int w = getWidth() - 2 * PAD;
            int y = PAD + headerHeight() + 10;
            body.setBounds(PAD, y, w, body.heightFor(w));
            if (delete != null) {
                Dimension d = delete.getPreferredSize();
                delete.setBounds(getWidth() - PAD - d.width, PAD, d.width, d.height);
            }
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = UI.aa(g0);
            int w = getWidth();
            UI.avatar(g, post.authorName, post.author, PAD, PAD, AVATAR);
            Font nameFont = Theme.font(14, Font.BOLD);
            Font metaFont = Theme.font(12, Font.PLAIN);
            FontMetrics nf = g.getFontMetrics(nameFont);
            FontMetrics mf = g.getFontMetrics(metaFont);
            int tx = PAD + AVATAR + 10;
            int top = PAD + (headerHeight() - nf.getHeight() - mf.getHeight() - 2) / 2;
            int reserve = delete != null ? delete.getPreferredSize().width + 8 : 0;
            g.setFont(nameFont);
            g.setColor(Theme.text);
            g.drawString(UI.ellipsize(post.authorName, nf, w - tx - PAD - reserve), tx, top + nf.getAscent());
            g.setFont(metaFont);
            g.setColor(Theme.muted);
            g.drawString(UI.ellipsize(UI.postTime(post.time) + "  -  " + audienceName(post.vis), mf, w - tx - PAD - reserve),
                    tx, top + nf.getHeight() + 2 + mf.getAscent());
            g.setColor(Theme.surface);
            g.fillRect(0, getHeight() - 8, w, 8);
            g.dispose();
        }
    }
}
