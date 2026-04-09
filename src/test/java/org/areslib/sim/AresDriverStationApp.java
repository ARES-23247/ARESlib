package org.areslib.sim;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import org.areslib.core.FieldConstants;

public class AresDriverStationApp extends JFrame {

  private final DesktopKeyboardListener keyboardListener;
  private final VirtualGamepadWrapper gamepadWrapper;

  private JRadioButton keyboardBtn;
  private JRadioButton gamepadBtn;
  private JToggleButton autoBtn;
  private JToggleButton allianceBtn;

  // HUD Data
  private double robotX = 0;
  private double robotY = 0;
  private double robotTheta = 0;
  private int heldSamples = 0;

  private float leftX, leftY, rightX, rightY;
  private boolean lb, rb;
  private float rt, lt;
  private boolean aBtn, bBtn, xBtn, yBtn;

  /** Current alliance — determines field-centric heading offset. */
  private volatile FieldConstants.Alliance currentAlliance = FieldConstants.Alliance.BLUE;

  private final HudPanel hudPanel;

  public AresDriverStationApp() {
    super("ARES Simulation Driver Station");

    this.keyboardListener = new DesktopKeyboardListener();
    this.gamepadWrapper = new VirtualGamepadWrapper(this.keyboardListener);

    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception e) {
      java.util.logging.Logger.getLogger(AresDriverStationApp.class.getName())
          .log(java.util.logging.Level.SEVERE, "Failed to set system look and feel", e);
    }

    this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    this.setSize(850, 650);
    this.setLocationRelativeTo(null);
    this.setFocusable(true);
    this.addKeyListener(keyboardListener);

    // Main Container with deep dark background
    JPanel mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBackground(new Color(18, 18, 20));

    // Top Controls Panel (Glass-like theme)
    JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 15));
    topPanel.setOpaque(false);

    JLabel titleLabel = new JLabel("ARES UNIFIED HUD");
    titleLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
    titleLabel.setForeground(new Color(230, 230, 230));
    topPanel.add(titleLabel);

    keyboardBtn = createCustomRadio("Keyboard Profile");
    gamepadBtn = createCustomRadio("Physical Gamepad");

    ButtonGroup group = new ButtonGroup();
    group.add(keyboardBtn);
    group.add(gamepadBtn);

    keyboardBtn.setSelected(true); // Default

    keyboardBtn.addActionListener(
        e -> {
          gamepadWrapper.setInputMode(VirtualGamepadWrapper.InputMode.KEYBOARD);
          this.requestFocusInWindow(); // Return focus directly to JFrame so Swing intercepts keys
        });

    gamepadBtn.addActionListener(
        e -> {
          gamepadWrapper.setInputMode(VirtualGamepadWrapper.InputMode.PHYSICAL);
        });

    topPanel.add(keyboardBtn);
    topPanel.add(gamepadBtn);

    // Alliance Toggle Button
    allianceBtn = new JToggleButton("BLUE");
    allianceBtn.setSelected(true); // Start on Blue
    allianceBtn.setFocusPainted(false);
    allianceBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
    allianceBtn.setForeground(new Color(50, 120, 255));
    allianceBtn.setBackground(new Color(30, 30, 30));
    allianceBtn.setPreferredSize(new Dimension(100, 32));
    allianceBtn.addActionListener(
        e -> {
          if (allianceBtn.isSelected()) {
            currentAlliance = FieldConstants.Alliance.BLUE;
            allianceBtn.setText("BLUE");
            allianceBtn.setForeground(new Color(50, 120, 255));
          } else {
            currentAlliance = FieldConstants.Alliance.RED;
            allianceBtn.setText("RED");
            allianceBtn.setForeground(new Color(255, 60, 60));
          }
          this.requestFocusInWindow();
        });
    topPanel.add(allianceBtn);

    autoBtn = new JToggleButton("Run Auto Mode");
    autoBtn.setFocusPainted(false);
    autoBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
    autoBtn.setForeground(new Color(255, 100, 100));
    autoBtn.setBackground(new Color(30, 30, 30));
    topPanel.add(autoBtn);

    JButton stopAutoBtn = new JButton("Stop Auto");
    stopAutoBtn.setFocusPainted(false);
    stopAutoBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
    stopAutoBtn.setForeground(new Color(100, 200, 100));
    stopAutoBtn.setBackground(new Color(30, 30, 30));
    stopAutoBtn.addActionListener(
        e -> {
          if (autoBtn.isSelected()) {
            autoBtn.setSelected(false);
          }
        });
    topPanel.add(stopAutoBtn);

    mainPanel.add(topPanel, BorderLayout.NORTH);

    hudPanel = new HudPanel();
    mainPanel.add(hudPanel, BorderLayout.CENTER);

    this.setContentPane(mainPanel);
    this.setVisible(true);
    this.requestFocusInWindow();
  }

  private JRadioButton createCustomRadio(String text) {
    JRadioButton rb = new JRadioButton(text);
    rb.setOpaque(false);
    rb.setForeground(new Color(200, 200, 200));
    rb.setFont(new Font("SansSerif", Font.PLAIN, 14));
    rb.setFocusPainted(false);
    return rb;
  }

  /**
   * @return The currently selected alliance for field-centric heading offset.
   */
  public FieldConstants.Alliance getAlliance() {
    return currentAlliance;
  }

  public void updateHud(double x, double y, double heading, int samples) {
    this.robotX = x;
    this.robotY = y;
    this.robotTheta = heading;
    this.heldSamples = samples;
  }

  public void updateGamepadState(
      float lx,
      float ly,
      float rx,
      float ry,
      boolean lb,
      boolean rb,
      float lt,
      float rt,
      boolean a,
      boolean b,
      boolean x,
      boolean y) {
    this.leftX = lx;
    this.leftY = ly;
    this.rightX = rx;
    this.rightY = ry;
    this.lb = lb;
    this.rb = rb;
    this.lt = lt;
    this.rt = rt;
    this.aBtn = a;
    this.bBtn = b;
    this.xBtn = x;
    this.yBtn = y;

    if (hudPanel != null) {
      hudPanel.repaint(); // force rapid 50hz redraws for smooth GUI
    }
  }

  public VirtualGamepadWrapper getGamepadWrapper() {
    return gamepadWrapper;
  }

  public boolean isAutoModeEnabled() {
    return autoBtn != null && autoBtn.isSelected();
  }

  private class HudPanel extends JPanel {
    HudPanel() {
      setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      int width = getWidth();
      int height = getHeight();

      int panelMargin = 20;
      int panelW = (width / 2) - 30;
      int panelH = height - 40;

      // Stats Glassmorphism Panel
      drawGlassPanel(g2d, panelMargin, panelMargin, panelW, panelH, "TELEMETRY & STATE");

      g2d.setColor(new Color(150, 255, 150));
      g2d.setFont(new Font("Consolas", Font.BOLD, 18));
      int startTextY = panelMargin + 70;
      g2d.drawString(String.format("X (meters)   : %7.3f", robotX), panelMargin + 25, startTextY);
      g2d.drawString(
          String.format("Y (meters)   : %7.3f", robotY), panelMargin + 25, startTextY + 30);
      g2d.drawString(
          String.format("Heading (deg): %7.2f", Math.toDegrees(robotTheta)),
          panelMargin + 25,
          startTextY + 60);

      // Draw Alliance indicator
      FieldConstants.Alliance alliance = currentAlliance;
      Color allianceColor =
          (alliance == FieldConstants.Alliance.BLUE)
              ? new Color(50, 120, 255)
              : new Color(255, 60, 60);
      g2d.setColor(allianceColor);
      g2d.setFont(new Font("Consolas", Font.BOLD, 18));
      g2d.drawString(
          String.format("Alliance     : %s", alliance.name()), panelMargin + 25, startTextY + 90);

      // Draw Held Samples
      g2d.setColor(new Color(255, 200, 50));
      g2d.drawString(
          String.format("Held Samples : %d", heldSamples), panelMargin + 25, startTextY + 130);

      // Draw connected state
      boolean hasGamepad =
          (gamepadWrapper.getControllerManager() != null
              && gamepadWrapper.getControllerManager().getNumControllers() > 0);
      if (gamepadWrapper.getInputMode() == VirtualGamepadWrapper.InputMode.PHYSICAL) {
        if (hasGamepad) {
          g2d.setColor(new Color(50, 200, 50));
          g2d.drawString("USB Gamepad Connected [OK]", panelMargin + 25, startTextY + 190);
        } else {
          g2d.setColor(new Color(255, 50, 50));
          g2d.drawString("No USB Gamepad [WARN]", panelMargin + 25, startTextY + 190);
        }
      } else {
        if (isAutoModeEnabled()) {
          g2d.setColor(new Color(255, 100, 100));
          g2d.drawString("AUTO RUNNING [LOCKOUT]", panelMargin + 25, startTextY + 190);
        } else {
          g2d.setColor(new Color(100, 150, 255));
          g2d.drawString("Keyboard Input [ACTIVE]", panelMargin + 25, startTextY + 190);
        }

        g2d.setColor(new Color(150, 150, 160));
        g2d.setFont(new Font("Consolas", Font.PLAIN, 13));
        int kbY = startTextY + 230;
        g2d.drawString("KEYBINDINGS:", panelMargin + 25, kbY);
        g2d.drawString("Left Stick  : W / A / S / D", panelMargin + 25, kbY + 20);
        g2d.drawString("Right Stick : Arrow Keys", panelMargin + 25, kbY + 40);
        g2d.drawString("A Button    : J", panelMargin + 25, kbY + 60);
        g2d.drawString("B Button    : L", panelMargin + 25, kbY + 80);
        g2d.drawString("X Button    : U", panelMargin + 25, kbY + 100);
        g2d.drawString("Y Button    : I", panelMargin + 25, kbY + 120);
        g2d.drawString("L Bumper (Outtake) : Q", panelMargin + 25, kbY + 140);
        g2d.drawString("R Bumper (Intake)  : E", panelMargin + 25, kbY + 160);
        g2d.drawString("L Trigger          : Space", panelMargin + 25, kbY + 180);
        g2d.drawString("R Trigger (Boost)  : Shift", panelMargin + 25, kbY + 200);
      }

      // Gamepad Glassmorphism Panel
      int rightXStart = width / 2 + 10;
      drawGlassPanel(g2d, rightXStart, panelMargin, panelW, panelH, "GAMEPAD INPUT");

      int padCenterY = panelMargin + (panelH / 2) + 20;
      int padLx = rightXStart + panelW / 3 - 10;
      int padRx = rightXStart + panelW * 2 / 3 + 10;
      int joyRadius = 40;

      // Draw Virtual Joysticks
      drawJoystick(g2d, padLx, padCenterY, joyRadius, leftX, leftY);
      drawJoystick(g2d, padRx, padCenterY, joyRadius, rightX, rightY);

      // Draw Shoulder Bumpers
      drawBumper(g2d, rightXStart + 40, panelMargin + 60, "LB", lb);
      drawBumper(g2d, rightXStart + panelW - 90, panelMargin + 60, "RB", rb);

      // Draw Triggers
      drawBumper(g2d, rightXStart + 40, panelMargin + 100, "LT", lt > 0.5f);
      drawBumper(g2d, rightXStart + panelW - 90, panelMargin + 100, "RT", rt > 0.5f);

      // Draw Face Buttons
      int faceX = rightXStart + panelW / 2;
      int faceY = panelMargin + 120;
      drawFaceButton(g2d, faceX, faceY - 25, "Y", yBtn, new Color(255, 200, 50));
      drawFaceButton(g2d, faceX, faceY + 25, "A", aBtn, new Color(50, 255, 100));
      drawFaceButton(g2d, faceX - 25, faceY, "X", xBtn, new Color(50, 150, 255));
      drawFaceButton(g2d, faceX + 25, faceY, "B", bBtn, new Color(255, 50, 50));

      g2d.dispose();
    }

    private void drawGlassPanel(Graphics2D g2d, int x, int y, int w, int h, String title) {
      RoundRectangle2D rect = new RoundRectangle2D.Float(x, y, w, h, 25, 25);
      g2d.setColor(new Color(40, 40, 45, 180));
      g2d.fill(rect);
      g2d.setColor(new Color(80, 80, 90, 150));
      g2d.setStroke(new BasicStroke(2f));
      g2d.draw(rect);

      g2d.setColor(new Color(200, 200, 220));
      g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
      g2d.drawString(title, x + 25, y + 35);

      g2d.setColor(new Color(80, 80, 90, 150));
      g2d.drawLine(x + 20, y + 50, x + w - 20, y + 50);
    }

    private void drawJoystick(Graphics2D g2d, int x, int y, int r, float vx, float vy) {
      g2d.setColor(new Color(30, 30, 35));
      g2d.fillOval(x - r, y - r, r * 2, r * 2);
      g2d.setColor(new Color(100, 100, 110));
      g2d.drawOval(x - r, y - r, r * 2, r * 2);

      int innerX = (int) (x + vx * r);
      int innerY = (int) (y + vy * r);

      g2d.setColor(new Color(50, 150, 255));
      g2d.fillOval(innerX - 8, innerY - 8, 16, 16);

      g2d.setStroke(new BasicStroke(1.5f));
      g2d.drawLine(x, y, innerX, innerY);
    }

    private void drawBumper(Graphics2D g2d, int x, int y, String label, boolean pressed) {
      g2d.setColor(pressed ? new Color(50, 255, 100) : new Color(50, 50, 60));
      g2d.fillRoundRect(x, y, 50, 25, 10, 10);

      g2d.setColor(pressed ? Color.BLACK : Color.WHITE);
      g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
      g2d.drawString(label, x + 18, y + 17);
    }

    private void drawFaceButton(
        Graphics2D g2d, int x, int y, String label, boolean pressed, Color tint) {
      int r = 16;
      g2d.setColor(pressed ? tint : new Color(40, 40, 50));
      g2d.fillOval(x - r, y - r, r * 2, r * 2);

      g2d.setColor(tint.darker());
      g2d.setStroke(new BasicStroke(2f));
      g2d.drawOval(x - r, y - r, r * 2, r * 2);

      g2d.setColor(pressed ? new Color(18, 18, 20) : tint);
      g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
      g2d.drawString(label, x - 5, y + 5);
    }
  }
}
