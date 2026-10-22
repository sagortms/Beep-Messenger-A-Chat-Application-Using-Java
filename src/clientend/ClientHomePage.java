package clientend;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.Deque;

// main screen: 4 tabs with the bar at the bottom, other pages open on top of it
final class ClientHomePage extends JPanel implements AppHost, ClientModel.Listener {
    private static final String[] TAB_KEYS = {"chats", "people", "feed", "menu"};

    private final ClientModel model;
    private final AppHost outer;
    private final CardLayout rootCards = new CardLayout();
    private final CardLayout tabCards = new CardLayout();
    private final JPanel tabContent = new JPanel(tabCards);
    private final CardLayout subCards = new CardLayout();
    private final JPanel subPanel = new JPanel(subCards);
    private final BottomNav nav;
    private final ChatsPage chats;
    private final PeoplePage people;
    private final FeedPage feed;
    private final MenuPage menu;
    private final Deque<JComponent> stack = new ArrayDeque<>();
    private int counter;

    ClientHomePage(ClientModel model, AppHost outer) {
        super();
        setLayout(rootCards);
        this.model = model;
        this.outer = outer;
        Theme.style(this, "panel");

        chats = new ChatsPage(model, this);
        people = new PeoplePage(model, this);
        feed = new FeedPage(model, this);
        menu = new MenuPage(model, this);
        Theme.style(tabContent, "panel");
        tabContent.add(chats, TAB_KEYS[0]);
        tabContent.add(people, TAB_KEYS[1]);
        tabContent.add(feed, TAB_KEYS[2]);
        tabContent.add(menu, TAB_KEYS[3]);

        nav = new BottomNav(new String[]{"Chats", "People", "Feed", "Menu"}, i -> tabCards.show(tabContent, TAB_KEYS[i]));
        JPanel tabs = new JPanel(new BorderLayout());
        Theme.style(tabs, "panel");
        tabs.add(tabContent, BorderLayout.CENTER);
        tabs.add(nav, BorderLayout.SOUTH);
        Theme.style(subPanel, "panel");

        add(tabs, "tabs");
        add(subPanel, "sub");
        rootCards.show(this, "tabs");
        updateBadges();
        model.addListener(this);
    }

    private void updateBadges() {
        nav.setBadge(0, model.unreadTotal());
        nav.setBadge(3, model.requestsIn.size());
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "chats":
            case "requests":
                updateBadges();
                break;
            case "message": {
                ClientModel.Msg m = (ClientModel.Msg) data;
                if (!m.from.equals(model.username) && !m.from.equals(model.openChat)) {
                    toast(model.displayName(m.from) + ": " + UI.preview(m.kind, m.text));
                    if (Settings.sound) {
                        Toolkit.getDefaultToolkit().beep();
                    }
                }
                break;
            }
            case "notice":
            case "error":
                toast((String) data);
                break;
            case "friend_removed":
                updateBadges();
                break;
            default:
                break;
        }
    }

    @Override
    public void toast(String message) {
        outer.toast(message);
    }

    @Override
    public void logout() {
        outer.logout();
    }

    @Override
    public void restyleAll() {
        outer.restyleAll();
    }

    @Override
    public Frame frame() {
        return outer.frame();
    }

    @Override
    public void open(JComponent page) {
        String name = "page" + (counter++);
        page.putClientProperty("cardName", name);
        subPanel.add(page, name);
        subCards.show(subPanel, name);
        stack.push(page);
        rootCards.show(this, "sub");
        revalidate();
    }

    @Override
    public void back() {
        if (stack.isEmpty()) {
            return;
        }
        JComponent top = stack.pop();
        subPanel.remove(top);
        if (top instanceof Disposable) {
            ((Disposable) top).dispose();
        }
        if (stack.isEmpty()) {
            rootCards.show(this, "tabs");
        } else {
            subCards.show(subPanel, (String) stack.peek().getClientProperty("cardName"));
        }
        revalidate();
        repaint();
    }

    @Override
    public void openChat(String peer) {
        JComponent top = stack.peek();
        if (top instanceof ClientChatPage) {
            if (((ClientChatPage) top).peer().equals(peer)) {
                return;
            }
            back();
        }
        open(new ClientChatPage(model, this, peer));
    }

    void dispose() {
        model.removeListener(this);
        while (!stack.isEmpty()) {
            back();
        }
        chats.dispose();
        people.dispose();
        feed.dispose();
        menu.dispose();
    }
}
