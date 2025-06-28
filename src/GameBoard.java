import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class GameBoard extends JPanel {

    public static final int BOARD_WIDTH = 1280;
    public static final int BOARD_HEIGHT = 720;

    private enum GameState { WAITING_TO_LAUNCH, GAME_ON, GAME_WIN, GAME_OVER }

    private GameState gameState;

    private Platform platform;
    private Thread gameThread;

    private final int TARGET_FPS = 60;
    private final long OPTIMAL_TIME = 1_000_000_000 / TARGET_FPS;

    private long startTime;
    private int score;

    private boolean ballLaunched = false;
    private int mouseX, mouseY;
    private float ballSpeed = 3f;
    private float ballspeedCap = 10f;
    private float ballSpeedMultiplyer = 0.01f;

    ArrayList<Brick> bricks = new ArrayList<>();
    ArrayList<Ball> balls = new ArrayList<>();
    ArrayList<Gadget> gadgets = new ArrayList<>();
    ArrayList<GadgetTime> activeGadgetTimes = new ArrayList<>();

    long speedBoostUntil;
    boolean speedBoostApplied = false;

    int level;

    private CardLayout cardLayout;
    private JPanel cardPanel;

    GameMenu gameMenu;

    public GameBoard(int level, CardLayout cardLayout, JPanel cardPanel, GameMenu gameMenu) {
        this.level = level;
        this.cardLayout = cardLayout;
        this.cardPanel = cardPanel;
        this.gameMenu = gameMenu;
        setFocusable(true);
        setPreferredSize(new Dimension(BOARD_WIDTH, BOARD_HEIGHT));
        setBackground(new Color(58, 64, 58));
        setupKeyBindings();

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                if (gameState == GameState.GAME_ON && ballLaunched && platform != null) {
                    platform.setX(mouseX - platform.getWidth() / 2);
                }
                repaint();
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameState == GameState.GAME_OVER || gameState == GameState.GAME_WIN) {
                    if (e.getKeyCode() == KeyEvent.VK_R) {
                        startGame();
                    } else if (e.getKeyCode() == KeyEvent.VK_M || e.getKeyCode() == KeyEvent.VK_L) {
                        cardLayout.show(cardPanel, "menu");
                    }
                }
            }
        });


        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gameState == GameState.WAITING_TO_LAUNCH && !ballLaunched) {
                    Ball ball = balls.getFirst();
                    float dx = e.getX() - ball.getX();
                    float dy = e.getY() - ball.getY();
                    float length = (float) Math.sqrt(dx * dx + dy * dy);
                    if (length != 0) {
                        dx /= length;
                        dy /= length;
                        ball.setDx(dx * ballSpeed);
                        ball.setDy(dy * ballSpeed);
                        ballLaunched = true;
                        gameState = GameState.GAME_ON;
                        startTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }

    private void setupKeyBindings() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke("R"), "retry");
        im.put(KeyStroke.getKeyStroke("M"), "menu");
        im.put(KeyStroke.getKeyStroke("L"), "levels");

        am.put("retry", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameState == GameState.GAME_OVER) {
                    startGame();
                }
            }
        });

        am.put("menu", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameState == GameState.GAME_OVER) {
                    cardLayout.show(cardPanel, "menu");
                }
            }
        });

        am.put("levels", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameState == GameState.GAME_WIN) {
                    gameMenu.loadLevels();
                    cardLayout.show(cardPanel, "levels");
                }
            }
        });
    }

    public void startGame() {
        initGame();
        gameState = GameState.WAITING_TO_LAUNCH;
        startGameLoop();
    }

    private void startGameLoop() {
        gameThread = new Thread(() -> {
            long lastTime = System.nanoTime();

            while (gameState != GameState.GAME_OVER && gameState != GameState.GAME_WIN) {
                long now = System.nanoTime();
                lastTime = now;

                if (gameState == GameState.GAME_ON) {
                    gameCycle();
                }

                repaint();

                long sleep = (OPTIMAL_TIME - (System.nanoTime() - lastTime)) / 1_000_000;
                if (sleep > 0) {
                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        gameThread.start();
    }

    private void initGame() {
        bricks.clear();
        balls.clear();
        activeGadgetTimes.clear();
        gadgets.clear();
        platform = new Platform(590, 600);
        balls.add(new Ball(632, 580, 0, 0));
        ballLaunched = false;
        ballSpeed = 3f;
        ballspeedCap = 10f;
        spawnBricks();
        score = 1000;
    }

    private void spawnBricks() {
        int maxRows = 3 + level;
        int maxCols = 5 + level % 4;
        int brickWidth = 128, brickHeight = 40, hGap = 10, vGap = 10;

        int totalWidth = maxCols * brickWidth + (maxCols - 1) * hGap;
        int startX = (BOARD_WIDTH - totalWidth) / 2;
        int startY = 100;


        Random rng = new Random(level); // asta face nivelul reproductibil (inspiratie seed: Minecraft)

        for (int row = 0; row < maxRows; row++) {
            for (int col = 0; col < maxCols; col++) {
                // ca unele caramizi sa nu se puna
                if (rng.nextDouble() < 0.2 + level * 0.02) continue; // mai putine gauri la nviel mare

                int x = startX + col * (brickWidth + hGap);
                int y = startY + row * (brickHeight + vGap);

                int baseHealth = 1 + level / 2;
                int health = baseHealth + rng.nextInt(1 + level / 3); // mai mult hp la nivele mari

                bricks.add(new Brick(x, y, brickWidth, brickHeight, health));
            }
        }
    }


    private void gameCycle() {
        updateBalls();
        updateGadgets();
        checkCollisions();
        updateBricks();
        checkEnd();
    }

    private void updateBalls(){
        synchronized(balls) {
            for (Ball b : balls) {
                if (b != null) b.update();
            }
            balls.removeIf(ball -> ball.getY() > 615);
            if(ballSpeed < ballspeedCap) ballSpeed += ballSpeedMultiplyer;
        }
        if (System.currentTimeMillis() > speedBoostUntil + 10000 && speedBoostApplied) {
            ballspeedCap = 10f;
            ballSpeed = 10f;
            speedBoostApplied = false;
        }
    }

    private void updateBricks() {
        synchronized (bricks){
            for (Brick b : bricks) {
                if(b.health <= 0) {
                    spawnGadget(b.x, b.y);
                }
            }
            bricks.removeIf(b->b.health <= 0);
        }
    }

    private void updateGadgets(){
        synchronized(gadgets) {
            for (Gadget g : gadgets) {
                if (g != null){
                    g.update();
                };
            }
            gadgets.removeIf(g -> g.x > 1300);
        }
    }

    private void spawnGadget(int x, int y) {
        double chance = Math.random();
        if (chance > 0.8) {
            synchronized (gadgets) {
                gadgets.add(new Gadget(x,y));
            }
        }
    }

    private void activateGadgetTime(Gadget g) {
        for (GadgetTime gt : activeGadgetTimes) {
            if (gt.getGadget().getType() == g.getType()) {
                gt.reset();
                return;
            }
        }
        activeGadgetTimes.add(new GadgetTime(g));
    }


    private void checkEnd() {
        if (balls.isEmpty()) {
            gameState = GameState.GAME_OVER;
        } else if (bricks.isEmpty()) {
            gameState = GameState.GAME_WIN;
            saveScoreAndProgress();
        }

    }

    private void saveScoreAndProgress() {
        if (Session.loggedUsername != null) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            int finalScore = Math.max(0, score - (int) elapsed);
            UserManager.saveScore(Session.loggedUsername, level, finalScore);
        }
    }

    private void checkCollisions() {
        for (Ball ball : balls) {
            Rectangle ballRect = ball.getBounds();

            if (ballRect.intersects(platform.getBounds())) {
                ball.setDy(-Math.abs(ball.getDy()));
                int ballCenterX = (int) ball.getX() + ball.getWidth() / 2;
                float relativeIntersect = (float) (ballCenterX - platform.getX()) / platform.getWidth() - 0.5f;
                ball.setDx(relativeIntersect * 10f);
                normalizeSpeed(ball, ballSpeed);
            }

            for (Brick b : bricks) {
                if (b != null && ballRect.intersects(b.getBounds())) {
                    b.health--;

                    Rectangle brickRect = b.getBounds();
                    int ballLeft = (int) ball.getX(), ballRight = ballLeft + ball.getWidth();
                    int ballTop = (int) ball.getY(), ballBottom = ballTop + ball.getHeight();
                    int brickLeft = brickRect.x, brickRight = brickRect.x + brickRect.width;
                    int brickTop = brickRect.y, brickBottom = brickRect.y + brickRect.height;

                    double overlapX = Math.min(ballRight - brickLeft, brickRight - ballLeft);
                    double overlapY = Math.min(ballBottom - brickTop, brickBottom - ballTop);

                    if (overlapX < overlapY) ball.setDx(-ball.getDx());
                    else ball.setDy(-ball.getDy());

                    normalizeSpeed(ball, ballSpeed);
                    break;
                }
            }
        }
        ArrayList<Gadget> gadgetsToRemove = new ArrayList<>();
        synchronized(gadgets) {
            for (Gadget g : gadgets) {
                if(platform.getBounds().intersects(g.getBounds())) {
                    applyGadget(g);
                    gadgetsToRemove.add(g);
                }
            }
            for(Gadget g : gadgetsToRemove) {
                gadgets.remove(g);
            }
        }
    }

    private void applyGadget(Gadget g) {
        synchronized(gadgets) {
            activateGadgetTime(g);
            switch (g.getType()){
                case 1 -> platform.extend(10);
                case 2 -> gadgetSpeed();
                case 3 -> gadgetBalls();
            }
        }
    }


    private void gadgetBalls() {
        ArrayList<Ball> ballsToCopy = (ArrayList<Ball>) balls.clone();
        for(Ball ball : ballsToCopy){
            balls.add(new Ball((int)ball.getX(), (int)ball.getY(), ball.getDx()*(-1), ball.getDy()*(-1)));
        }
    }

    private void gadgetSpeed() {
        speedBoostUntil = System.currentTimeMillis();
        ballspeedCap = 20f;
        ballSpeed = 20f;
        speedBoostApplied = true;
    }

    private void normalizeSpeed(Ball ball, float speed) {
        float dx = ball.getDx();
        float dy = ball.getDy();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len == 0) return;
        ball.setDx(dx / len * speed);
        ball.setDy(dy / len * speed);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        synchronized (balls){
            for (Ball ball : balls) if (ball != null) ball.draw(g2d);
        }
        if (platform != null) platform.draw(g2d);
        synchronized (bricks) {
            for (Brick b : bricks) if (b != null && b.isVisible) b.draw(g2d);
        }

        synchronized (gadgets) {
            for(Gadget gadget : gadgets) gadget.draw(g2d);
        }

        synchronized(activeGadgetTimes) {
            activeGadgetTimes.removeIf(GadgetTime::isExpired);
            int gtIndex = 0;
            for (GadgetTime gt : activeGadgetTimes) {
                if (!gt.isExpired()) {
                    gt.draw(g2d, gtIndex);
                    gtIndex++;
                }
            }
        }

        if (gameState == GameState.WAITING_TO_LAUNCH && !ballLaunched) {
            Ball b = balls.getFirst();
            int cx = (int) b.getX() + b.getWidth() / 2;
            int cy = (int) b.getY() + b.getHeight() / 2;

            float dx = mouseX - cx;
            float dy = mouseY - cy;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len != 0) {
                dx /= len;
                dy /= len;

                int arrowLength = 100;
                int ex = (int) (cx + dx * arrowLength);
                int ey = (int) (cy + dy * arrowLength);

                g2d.setColor(Color.WHITE);
                g2d.drawLine(cx, cy, ex, ey);

                int size = 10;
                int ax1 = (int) (ex + (-dy) * size - dx * size);
                int ay1 = (int) (ey + (dx) * size - dy * size);
                int ax2 = (int) (ex - (-dy) * size - dx * size);
                int ay2 = (int) (ey - (dx) * size - dy * size);

                Polygon arrowHead = new Polygon();
                arrowHead.addPoint(ex, ey);
                arrowHead.addPoint(ax1, ay1);
                arrowHead.addPoint(ax2, ay2);

                g2d.fill(arrowHead);
            }
        }

        if (gameState == GameState.GAME_ON || gameState == GameState.GAME_WIN) {
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            int timePenalty = (int) elapsed;
            int displayScore = Math.max(0, score - timePenalty);
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            g2d.drawString("Timp: " + elapsed + "s", BOARD_WIDTH - 150, 30);
            g2d.drawString("Scor: " + displayScore, BOARD_WIDTH - 150, 55);
        }

        if (gameState == GameState.GAME_OVER) {
            g2d.setColor(Color.RED);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("Game Over", BOARD_WIDTH / 2 - 100, BOARD_HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            g2d.drawString("Retry (R) / Menu (M)", BOARD_WIDTH / 2 - 90, BOARD_HEIGHT / 2 + 40);
        } else if (gameState == GameState.GAME_WIN) {
            g2d.setColor(Color.GREEN);
            g2d.setFont(new Font("Arial", Font.BOLD, 36));
            g2d.drawString("You Win!", BOARD_WIDTH / 2 - 80, BOARD_HEIGHT / 2);
            g2d.setFont(new Font("Arial", Font.PLAIN, 24));
            long elapsed = (System.currentTimeMillis() - startTime) / 1000;
            int timePenalty = (int) elapsed;
            int displayScore = Math.max(0, score - timePenalty);
            g2d.drawString("Score: " + displayScore, BOARD_WIDTH / 2 - 50, BOARD_HEIGHT / 2 + 40);
            g2d.drawString("Press L for Levels", BOARD_WIDTH / 2 - 70, BOARD_HEIGHT / 2 + 70);
        }

    }
}
