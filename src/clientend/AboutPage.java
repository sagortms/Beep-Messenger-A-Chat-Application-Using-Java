package clientend;

import javax.swing.*;
import java.awt.BorderLayout;

final class AboutPage extends JPanel {
    AboutPage(AppHost host) {
        super(new BorderLayout());
        Theme.style(this, "panel");
        add(new PageHeader("About Beep", host::back), BorderLayout.NORTH);
        FlexList list = new FlexList().maxWidth(560);
        list.add(new TextRow("Beep Messenger",
                "A small chat application made for the Computer Networks course project. "
                        + "The desktop client is written in Java Swing and talks to a Java socket server over TCP.", 18, 20, 4)
                .bodyMode(WrapText.Mode.MUTED));
        list.add(new TextRow("What it does",
                "One-to-one chat with file sharing, Beep Requests to connect with people, and a feed with Public, Beepers and Private posts. "
                        + "Accounts, chats and posts are stored by the server in plain text files.", 18, 16, 4)
                .bodyMode(WrapText.Mode.MUTED));
        list.add(new TextRow("Privacy note",
                "Messages travel over a plain TCP connection without encryption, which is fine for a lab network but not for real private conversations.",
                18, 16, 4).bodyMode(WrapText.Mode.MUTED));
        add(UI.scroll(list), BorderLayout.CENTER);
    }
}
