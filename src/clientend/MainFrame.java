package clientend;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

final class MainFrame extends JFrame implements AppHost, ClientModel.Listener {
    private final ClientModel model = new ClientModel();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final Map<String, JComponent> screens = new HashMap<>();
    private final ToastLayer toastLayer = new ToastLayer();
    private final Timer toastTimer = new Timer(3200, e -> toastLayer.hideToast());
    private ClientHomePage home;

    MainFrame() {
        super("Beep Messenger");
        setIconImage(Logo.image(128));
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                quit();
            }
        });
        Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        setSize(Math.min(450, screen.width), Math.min(800, screen.height - 20));
        setMinimumSize(new Dimension(360, 520));
        setLocationRelativeTo(null);

        Theme.style(root, "panel");
        setContentPane(root);
        setGlassPane(toastLayer);
        toastLayer.setVisible(true);
        toastTimer.setRepeats(false);
        model.addListener(this);

        show("splash", new ClientWelcomePage());
        setVisible(true);
        Timer splash = new Timer(1100, e -> showLogin(null, null));
        splash.setRepeats(false);
        splash.start();
    }

    private void show(String name, JComponent screen) {
        JComponent old = screens.put(name, screen);
        if (old != null) {
            root.remove(old);
            if (old instanceof Disposable) {
                ((Disposable) old).dispose();
            }
        }
        root.add(screen, name);
        cards.show(root, name);
        root.revalidate();
        root.repaint();
    }

    private interface Disposable extends AppHost.Disposable {
    }

    private void disposeScreen(String name) {
        JComponent old = screens.remove(name);
        if (old != null) {
            root.remove(old);
            if (old instanceof AppHost.Disposable) {
                ((AppHost.Disposable) old).dispose();
            }
        }
    }

    private void showLogin(String message, String user) {
        disposeHome();
        disposeScreen("signup");
        show("login", new ClientLoginPage(model, this::showSignup, message, user));
    }

    private void showSignup() {
        show("signup", new ClientSignUpPage(model,
                () -> showLogin(null, null),
                user -> showLogin("Account created. Log in to continue.", user)));
    }

    private void showHome() {
        disposeScreen("login");
        disposeScreen("signup");
        home = new ClientHomePage(model, this);
        show("home", home);
    }

    private void disposeHome() {
        if (home != null) {
            home.dispose();
            JComponent old = screens.remove("home");
            if (old != null) {
                root.remove(old);
            }
            home = null;
        }
    }

    @Override
    public void on(String event, Object data) {
        if (event.equals("login_ok")) {
            showHome();
        } else if (event.equals("disconnected") && home != null) {
            showLogin("Connection to the server was lost. Please log in again.", model.username);
        }
    }

    private void quit() {
        model.logout();
        Timer t = new Timer(250, e -> System.exit(0));
        t.setRepeats(false);
        t.start();
    }

    @Override
    public void toast(String message) {
        toastLayer.showToast(message);
        toastTimer.restart();
    }

    @Override
    public void logout() {
        model.logout();
        showLogin(null, null);
    }

    @Override
    public void restyleAll() {
        Theme.apply(Settings.dark, Settings.scale());
        Theme.refresh(getContentPane());
        toastLayer.repaint();
    }

    @Override
    public void open(JComponent page) {
        if (home != null) {
            home.open(page);
        }
    }

    @Override
    public void back() {
        if (home != null) {
            home.back();
        }
    }

    @Override
    public void openChat(String peer) {
        if (home != null) {
            home.openChat(peer);
        }
    }

    @Override
    public Frame frame() {
        return this;
    }

    private static final class ToastLayer extends JComponent {
        private String message;

        void showToast(String m) {
            message = m;
            repaint();
        }

        void hideToast() {
            message = null;
            repaint();
        }

        @Override
        public boolean contains(int x, int y) {
            return false; // let clicks pass through to the page underneath
        }

        @Override
        protected void paintComponent(Graphics g0) {
            if (message == null) {
                return;
            }
            Graphics2D g = UI.aa(g0);
            g.setFont(Theme.font(13, Font.PLAIN));
            FontMetrics fm = g.getFontMetrics();
            String s = UI.ellipsize(message.replace('\n', ' '), fm, getWidth() - 80);
            int w = fm.stringWidth(s) + 32;
            int h = fm.getHeight() + 20;
            int x = (getWidth() - w) / 2;
            int y = 16;
            g.setColor(new Color(0x2B2D31));
            g.fillRoundRect(x, y, w, h, h, h);
            g.setColor(Color.WHITE);
            g.drawString(s, x + 16, y + (h - fm.getHeight()) / 2 + fm.getAscent());
            g.dispose();
        }
    }
}
