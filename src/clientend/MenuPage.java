package clientend;

import javax.swing.*;
import java.awt.BorderLayout;

final class MenuPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final AppHost host;
    private final FlexList list = new FlexList();

    MenuPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        this.host = host;
        Theme.style(this, "panel");
        add(UI.tabHeader("Menu", null), BorderLayout.NORTH);
        add(UI.scroll(list), BorderLayout.CENTER);
        model.addListener(this);
        rebuild();
    }

    @Override
    public void on(String event, Object data) {
        if (event.equals("requests") || event.equals("profile_ok") || event.equals("login_ok")) {
            rebuild();
        }
    }

    private void rebuild() {
        list.removeAll();
        list.add(new ListItem(model.fullName, model.username)
                .title(model.fullName)
                .subtitle("@" + model.username + " - View your account")
                .onClick(() -> host.open(new AccountPage(model, host))));
        list.add(UI.spacer(6));
        list.add(new MenuRow("Account", false, () -> host.open(new AccountPage(model, host))));
        list.add(new MenuRow("Settings", false, () -> host.open(new SettingsPage(model, host))));
        list.add(new MenuRow("Beep Requests", false, () -> host.open(new RequestsPage(model, host)))
                .badge(model.requestsIn.size()));
        list.add(new MenuRow("Help", false, () -> host.open(new HelpPage(host))));
        list.add(new MenuRow("About Beep", false, () -> host.open(new AboutPage(host))));
        list.add(UI.spacer(10));
        list.add(new MenuRow("Log out", true, () -> {
            if (UI.confirm(this, "Log out of Beep Messenger?", "Log out")) {
                host.logout();
            }
        }));
        list.revalidate();
        list.repaint();
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }
}
