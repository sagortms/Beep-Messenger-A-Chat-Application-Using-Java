package clientend;

import javax.swing.*;
import java.awt.BorderLayout;

final class HelpPage extends JPanel {
    private static final String[][] FAQ = {
            {"How do I start chatting with someone?",
                    "Open the People tab and search for the person by name, username, phone number or email. Press Beep Request. "
                            + "When they accept, they become a beeper and the chat appears in your Chats tab."},
            {"Where are the requests people sent me?",
                    "Menu > Beep Requests. A red number on the Menu tab shows how many are waiting. Accept to start chatting, or Decline."},
            {"How do I send a file?",
                    "In a chat, press the + button next to the message box, or drag a file into the chat window. Files up to 25 MB are supported. "
                            + "The other person presses Save on the file message to download it."},
            {"How do I copy text?",
                    "Select text in a message with the mouse and press Ctrl+C (Cmd+C on a Mac), or right-click a message and choose Copy message. "
                            + "Paste into the message box with Ctrl+V (Cmd+V)."},
            {"How do I send a message?",
                    "Press Enter. Press Shift+Enter for a new line. If you prefer Enter to make a new line, turn off \"Press Enter to send\" in Settings."},
            {"Who can see my posts?",
                    "Public: everyone on Beep. Beepers: only people connected with you. Private: only you. You choose the audience each time you post."},
            {"How do I use dark mode or bigger text?",
                    "Menu > Settings > Appearance. Both changes apply immediately and are remembered."},
            {"How do I remove a beeper?",
                    "Right-click their name in Chats or People, or open the chat and use the menu button at the top right."},
            {"I cannot log in or connect.",
                    "Make sure the server (ServerSide) is running and that the server address matches. The default is 127.0.0.1:8080; "
                            + "you can change it from the login screen or in Settings."},
    };

    HelpPage(AppHost host) {
        super(new BorderLayout());
        Theme.style(this, "panel");
        add(new PageHeader("Help", host::back), BorderLayout.NORTH);
        FlexList list = new FlexList().maxWidth(560);
        for (String[] qa : FAQ) {
            list.add(new TextRow(qa[0], qa[1], 18, 16, 4).bodyMode(WrapText.Mode.MUTED));
        }
        list.add(UI.spacer(16));
        add(UI.scroll(list), BorderLayout.CENTER);
    }
}
