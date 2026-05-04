package Frontend;

import Backend.Player;
import Backend.RoundManager;
import Backend.RoundManager.GameResult;
import Backend.RoundManager.GameResult.PlayerResult;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class ResultsPanel extends JPanel implements MouseListener {

    // ── Layout constants ─────────────────────────────────────────────────────

    // Table origin and column widths
    private static final int TABLE_X      = 940;
    private static final int TABLE_TOP    = 160;
    private static final int ROW_H        = 90;       // height of each player row
    private static final int HEADER_H     = 60;       // height of the column-header row

    private static final int COL_RANK_W   = 60;
    private static final int COL_NAME_W   = 210;
    private static final int COL_POW_W    = 170;
    private static final int COL_MONEY_W  = 170;
    private static final int COL_CITIES_W = 170;

    private static final int TABLE_W =
            COL_RANK_W + COL_NAME_W + COL_POW_W + COL_MONEY_W + COL_CITIES_W;

    // Horizontal grid-line x-positions (left edge of each column)
    private static final int X_RANK   = TABLE_X;
    private static final int X_NAME   = X_RANK  + COL_RANK_W;
    private static final int X_POW    = X_NAME  + COL_NAME_W;
    private static final int X_MONEY  = X_POW   + COL_POW_W;
    private static final int X_CITIES = X_MONEY + COL_MONEY_W;
    private static final int X_END    = X_CITIES + COL_CITIES_W;

    // Colours
    private static final Color GOLD   = new Color(212, 175, 55);
    private static final Color SILVER = new Color(180, 180, 190);
    private static final Color BRONZE = new Color(176, 112, 60);
    private static final Color HEADER_BG  = new Color(60,  40,  15, 210);
    private static final Color ROW_ODD    = new Color(30,  20,  5,  180);
    private static final Color ROW_EVEN   = new Color(50,  35,  10, 180);
    private static final Color WINNER_BG  = new Color(180, 140, 20, 100);
    private static final Color BORDER_COL = new Color(120, 80, 25);
    private static final Color TEXT_WHITE = Color.WHITE;
    private static final Color TEXT_GOLD  = new Color(255, 230, 100);

    // ── State ────────────────────────────────────────────────────────────────

    private final PowergridFrame frame;
    private BufferedImage background;

    // ── Constructor ──────────────────────────────────────────────────────────

    public ResultsPanel(PowergridFrame frame) {
        this.frame = frame;
        setLayout(null);

        try {
            background = ImageIO.read(getClass().getResource("/Images/background.png"));
        } catch (Exception e) {
            System.err.println("ResultsPanel: could not load background – " + e.getMessage());
        }

        initUI();
        addMouseListener(this);
    }

    // ── UI init ──────────────────────────────────────────────────────────────

    private void initUI() {
        // "PLAY AGAIN" / quit buttons sit below the table.
        // Adjust y if your frame is taller/shorter.
        JButton quitBtn = new JButton("QUIT");
        quitBtn.setFont(new Font("Arial", Font.BOLD, 28));
        quitBtn.setBackground(new Color(180, 60, 60));
        quitBtn.setForeground(Color.WHITE);
        quitBtn.setOpaque(true);
        quitBtn.setBorderPainted(true);
        quitBtn.setBorder(new LineBorder(Color.GRAY));
        quitBtn.setBounds(TABLE_X + TABLE_W / 2 - 90, TABLE_TOP + HEADER_H + ROW_H * 6 + 30,
                180, 60);
        quitBtn.addActionListener(e -> System.exit(0));
        add(quitBtn);
    }

    // ── Entry-point called by PowergridFrame.showScreen ──────────────────────

    /**
     * Called every time the RESULTS screen becomes visible.
     * Pulls the finalized GameResult from RoundManager and repaints.
     */
    public void startResultsPhase() {
        repaint();
    }

    // ── Painting ─────────────────────────────────────────────────────────────

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background
        if (background != null) {
            g.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        } else {
            g.setColor(new Color(30, 20, 5));
            g.fillRect(0, 0, getWidth(), getHeight());
        }

        GameResult result = frame.getRoundManager().getGameResult();
        if (result == null) {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(Color.WHITE);
            g.drawString("No results available.", 900, 400);
            return;
        }

        paintTitle(g, result);
        paintTable(g, result);
    }

    // ── Title / winner banner ─────────────────────────────────────────────────

    private void paintTitle(Graphics g, GameResult result) {
        // "GAME OVER" header — matches style of other panels
        g.setFont(new Font("Arial", Font.BOLD, 40));
        g.setColor(Color.WHITE);
        g.drawString("GAME OVER — FINAL RESULTS", TABLE_X, 49);

        // Horizontal rule
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(3));
        g2.setColor(BORDER_COL);
        g2.drawLine(TABLE_X, 60, X_END, 60);

        // Winner announcement
        if (result.winner != null) {
            String winnerName = result.winner.getName();
            Color  winnerCol  = PanelUtils.parseColor(result.winner.getColor());

            // Coloured swatch next to name
            g.setColor(winnerCol);
            g.fillOval(TABLE_X, 75, 30, 30);

            g.setFont(new Font("Arial", Font.BOLD, 34));
            g.setColor(TEXT_GOLD);
            g.drawString("🏆  " + winnerName + " wins!", TABLE_X + 40, 100);

            // Tiebreak note (which stat decided it)
            String note = determineTiebreakNote(result);
            g.setFont(new Font("Arial", Font.ITALIC, 18));
            g.setColor(new Color(220, 200, 140));
            g.drawString(note, TABLE_X + 40, 125);
        }
    }

    /** Returns a human-readable string explaining which stat decided the winner. */
    private String determineTiebreakNote(GameResult result) {
        List<PlayerResult> rows = result.rankedResults;
        if (rows.size() < 2) return "Sole competitor — automatic champion.";

        PlayerResult first  = rows.get(0);
        PlayerResult second = rows.get(1);

        if (first.citiesPowered != second.citiesPowered) {
            return "Won by powering the most cities (" + first.citiesPowered + ").";
        }
        if (first.money != second.money) {
            return "Tiebreak: most Elektro ($" + first.money + ").";
        }
        return "Tiebreak: most cities built (" + first.citiesOwned + ").";
    }

    // ── Results table ─────────────────────────────────────────────────────────

    private void paintTable(Graphics g, GameResult result) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        List<PlayerResult> rows = result.rankedResults;
        int tableBottom = TABLE_TOP + HEADER_H + ROW_H * rows.size();

        // ── Column header background ──────────────────────────────────────────
        g2.setColor(HEADER_BG);
        g2.fillRect(TABLE_X, TABLE_TOP, TABLE_W, HEADER_H);

        // ── Header text ───────────────────────────────────────────────────────
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.setColor(TEXT_GOLD);
        drawCentered(g, "#",          X_RANK,   X_NAME,   TABLE_TOP, HEADER_H);
        drawCentered(g, "PLAYER",     X_NAME,   X_POW,    TABLE_TOP, HEADER_H);
        drawCentered(g, "CITIES POWERED", X_POW, X_MONEY, TABLE_TOP, HEADER_H);
        drawCentered(g, "ELEKTRO",    X_MONEY,  X_CITIES, TABLE_TOP, HEADER_H);
        drawCentered(g, "CITIES BUILT", X_CITIES, X_END,  TABLE_TOP, HEADER_H);

        // ── Player rows ───────────────────────────────────────────────────────
        for (int i = 0; i < rows.size(); i++) {
            PlayerResult pr = rows.get(i);
            int rowY = TABLE_TOP + HEADER_H + i * ROW_H;

            // Row background
            Color rowBg = pr.isWinner ? WINNER_BG : (i % 2 == 0 ? ROW_EVEN : ROW_ODD);
            g2.setColor(rowBg);
            g2.fillRect(TABLE_X, rowY, TABLE_W, ROW_H);

            // Rank medal colour
            Color rankColor = switch (i) {
                case 0 -> GOLD;
                case 1 -> SILVER;
                case 2 -> BRONZE;
                default -> new Color(200, 200, 200);
            };

            // Rank number (e.g. "1st")
            g.setFont(new Font("Arial", Font.BOLD, 26));
            g.setColor(rankColor);
            drawCentered(g, ordinal(i + 1), X_RANK, X_NAME, rowY, ROW_H);

            // Player colour swatch + name
            Color playerColor = PanelUtils.parseColor(pr.player.getColor());
            int swatchX = X_NAME + 10;
            int swatchY = rowY + ROW_H / 2 - 12;
            g.setColor(playerColor);
            g.fillOval(swatchX, swatchY, 24, 24);
            g.setColor(pr.isWinner ? TEXT_GOLD : TEXT_WHITE);
            g.setFont(new Font("Arial", Font.BOLD, pr.isWinner ? 24 : 22));
            g.drawString(pr.player.getName(), swatchX + 32, rowY + ROW_H / 2 + 9);

            // Cities powered
            g.setFont(new Font("Arial", Font.BOLD, 28));
            g.setColor(pr.isWinner ? TEXT_GOLD : TEXT_WHITE);
            drawCentered(g, String.valueOf(pr.citiesPowered), X_POW,    X_MONEY,  rowY, ROW_H);

            // Money
            drawCentered(g, "$" + pr.money,                  X_MONEY,  X_CITIES, rowY, ROW_H);

            // Cities owned
            drawCentered(g, String.valueOf(pr.citiesOwned),  X_CITIES, X_END,    rowY, ROW_H);

            // Winner crown overlay on left edge
            if (pr.isWinner) {
                g.setFont(new Font("Arial", Font.PLAIN, 22));
                g.setColor(GOLD);
                g.drawString("👑", X_RANK + 6, rowY + 22);
            }
        }

        // ── Grid lines ────────────────────────────────────────────────────────
        g2.setStroke(new BasicStroke(2));
        g2.setColor(BORDER_COL);

        // Vertical column dividers
        for (int x : new int[]{X_RANK, X_NAME, X_POW, X_MONEY, X_CITIES, X_END}) {
            g2.drawLine(x, TABLE_TOP, x, tableBottom);
        }

        // Horizontal row dividers
        g2.drawLine(TABLE_X, TABLE_TOP,              X_END, TABLE_TOP);
        g2.drawLine(TABLE_X, TABLE_TOP + HEADER_H,   X_END, TABLE_TOP + HEADER_H);
        for (int i = 1; i <= rows.size(); i++) {
            int y = TABLE_TOP + HEADER_H + i * ROW_H;
            g2.drawLine(TABLE_X, y, X_END, y);
        }

        // Outer border (thicker)
        g2.setStroke(new BasicStroke(3));
        g2.drawRect(TABLE_X, TABLE_TOP, TABLE_W, tableBottom - TABLE_TOP);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Draws {@code text} centred inside the cell defined by [x1,x2] × [y, y+h]. */
    private void drawCentered(Graphics g, String text, int x1, int x2, int y, int h) {
        FontMetrics fm = g.getFontMetrics();
        int textW = fm.stringWidth(text);
        int textH = fm.getAscent();
        g.drawString(text, x1 + (x2 - x1 - textW) / 2, y + (h + textH) / 2 - 2);
    }

    /** Returns "1st", "2nd", "3rd", "4th", … */
    private String ordinal(int n) {
        return switch (n) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> n + "th";
        };
    }

    // ── MouseListener ─────────────────────────────────────────────────────────

    @Override
    public void mouseClicked(MouseEvent e) {
        System.out.println("(" + e.getX() + ", " + e.getY() + ")");
    }
    @Override public void mousePressed(MouseEvent e)  {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e)  {}
    @Override public void mouseExited(MouseEvent e)   {}
}