package clientend;

import javax.swing.*;
import java.awt.*;

interface AppHost {
    void toast(String message);

    void logout();

    void restyleAll();

    void open(JComponent page);

    void back();

    void openChat(String peer);

    Frame frame();

    interface Disposable {
        void dispose();
    }
}
