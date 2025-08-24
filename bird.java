import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class FlappyBird extends JPanel implements ActionListener, KeyListener {

    // ---------- Game constants ----------
    private static final int WIDTH = 420;
    private static final int HEIGHT = 640;

    private static final int GROUND_HEIGHT = 100;
    private static final int BIRD_SIZE = 24;
    private static final int PIPE_WIDTH = 60;
    private static final int PIPE_GAP = 170;           // vertical gap between pipes
    private static final int PIPE_SPACING = 220;       // distance between pipe pairs
    private static final int PIPE_SPEED = 3;

    private static final double GRAVITY = 0.55;
    private static final double JUMP_VELOCITY = -8.5;

    // ---------- Game state ----------
    private Timer timer;
    private boolean running = false;
    private boolean gameOver = false;
    private boolean paused = false;

    private double birdX, birdY;
    private double velY;

    private static class PipePair {
        int x;          // left of the pipes
        int gapY;       // top of gap (where opening starts)
        boolean scored; // has the bird passed this pair?

        PipePair(int x, int gapY) {
            this.x = x;
            this.gapY = gapY;
            this.scored = false;
        }
    }

    private final ArrayList<PipePair> pipes = new ArrayList<>();
    private final Random rng = new Random();
    private int score = 0;
    private int bestScore = 0;

    // ---------- Setup ----------
    public FlappyBird() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(135, 206, 235)); // sky blue
        setFocusable(true);
        addKeyListener(this);

        timer = new Timer(16, this); // ~60 FPS
        resetGame();
        timer.start();
    }

    private void resetGame() {
        running = true;
        gameOver = false;
        paused = false;
        score = 0;
        velY = 0;
        birdX = WIDTH * 0.28;
        birdY = HEIGHT / 2.0 - BIRD_SIZE / 2.0;
        pipes.clear();

        // spawn initial pipes filling the screen to the right
        int startX = WIDTH + 100;
        for (int i = 0; i < 4; i++) {
            spawnPipe(startX + i * PIPE_SPACING);
        }
    }

    private void spawnPipe(int x) {
        // Gap vertical position with some margin from top and above ground
        int minTop = 60;
        int maxTop = HEIGHT - GROUND_HEIGHT - PIPE_GAP - 60;
        int gapY = minTop + rng.nextInt(Math.max(1, maxTop - minTop));
        pipes.add(new PipePair(x, gapY));
    }

    // ---------- Update loop ----------
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running || paused) {
            repaint();
            return;
        }

        // Apply gravity
        velY += GRAVITY;
        birdY += velY;

        // Move pipes
        for (PipePair p : pipes) p.x -= PIPE_SPEED;

        // Recycle pipes that go off-screen and spawn new ones
        if (!pipes.isEmpty() && pipes.get(0).x + PIPE_WIDTH < 0) {
            pipes.remove(0);
            int lastX = pipes.get(pipes.size() - 1).x;
            spawnPipe(lastX + PIPE_SPACING);
        }

        // Scoring: when bird passes a pipe's center line
        for (PipePair p : pipes) {
            if (!p.scored && p.x + PIPE_WIDTH < birdX) {
                p.scored = true;
                score++;
                bestScore = Math.max(bestScore, score);
            }
        }

        // Collisions: ground, ceiling, or pipes
        if (birdY < 0) {
            birdY = 0;
            velY = 0;
        }
        if (birdY + BIRD_SIZE > HEIGHT - GROUND_HEIGHT) {
            birdY = HEIGHT - GROUND_HEIGHT - BIRD_SIZE;
            die();
        } else {
            // Bird rectangle
            Rectangle bird = new Rectangle((int) birdX, (int) birdY, BIRD_SIZE, BIRD_SIZE);

            for (PipePair p : pipes) {
                Rectangle topPipe = new Rectangle(p.x, 0, PIPE_WIDTH, p.gapY);
                Rectangle bottomPipe = new Rectangle(p.x, p.gapY + PIPE_GAP, PIPE_WIDTH,
                        HEIGHT - GROUND_HEIGHT - (p.gapY + PIPE_GAP));
                if (bird.intersects(topPipe) || bird.intersects(bottomPipe)) {
                    die();
                    break;
                }
            }
        }

        repaint();
    }

    private void die() {
        gameOver = true;
        running = false;
    }

    private void flap() {
        if (gameOver) return;
        velY = JUMP_VELOCITY;
    }

    // ---------- Rendering ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background gradient sky
        Paint old = g2.getPaint();
        g2.setPaint(new GradientPaint(0, 0, new Color(135, 206, 250),
                                      0, HEIGHT, new Color(100, 180, 240)));
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setPaint(old);

        // Pipes
        for (PipePair p : pipes) {
            // Top
            g2.setColor(new Color(34, 139, 34));
            g2.fillRect(p.x, 0, PIPE_WIDTH, p.gapY);
            // Bottom
            g2.fillRect(p.x, p.gapY + PIPE_GAP, PIPE_WIDTH,
                    HEIGHT - GROUND_HEIGHT - (p.gapY + PIPE_GAP));
            // Pipe borders for definition
            g2.setColor(new Color(0, 100, 0));
            g2.drawRect(p.x, 0, PIPE_WIDTH, p.gapY);
            g2.drawRect(p.x, p.gapY + PIPE_GAP, PIPE_WIDTH,
                    HEIGHT - GROUND_HEIGHT - (p.gapY + PIPE_GAP));
        }

        // Bird (simple circle with a beak)
        int bx = (int) birdX;
        int by = (int) birdY;
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(bx, by, BIRD_SIZE, BIRD_SIZE);
        g2.setColor(Color.BLACK);
        g2.drawOval(bx, by, BIRD_SIZE, BIRD_SIZE);
        // Eye
        g2.setColor(Color.WHITE);
        g2.fillOval(bx + BIRD_SIZE / 2, by + BIRD_SIZE / 5, 6, 6);
        g2.setColor(Color.BLACK);
        g2.fillOval(bx + BIRD_SIZE / 2 + 2, by + BIRD_SIZE / 5 + 2, 3, 3);
        // Beak
        g2.setColor(new Color(255, 140, 0));
        int[] px = { bx + BIRD_SIZE, bx + BIRD_SIZE + 8, bx + BIRD_SIZE };
        int[] py = { by + BIRD_SIZE / 2 - 3, by + BIRD_SIZE / 2, by + BIRD_SIZE / 2 + 3 };
        g2.fillPolygon(px, py, 3);

        // Ground
        g2.setColor(new Color(222, 184, 135));
        g2.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, GROUND_HEIGHT);
        g2.setColor(new Color(160, 82, 45));
        g2.fillRect(0, HEIGHT - GROUND_HEIGHT, WIDTH, 8);

        // Score & UI
        g2.setFont(g2.getFont().deriveFont(Font.BOLD, 28f));
        g2.setColor(Color.BLACK);
        String scoreText = "Score: " + score + "   Best: " + bestScore;
        g2.drawString(scoreText, 12, 36);

        if (paused && !gameOver) {
            drawCenteredText(g2, "PAUSED (P to resume)", 48);
        }

        if (gameOver) {
            drawCenteredText(g2, "GAME OVER", 60);
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 20f));
            drawCenteredText(g2, "Press R to restart", 20, HEIGHT / 2 + 40);
        } else if (!paused) {
            g2.setFont(g2.getFont().deriveFont(Font.PLAIN, 18f));
            g2.setColor(new Color(0, 0, 0, 140));
            g2.drawString("SPACE/UP: Flap   P: Pause   R: Restart", 12, HEIGHT - GROUND_HEIGHT + 28);
        }

        g2.dispose();
    }

    private void drawCenteredText(Graphics2D g2, String text, int size) {
        drawCenteredText(g2, text, size, HEIGHT / 2);
    }

    private void drawCenteredText(Graphics2D g2, String text, int size, int y) {
        Font old = g2.getFont();
        g2.setFont(old.deriveFont(Font.BOLD, size));
        FontMetrics fm = g2.getFontMetrics();
        int x = (WIDTH - fm.stringWidth(text)) / 2;
        int ascent = fm.getAscent();
        // Drop shadow
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(text, x + 2, y + ascent / 2 + 2);
        // Text
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y + ascent / 2);
        g2.setFont(old);
    }

    // ---------- Input ----------
    @Override public void keyTyped(KeyEvent e) { }

    @Override
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_SPACE || k == KeyEvent.VK_UP) {
            if (gameOver) return;
            flap();
        } else if (k == KeyEvent.VK_R) {
            resetGame();
        } else if (k == KeyEvent.VK_P) {
            if (!gameOver) paused = !paused;
        }
    }

    @Override public void keyReleased(KeyEvent e) { }

    // ---------- Boot ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Flappy Bird - Java (Swing)");
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            FlappyBird game = new FlappyBird();
            f.setContentPane(game);
            f.pack();
            f.setResizable(false);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}

