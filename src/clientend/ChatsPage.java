package clientend;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.awt.BorderLayout;

final class ChatsPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final AppHost host;
    private final FlexList list = new FlexList();
    private final HintField search = new HintField("Search chats");

    ChatsPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        this.host = host;
        Theme.style(this, "panel");
        add(UI.tabHeader("Chats", search), BorderLayout.NORTH);
        add(UI.scroll(list), BorderLayout.CENTER);
        UI.onChange(search, this::rebuild);
        model.addListener(this);
        rebuild();
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "friends":
            case "chats":
            case "presence":
            case "friend_removed":
                rebuild();
                break;
            default:
                break;
        }
    }

    private long activity(ClientModel.Person p) {
        ClientModel.Chat c = model.chats.get(p.username);
        return c != null ? c.lastTime : p.since;
    }

    private void rebuild() {
        list.removeAll();
        String q = search.getText().trim().toLowerCase();
        List<ClientModel.Person> people = new ArrayList<>(model.friends.values());
        Collections.sort(people, new Comparator<ClientModel.Person>() {
            @Override
            public int compare(ClientModel.Person a, ClientModel.Person b) {
                return Long.compare(activity(b), activity(a));
            }
        });
        int shown = 0;
        for (final ClientModel.Person p : people) {
            if (!q.isEmpty() && !p.fullName.toLowerCase().contains(q) && !p.username.contains(q)) {
                continue;
            }
            ClientModel.Chat c = model.chats.get(p.username);
            String sub;
            if (c == null) {
                sub = "You are connected. Say hi!";
            } else {
                sub = (c.lastFrom.equals(model.username) ? "You: " : "") + UI.preview(c.lastKind, c.lastText);
            }
            ListItem item = new ListItem(p.fullName, p.username)
                    .title(p.fullName)
                    .subtitle(sub)
                    .online(p.online)
                    .onClick(() -> host.openChat(p.username))
                    .menu(() -> UI.beeperMenu(this, model, p));
            if (c != null) {
                item.rightText(UI.listTime(c.lastTime)).badge(c.unread).bold(c.unread > 0);
            }
            list.add(item);
            shown++;
        }
        if (shown == 0) {
            if (!q.isEmpty()) {
                list.add(TextRow.empty("No chats found", "Nobody in your chats matches \"" + search.getText().trim() + "\"."));
            } else {
                list.add(TextRow.empty("No chats yet",
                        "Open the People tab, search for someone and send a Beep Request. Once they accept, your chat will appear here."));
            }
        }
        list.revalidate();
        list.repaint();
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }
}
