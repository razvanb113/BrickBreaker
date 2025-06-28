import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameMenu extends JFrame {



    private CardLayout cardLayout;
    private JPanel cardPanel;
    private GameBoard gameBoard;
    private JPanel levelsPanel;


    public GameMenu() {
        super("Brick Breaker");
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1280, 720));
        setResizable(false);
        setFocusable(true);
        pack();

        BufferedImage icon;
        try {
            icon = ImageIO.read(new File("./sprites/icon.png"));
            setIconImage(icon);
        } catch (IOException e) {
            e.printStackTrace();
        }

        JPanel menuPanel = createMenuPanel();
        JPanel settingsPanel = createSettingsPanel();
        UserManager.init();
        levelsPanel = createLevelSelectorPanel();
        JPanel accountPanel = createAccountPanel();

        cardPanel.add(menuPanel, "menu");
        cardPanel.add(settingsPanel, "settings");
        cardPanel.add(accountPanel, "account");
        cardPanel.add(levelsPanel, "levels");

        add(cardPanel);
        setVisible(true);
    }

    private JPanel createAccountPanel() {
        JPanel accountPanel = new JPanel(new BorderLayout());
        accountPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));
        accountPanel.setBackground(new Color(245, 245, 250));

        Font font = new Font("Segoe UI", Font.PLAIN, 16);
        Font titleFont = new Font("Segoe UI", Font.BOLD, 20);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);

        Butoane backButton = new Butoane("← Înapoi");
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setHorizontalAlignment(SwingConstants.LEFT);
        topPanel.add(backButton, BorderLayout.WEST);

        accountPanel.add(topPanel, BorderLayout.NORTH);

        JLabel statusLabel = new JLabel("Nu ești autentificat", JLabel.CENTER);
        statusLabel.setFont(titleFont);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 30, 0));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField userField = new JTextField(20);
        JTextField emailField = new JTextField(20);
        JPasswordField passField = new JPasswordField(20);

        userField.setFont(font);
        emailField.setFont(font);
        passField.setFont(font);

        JLabel userLabel = new JLabel("Username:", JLabel.RIGHT);
        JLabel emailLabel = new JLabel("Email:", JLabel.RIGHT);
        JLabel passLabel = new JLabel("Parolă:", JLabel.RIGHT);

        userLabel.setFont(font);
        emailLabel.setFont(font);
        passLabel.setFont(font);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setMaximumSize(new Dimension(500, 180));
        formPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        formPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(userLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(userField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        formPanel.add(emailLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(emailField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        formPanel.add(passLabel, gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        formPanel.add(passField, gbc);

        Butoane loginButton = new Butoane("Autentificare");
        Butoane registerButton = new Butoane("Creează cont");
        Butoane logoutButton = new Butoane("Logout");
        Butoane toggleButton = new Butoane("Nu ai cont? Creează unul");
        toggleButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerButton.setVisible(false);
        logoutButton.setVisible(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(logoutButton);
        buttonPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);

        centerPanel.add(statusLabel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(formPanel);
        centerPanel.add(Box.createVerticalStrut(20));
        centerPanel.add(buttonPanel);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(toggleButton);

        centerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        accountPanel.add(centerPanel, BorderLayout.CENTER);

        toggleButton.addActionListener(e -> {
            boolean inRegisterMode = registerButton.isVisible();
            registerButton.setVisible(!inRegisterMode);
            loginButton.setVisible(inRegisterMode);
            toggleButton.setText(inRegisterMode ? "Nu ai cont? Creează unul" : "Ai deja cont? Autentifică-te");
            statusLabel.setText(inRegisterMode ? "Nu ești autentificat" : "Creează un cont nou");
        });

        if (Session.loggedUsername != null) {
            statusLabel.setText(UserManager.getAccountSummary(Session.loggedUsername));
            registerButton.setVisible(false);
            loginButton.setVisible(false);
            logoutButton.setVisible(true);
            toggleButton.setVisible(false);
            formPanel.setVisible(false);
            loadLevels();
        }

        loginButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Completează toate câmpurile.");
                return;
            }

            if (!isValidEmail(email)) {
                statusLabel.setText("Email invalid.");
                return;
            }

            String result = UserManager.login(username, password);
            if (result.equals("Autentificat cu succes.")) {
                Session.loggedUsername = username;
                statusLabel.setText(UserManager.getAccountSummary(username));
                registerButton.setVisible(false);
                loginButton.setVisible(false);
                logoutButton.setVisible(true);
                toggleButton.setVisible(false);
                formPanel.setVisible(false);
                loadLevels();
            } else {
                statusLabel.setText(result);
            }
        });

        registerButton.addActionListener(e -> {
            String username = userField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                statusLabel.setText("Completează toate câmpurile.");
                return;
            }

            if (!isValidEmail(email)) {
                statusLabel.setText("Email invalid.");
                return;
            }

            String result = UserManager.register(username, email, password);
            if (result.equals("Cont creat cu succes.")) {
                Session.loggedUsername = username;
                statusLabel.setText("Bun venit, " + username + "!");
                toggleButton.setVisible(false);
                loginButton.setVisible(false);
                registerButton.setVisible(false);
                formPanel.setVisible(false);

                Timer t = new Timer(1200, ev -> cardLayout.show(cardPanel, "menu"));
                t.setRepeats(false);
                t.start();
            } else {
                statusLabel.setText(result);
            }
        });

        logoutButton.addActionListener(e -> {
            UserManager.logout();
            updateUIAfterLogout();
        });

        backButton.addActionListener(e -> cardLayout.show(cardPanel, "menu"));

        return accountPanel;
    }


    private void updateUIAfterLogout() {
        Session.loggedUsername = null;

        cardPanel.remove(2);
        JPanel newAccountPanel = createAccountPanel();
        cardPanel.add(newAccountPanel, "account");

        loadLevels();

        cardLayout.show(cardPanel, "menu");
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$";
        return email.matches(emailRegex);
    }

    private JPanel createMenuPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());


        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton accountButton = new Butoane("Cont");
        accountButton.setFocusPainted(false);
        accountButton.addActionListener(e -> {
            cardPanel.remove(2);

            JPanel newAccountPanel = createAccountPanel();
            cardPanel.add(newAccountPanel, "account");

            cardLayout.show(cardPanel, "account");
        });

        topBar.add(accountButton);
        topBar.setOpaque(false);

        JPanel centerPanel = new JPanel(new GridLayout(6, 3, 10, 10));

        centerPanel.add(new JLabel());  // col 0 - gol
        JLabel title = new JLabel("BRICK BREAKER", JLabel.CENTER);
        title.setFont(new Font("Century Gothic", Font.BOLD, 24));
        centerPanel.add(title);
        centerPanel.add(new JLabel());

        centerPanel.add(new JLabel());
        centerPanel.add(new JLabel());
        centerPanel.add(new JLabel());

        centerPanel.add(new JLabel());
        JButton startButton = new Butoane("Start Joc");
        startButton.addActionListener(e -> {
            cardLayout.show(cardPanel, "levels");
            //cardLayout.show(cardPanel, "game");
            //gameBoard.startGame();
        });
        centerPanel.add(startButton);
        centerPanel.add(new JLabel());

        centerPanel.add(new JLabel());
        JButton settingsButton = new Butoane("Setari");
        settingsButton.addActionListener(e -> cardLayout.show(cardPanel, "settings"));
        centerPanel.add(settingsButton);
        centerPanel.add(new JLabel());

        centerPanel.add(new JLabel());
        JButton exitButton = new Butoane("Iesire");
        exitButton.addActionListener(e -> System.exit(0));
        centerPanel.add(exitButton);
        centerPanel.add(new JLabel());

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        return mainPanel;
    }

    private JPanel createLevelSelectorPanel() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setOpaque(false);
        Butoane backButton = new Butoane("← Înapoi");
        backButton.setFocusPainted(false);
        backButton.setBorderPainted(false);
        backButton.setContentAreaFilled(false);
        backButton.setHorizontalAlignment(SwingConstants.LEFT);
        topPanel.add(backButton, BorderLayout.WEST);
        mainPanel.add(topPanel, BorderLayout.NORTH);

        backButton.addActionListener(e -> cardLayout.show(cardPanel, "menu"));

        JPanel levelsGrid = new JPanel(new GridLayout(2, 5, 10, 10));
        levelsGrid.setBackground(Color.WHITE);

        int maxAccessibleLevel = 1;
        if (Session.loggedUsername != null) {
            maxAccessibleLevel = UserManager.getLastLevel(Session.loggedUsername) + 1;
        }

        Map<Integer, Integer> scoreMap = new HashMap<>();
        if (Session.loggedUsername != null) {
            scoreMap = UserManager.getScoresByLevel(Session.loggedUsername);
        }

        for (int i = 1; i <= 10; i++) {
            final int level = i;
            boolean isAccessible = (i <= maxAccessibleLevel);

            Butoane levelButton = new Butoane("Level " + i);
            levelButton.setEnabled(isAccessible);

            if (!isAccessible) {
                levelButton.setForeground(Color.GRAY);
                levelButton.setBackground(new Color(220, 220, 220));
            }

            levelButton.addActionListener(e -> {
                if (!isAccessible) return;
                GameBoard gameBoard = new GameBoard(level, cardLayout, cardPanel, this);
                cardPanel.add(gameBoard, "game");
                cardLayout.show(cardPanel, "game");
                gameBoard.startGame();
            });

            int score = scoreMap.getOrDefault(i, 0);
            JLabel scoreLabel = new JLabel("Scor: " + score, SwingConstants.CENTER);
            scoreLabel.setFont(new Font("Arial", Font.PLAIN, 12));

            JPanel levelContainer = new JPanel(new BorderLayout());
            levelContainer.setBackground(Color.WHITE);
            levelContainer.add(levelButton, BorderLayout.CENTER);
            levelContainer.add(scoreLabel, BorderLayout.SOUTH);

            levelsGrid.add(levelContainer);
        }

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Color.WHITE);

        centerPanel.add(Box.createVerticalStrut(20));

        centerPanel.add(levelsGrid);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        return mainPanel;
    }



    public void loadLevels() {
        cardPanel.remove(levelsPanel);

        levelsPanel = createLevelSelectorPanel();

        cardPanel.add(levelsPanel, "levels");

        cardPanel.revalidate();
        cardPanel.repaint();
    }



    private JPanel createSettingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("Setări Joc", JLabel.CENTER);
        label.setFont(new Font("Arial", Font.PLAIN, 20));
        panel.add(label, BorderLayout.CENTER);

        JButton backButton = new JButton("Înapoi la Meniu");
        backButton.addActionListener(e -> cardLayout.show(cardPanel, "menu"));
        panel.add(backButton, BorderLayout.SOUTH);

        return panel;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(GameMenu::new);
    }
}
