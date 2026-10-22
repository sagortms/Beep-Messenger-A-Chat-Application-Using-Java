package clientend;

import javax.swing.*;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

final class PeoplePage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final AppHost host;
    private final FlexList list = new FlexList();
    private final HintField search = new HintField("Search by name, username, phone or email");
    private final Timer debounce;
    private String searching = "";

    PeoplePage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        this.host = host;
        Theme.style(this, "panel");
        add(UI.tabHeader("People", search), BorderLayout.NORTH);
        add(UI.scroll(list), BorderLayout.CENTER);
        debounce = new Timer(300, e -> runSearch());
        debounce.setRepeats(false);
        UI.onChange(search, () -> {
            if (query().length() < 2) {
                model.searchQuery = "";
                model.searchResults.clear();
                debounce.stop();
                rebuild();
            } else {
                debounce.restart();
            }
        });
        model.addListener(this);
        rebuild();
    }

    private String query() {
        return search.getText().trim();
    }

    private void runSearch() {
        searching = query();
        if (searching.length() >= 2) {
            model.search(searching);
            rebuild();
        }
    }

    @Override
    public void on(String event, Object data) {
        switch (event) {
            case "friends":
            case "presence":
            case "search":
                rebuild();
                break;
            case "requests":
                // relations in the visible search results may have changed
                if (query().length() >= 2) {
                    model.search(query());
                }
                rebuild();
                break;
            default:
                break;
        }
    }

    private void rebuild() {
        list.removeAll();
        if (query().length() >= 2) {
            buildResults();
        } else {
            buildBeepers();
        }
        list.revalidate();
        list.repaint();
    }

    private void buildBeepers() {
        List<ClientModel.Person> people = new ArrayList<>(model.friends.values());
        if (people.isEmpty()) {
            list.add(TextRow.empty("No beepers yet",
                    "Search for a friend above using their name, username, phone number or email, then send a Beep Request."));
            return;
        }
        list.add(UI.sectionLabel("YOUR BEEPERS (" + people.size() + ")"));
        for (final ClientModel.Person p : people) {
            list.add(new ListItem(p.fullName, p.username)
                    .title(p.fullName)
                    .subtitle(p.online ? "Active now" : "@" + p.username)
                    .online(p.online)
                    .onClick(() -> host.openChat(p.username))
                    .menu(() -> UI.beeperMenu(this, model, p)));
        }
        list.add(TextRow.note("Tip: right-click a beeper to remove them.", WrapText.Mode.MUTED).padded());
    }

    private void buildResults() {
        if (!query().equals(model.searchQuery)) {
            list.add(TextRow.empty(null, "Searching..."));
            return;
        }
        list.add(UI.sectionLabel("SEARCH RESULTS"));
        if (model.searchResults.isEmpty()) {
            list.add(TextRow.empty("No users found", "Check the spelling, or try the exact phone number or email."));
            return;
        }
        for (final ClientModel.Found f : model.searchResults) {
            ListItem item = new ListItem(f.fullName, f.username).title(f.fullName);
            switch (f.relation) {
                case "friend":
                    item.subtitle("@" + f.username + " - Beeper")
                            .onClick(() -> host.openChat(f.username))
                            .action(button("Message", FlatButton.Kind.SECONDARY, () -> host.openChat(f.username)));
                    break;
                case "sent":
                    item.subtitle("@" + f.username + " - Beep Request sent")
                            .action(button("Cancel", FlatButton.Kind.SECONDARY, () -> model.cancelRequest(f.username)));
                    break;
                case "received":
                    item.subtitle("@" + f.username + " - wants to connect")
                            .action(button("Decline", FlatButton.Kind.SECONDARY, () -> model.decline(f.username)))
                            .action(button("Accept", FlatButton.Kind.PRIMARY, () -> model.accept(f.username)));
                    break;
                default:
                    item.subtitle("@" + f.username)
                            .action(button("Beep Request", FlatButton.Kind.PRIMARY, () -> model.sendRequest(f.username)));
                    break;
            }
            list.add(item);
        }
    }

    static FlatButton button(String text, FlatButton.Kind kind, Runnable r) {
        FlatButton b = new FlatButton(text, kind).size(13);
        b.addActionListener(e -> r.run());
        return b;
    }

    @Override
    public void dispose() {
        debounce.stop();
        model.removeListener(this);
    }
}
