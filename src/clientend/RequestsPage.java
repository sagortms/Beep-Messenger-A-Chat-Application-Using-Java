package clientend;

import javax.swing.*;
import java.awt.BorderLayout;

final class RequestsPage extends JPanel implements AppHost.Disposable, ClientModel.Listener {
    private final ClientModel model;
    private final FlexList list = new FlexList();

    RequestsPage(ClientModel model, AppHost host) {
        super(new BorderLayout());
        this.model = model;
        Theme.style(this, "panel");
        add(new PageHeader("Beep Requests", host::back), BorderLayout.NORTH);
        add(UI.scroll(list), BorderLayout.CENTER);
        model.addListener(this);
        rebuild();
    }

    @Override
    public void on(String event, Object data) {
        if (event.equals("requests")) {
            rebuild();
        }
    }

    private void rebuild() {
        list.removeAll();
        list.add(UI.sectionLabel("RECEIVED (" + model.requestsIn.size() + ")"));
        if (model.requestsIn.isEmpty()) {
            list.add(new TextRow(null, "No one is waiting for your answer.", 16, 2, 10).bodyMode(WrapText.Mode.MUTED));
        }
        for (final ClientModel.Req r : model.requestsIn) {
            list.add(new ListItem(r.fullName, r.username)
                    .title(r.fullName)
                    .subtitle("@" + r.username + " - " + UI.listTime(r.time))
                    .action(PeoplePage.button("Decline", FlatButton.Kind.SECONDARY, () -> model.decline(r.username)))
                    .action(PeoplePage.button("Accept", FlatButton.Kind.PRIMARY, () -> model.accept(r.username))));
        }
        list.add(UI.sectionLabel("SENT (" + model.requestsOut.size() + ")"));
        if (model.requestsOut.isEmpty()) {
            list.add(new TextRow(null, "You have no pending requests. Find people in the People tab.", 16, 2, 10).bodyMode(WrapText.Mode.MUTED));
        }
        for (final ClientModel.Req r : model.requestsOut) {
            list.add(new ListItem(r.fullName, r.username)
                    .title(r.fullName)
                    .subtitle("@" + r.username + " - waiting for reply")
                    .action(PeoplePage.button("Cancel", FlatButton.Kind.SECONDARY, () -> model.cancelRequest(r.username))));
        }
        list.revalidate();
        list.repaint();
    }

    @Override
    public void dispose() {
        model.removeListener(this);
    }
}
