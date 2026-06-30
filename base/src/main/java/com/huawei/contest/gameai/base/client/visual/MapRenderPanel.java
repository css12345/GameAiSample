package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.entity.GameUnit;
import com.huawei.contest.gameai.base.client.entity.Position;
import com.huawei.contest.gameai.base.client.entity.UnitType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * 地图渲染面板 — 多层绘制网格地图，支持坐标标注、缩放、平移和鼠标交互。
 */
public class MapRenderPanel extends JPanel {

    // 显示层开关
    private boolean showThreatMap = true;
    private boolean showReservations = true;
    private boolean showPaths = true;
    private boolean showVisitedNodes = false;
    private boolean showCoords = true;

    // 渲染参数
    private int cellSize = 24;
    private int panX = 0;
    private int panY = 0;
    private Point lastMouse;

    /** 坐标轴边距 */
    private static final int AXIS_MARGIN = 28;

    // 数据引用
    private MovementSimulator sim;

    // 交互状态
    private UnitType selectedUnitType = UnitType.FIGHTER;
    private boolean placingEnemy = false;

    // 当前悬停的格子坐标（用于状态栏显示）
    private Position hoveredCell;
    private BiConsumer<Integer, Integer> onHoverCallback;

    // 颜色常量
    private static final Color COLOR_EMPTY = new Color(0xF0, 0xF0, 0xF0);
    private static final Color COLOR_TREE = new Color(0x22, 0x8B, 0x22);
    private static final Color COLOR_MOUNTAIN = new Color(0x69, 0x69, 0x69);
    private static final Color COLOR_GOLDMINE = new Color(0xFF, 0xD7, 0x00);
    private static final Color COLOR_GEMMINE = new Color(0x41, 0x69, 0xE1);
    private static final Color COLOR_STATION = new Color(0x00, 0xCD, 0xCD);
    private static final Color COLOR_MY_UNIT = new Color(0x1E, 0x90, 0xFF);
    private static final Color COLOR_ENEMY_UNIT = new Color(0xDC, 0x14, 0x3C);
    private static final Color COLOR_GUARDIAN = new Color(0x8B, 0x00, 0x8B);
    private static final Color COLOR_TARGET = new Color(0xFF, 0x45, 0x00);
    private static final Color COLOR_VISITED = new Color(0xFF, 0xD7, 0x00, 0x60);
    private static final Color COLOR_RESERVATION = new Color(0x00, 0x99, 0xCC, 0x80);
    private static final Color COLOR_GRID = new Color(0xCC, 0xCC, 0xCC);
    private static final Color COLOR_AXIS_BG = new Color(0xE8, 0xE8, 0xE8);
    private static final Color COLOR_AXIS_TEXT = new Color(0x33, 0x33, 0x33);

    // 路径彩虹色
    private static final Color[] PATH_COLORS = {
            new Color(0xE6, 0x19, 0x4B),
            new Color(0x3C, 0xB4, 0x4B),
            new Color(0x42, 0x8B, 0xFF),
            new Color(0xF5, 0x82, 0x31),
            new Color(0x91, 0x1E, 0xB4),
            new Color(0x46, 0xF0, 0xF0),
            new Color(0xF0, 0x32, 0xE6),
            new Color(0xFA, 0xFF, 0x0A),
    };

    public MapRenderPanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                if (e.getButton() == MouseEvent.BUTTON2 ||
                        (e.getButton() == MouseEvent.BUTTON1 && e.isShiftDown())) {
                    lastMouse = e.getPoint();
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    handleLeftClick(e);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    handleRightClick(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (lastMouse != null) {
                    panX += e.getX() - lastMouse.x;
                    panY += e.getY() - lastMouse.y;
                    lastMouse = e.getPoint();
                    repaint();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                lastMouse = null;
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int[] grid = screenToGrid(e.getX(), e.getY());
                if (grid != null) {
                    hoveredCell = Position.of(grid[0], grid[1]);
                } else {
                    hoveredCell = null;
                }
                if (onHoverCallback != null && hoveredCell != null) {
                    onHoverCallback.accept(hoveredCell.getX(), hoveredCell.getY());
                }
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int oldCellSize = cellSize;
                cellSize = Math.max(8, Math.min(48, cellSize - e.getWheelRotation() * 2));
                double scale = (double) cellSize / oldCellSize;
                panX = (int)(e.getX() - scale * (e.getX() - panX));
                panY = (int)(e.getY() - scale * (e.getY() - panY));
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    public void setOnHoverCallback(BiConsumer<Integer, Integer> cb) { this.onHoverCallback = cb; }

    private void handleLeftClick(MouseEvent e) {
        if (sim == null || sim.getWorld() == null) return;
        int[] grid = screenToGrid(e.getX(), e.getY());
        if (grid == null) return;
        int gx = grid[0], gy = grid[1];
        if (placingEnemy) {
            sim.addEnemyUnit(selectedUnitType, gx, gy);
        } else {
            sim.addMyUnit(selectedUnitType, gx, gy);
        }
        repaint();
    }

    private void handleRightClick(MouseEvent e) {
        if (sim == null) return;
        int[] grid = screenToGrid(e.getX(), e.getY());
        if (grid == null) return;
        sim.setTarget(grid[0], grid[1]);
        repaint();
    }

    public void setSimulator(MovementSimulator sim) {
        this.sim = sim;
        repaint();
    }

    // ==================== 显示层开关 ====================

    public void setShowThreatMap(boolean v) { showThreatMap = v; repaint(); }
    public void setShowReservations(boolean v) { showReservations = v; repaint(); }
    public void setShowPaths(boolean v) { showPaths = v; repaint(); }
    public void setShowVisitedNodes(boolean v) { showVisitedNodes = v; repaint(); }
    public void setShowCoords(boolean v) { showCoords = v; repaint(); }
    public void setSelectedUnitType(UnitType type) { this.selectedUnitType = type; }
    public void setPlacingEnemy(boolean v) { this.placingEnemy = v; }

    // ==================== 坐标转换 ====================

    private int[] screenToGrid(int sx, int sy) {
        if (sim == null || sim.getWorld() == null) return null;
        int h = sim.getWorld().getHeight();
        // 减去轴边距
        int gx = (sx - panX - AXIS_MARGIN) / cellSize;
        int gy = h - 1 - (sy - panY - AXIS_MARGIN) / cellSize;
        if (gx < 0 || gx >= sim.getWorld().getWidth() || gy < 0 || gy >= h) return null;
        return new int[]{gx, gy};
    }

    private int gridToScreenX(int gx) {
        return panX + AXIS_MARGIN + gx * cellSize;
    }

    private int gridToScreenY(int gy) {
        if (sim == null || sim.getWorld() == null) return panY;
        int h = sim.getWorld().getHeight();
        return panY + AXIS_MARGIN + (h - 1 - gy) * cellSize;
    }

    // ==================== 绘制 ====================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (sim == null || sim.getWorld() == null) {
            g.setColor(Color.WHITE);
            g.drawString("请先选择地图", 20, 30);
            return;
        }

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = sim.getWorld().getWidth();
        int h = sim.getWorld().getHeight();

        // 坐标轴背景
        drawAxisBackground(g2, w, h);

        // Layer 1: 地形
        drawTerrain(g2, w, h);

        // Layer 2: 威胁热力图
        if (showThreatMap) drawThreatMap(g2, w, h);

        // Layer 3: A* 访问节点
        if (showVisitedNodes) drawVisitedNodes(g2);

        // Layer 4: 预留表
        if (showReservations && sim.hasPlan() && sim.getMaxPathLen() > 0) {
            drawReservations(g2, w, h);
        }

        // Layer 5: 路径
        if (showPaths && sim.hasPlan()) drawPaths(g2);

        // Layer 6: 网格线
        drawGrid(g2, w, h);

        // Layer 7: 单位
        drawUnits(g2);

        // Layer 8: 目标
        if (sim.getTarget() != null) drawTarget(g2);

        // Layer 9: 坐标标注（在最上层）
        if (showCoords) drawCoordinateLabels(g2, w, h);

        // Layer 10: 悬停高亮
        if (hoveredCell != null) drawHoverHighlight(g2, hoveredCell);
    }

    private void drawAxisBackground(Graphics2D g2, int w, int h) {
        int right = gridToScreenX(w - 1) + cellSize;
        int bottom = gridToScreenY(0) + cellSize;
        g2.setColor(COLOR_AXIS_BG);
        // 左边 Y 轴背景
        g2.fillRect(panX, panY, AXIS_MARGIN, bottom - panY);
        // 底部 X 轴背景
        g2.fillRect(panX, bottom, right - panX, AXIS_MARGIN);
    }

    private void drawCoordinateLabels(Graphics2D g2, int w, int h) {
        g2.setColor(COLOR_AXIS_TEXT);
        int fontSize = Math.max(9, Math.min(12, cellSize * 9 / 20));
        Font axisFont = new Font("SansSerif", Font.PLAIN, fontSize);
        g2.setFont(axisFont);
        FontMetrics fm = g2.getFontMetrics();

        // 标注频率：每 5 格标一次（最小 1 格）
        int step = Math.max(1, 5);
        // 如果格子很大，则每格都标
        if (cellSize >= 26) step = 1;
        else if (cellSize >= 18) step = 2;

        // X 轴标签 — 绘制在底部坐标轴背景正中
        int yBottomLine = gridToScreenY(0) + cellSize;        // 网格底部线
        int yBottomText = yBottomLine + AXIS_MARGIN / 2 + fm.getAscent() / 2 - 2;
        for (int x = 0; x < w; x += step) {
            int sx = gridToScreenX(x) + cellSize / 2;
            String label = String.valueOf(x);
            int tx = sx - fm.stringWidth(label) / 2;
            g2.drawString(label, tx, yBottomText);
        }

        // Y 轴标签 — 绘制在左侧坐标轴背景正中
        int xLeftLine = gridToScreenX(0);                      // 网格左侧线
        for (int y = 0; y < h; y += step) {
            int sy = gridToScreenY(y) + cellSize / 2 + fm.getAscent() / 2 - 1;
            String label = String.valueOf(y);
            int labelW = fm.stringWidth(label);
            g2.drawString(label, xLeftLine - AXIS_MARGIN / 2 - labelW / 2, sy);
        }

        // 轴方向箭头
        g2.setFont(new Font("SansSerif", Font.BOLD, 11));
        g2.drawString("x→", gridToScreenX(w - 1) + cellSize + 4, yBottomText);
        g2.drawString("y↑", xLeftLine - AXIS_MARGIN / 2 - 10, gridToScreenY(h - 1) - 5);
    }

    private void drawHoverHighlight(Graphics2D g2, Position cell) {
        int sx = gridToScreenX(cell.getX());
        int sy = gridToScreenY(cell.getY());
        g2.setColor(new Color(255, 255, 255, 100));
        g2.fillRect(sx, sy, cellSize, cellSize);
        g2.setColor(new Color(255, 255, 0, 200));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRect(sx, sy, cellSize, cellSize);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawTerrain(Graphics2D g2, int w, int h) {
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int sx = gridToScreenX(x);
                int sy = gridToScreenY(y);
                char terrain = sim.getTerrainChar(x, y);
                Color c = getTerrainColor(terrain);
                g2.setColor(c);
                g2.fillRect(sx, sy, cellSize, cellSize);
            }
        }
    }

    private Color getTerrainColor(char code) {
        return switch (code) {
            case '0' -> COLOR_EMPTY;
            case '1' -> COLOR_STATION;
            case '2', '3', '4', '5' -> COLOR_EMPTY; // 矿工/战士/火箭/医疗（地图上的初始单位位）
            case '6' -> COLOR_GUARDIAN;               // 守护者（中立单位）
            case '7' -> COLOR_GOLDMINE;
            case '8' -> COLOR_GEMMINE;
            case '9' -> COLOR_TREE;
            case 'a' -> COLOR_MOUNTAIN;
            default -> COLOR_EMPTY;
        };
    }

    private void drawThreatMap(Graphics2D g2, int w, int h) {
        double maxDanger = sim.getMaxDanger();
        if (maxDanger <= 0) return;

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                double danger = sim.getDanger(x, y);
                if (danger <= 0) continue;

                double ratio = Math.min(danger / maxDanger, 1.0);
                int alpha = (int)(50 + ratio * 150);
                Color heatColor;
                if (ratio < 0.33) {
                    heatColor = new Color(255, 255, 0, alpha);
                } else if (ratio < 0.66) {
                    heatColor = new Color(255, 165, 0, alpha);
                } else {
                    heatColor = new Color(220, 20, 20, alpha);
                }

                int sx = gridToScreenX(x);
                int sy = gridToScreenY(y);
                g2.setColor(heatColor);
                g2.fillRect(sx, sy, cellSize, cellSize);

                if (cellSize >= 20 && danger > 0.5) {
                    g2.setColor(new Color(0, 0, 0, 120));
                    g2.setFont(new Font("SansSerif", Font.PLAIN, 9));
                    String label = String.format("%.0f", danger);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = sx + (cellSize - fm.stringWidth(label)) / 2;
                    int ty = sy + (cellSize + fm.getAscent()) / 2 - 2;
                    g2.drawString(label, tx, ty);
                }
            }
        }
    }

    private void drawVisitedNodes(Graphics2D g2) {
        Set<Position> visited = sim.getVisitedPositions();
        if (visited == null || visited.isEmpty()) return;

        g2.setColor(COLOR_VISITED);
        for (Position p : visited) {
            int sx = gridToScreenX(p.getX());
            int sy = gridToScreenY(p.getY());
            g2.fillRect(sx + 1, sy + 1, cellSize - 2, cellSize - 2);
        }
    }

    private void drawReservations(Graphics2D g2, int w, int h) {
        int step = sim.getCurrentStep() + 1;
        g2.setColor(COLOR_RESERVATION);
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int resId = sim.getReservation(step, x, y);
                if (resId != -1) {
                    int sx = gridToScreenX(x);
                    int sy = gridToScreenY(y);
                    g2.fillRect(sx + 2, sy + 2, cellSize - 4, cellSize - 4);
                    if (cellSize >= 20) {
                        g2.setColor(Color.BLACK);
                        g2.setFont(new Font("SansSerif", Font.BOLD, 10));
                        String label = String.valueOf(step);
                        FontMetrics fm = g2.getFontMetrics();
                        g2.drawString(label, sx + (cellSize - fm.stringWidth(label)) / 2,
                                sy + (cellSize + fm.getAscent()) / 2);
                        g2.setColor(COLOR_RESERVATION);
                    }
                }
            }
        }
    }

    private void drawPaths(Graphics2D g2) {
        int colorIdx = 0;
        for (GameUnit u : sim.getMyUnits()) {
            List<Position> path = sim.getUnitPath(u.getId());
            if (path.isEmpty()) continue;

            Color pathColor = PATH_COLORS[colorIdx % PATH_COLORS.length];
            colorIdx++;

            Position start = u.getPos();
            Position prev = start;
            int curStep = sim.getCurrentStep();

            g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(pathColor);

            for (int i = 0; i < path.size(); i++) {
                Position cur = path.get(i);
                int x1 = gridToScreenX(prev.getX()) + cellSize / 2;
                int y1 = gridToScreenY(prev.getY()) + cellSize / 2;
                int x2 = gridToScreenX(cur.getX()) + cellSize / 2;
                int y2 = gridToScreenY(cur.getY()) + cellSize / 2;

                if (i < curStep) {
                    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                } else {
                    g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                g2.drawLine(x1, y1, x2, y2);
                drawArrow(g2, x1, y1, x2, y2, pathColor);
                prev = cur;
            }
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color c) {
        double angle = Math.atan2(y2 - y1, x2 - x1);
        int arrowSize = Math.max(6, cellSize / 3);
        int midX = (x1 + x2) / 2;
        int midY = (y1 + y2) / 2;
        int ax = midX - (int)(arrowSize * Math.cos(angle - Math.PI / 6));
        int ay = midY - (int)(arrowSize * Math.sin(angle - Math.PI / 6));
        int bx = midX - (int)(arrowSize * Math.cos(angle + Math.PI / 6));
        int by = midY - (int)(arrowSize * Math.sin(angle + Math.PI / 6));
        g2.setColor(c);
        g2.fillPolygon(new int[]{midX, ax, bx}, new int[]{midY, ay, by}, 3);
    }

    private void drawGrid(Graphics2D g2, int w, int h) {
        g2.setColor(COLOR_GRID);
        g2.setStroke(new BasicStroke(0.5f));
        for (int x = 0; x <= w; x++) {
            int sx = gridToScreenX(x);
            g2.drawLine(sx, gridToScreenY(h - 1), sx, gridToScreenY(0) + cellSize);
        }
        for (int y = 0; y <= h; y++) {
            int sy = gridToScreenY(y);
            g2.drawLine(gridToScreenX(0), sy, gridToScreenX(w - 1) + cellSize, sy);
        }
    }

    private void drawUnits(Graphics2D g2) {
        if (sim.hasPlan() && sim.getCurrentStep() > 0) {
            Map<Integer, Position> snapshot = sim.getCurrentSnapshot();
            for (GameUnit u : sim.getMyUnits()) {
                Position pos = snapshot.getOrDefault(u.getId(), u.getPos());
                drawUnit(g2, u, pos.getX(), pos.getY(), true);
            }
        } else {
            for (GameUnit u : sim.getMyUnits()) {
                drawUnit(g2, u, u.getPos().getX(), u.getPos().getY(), true);
            }
        }
        for (GameUnit u : sim.getEnemyUnits()) {
            drawUnit(g2, u, u.getPos().getX(), u.getPos().getY(), false);
        }
    }

    private void drawUnit(Graphics2D g2, GameUnit u, int gx, int gy, boolean isMine) {
        int sx = gridToScreenX(gx);
        int sy = gridToScreenY(gy);
        int margin = Math.max(2, cellSize / 10);
        int size = cellSize - 2 * margin;

        Color fillColor;
        if (u.type == UnitType.GUARDIAN) {
            fillColor = COLOR_GUARDIAN;
        } else {
            fillColor = isMine ? COLOR_MY_UNIT : COLOR_ENEMY_UNIT;
        }

        g2.setColor(fillColor);
        g2.fillOval(sx + margin, sy + margin, size, size);

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(sx + margin, sy + margin, size, size);

        if (cellSize >= 16) {
            String label = getUnitLabel(u.type);
            g2.setColor(Color.WHITE);
            int fontSize = Math.max(9, cellSize / 2);
            g2.setFont(new Font("SansSerif", Font.BOLD, fontSize));
            FontMetrics fm = g2.getFontMetrics();
            int tx = sx + (cellSize - fm.stringWidth(label)) / 2;
            int ty = sy + (cellSize + fm.getAscent()) / 2 - 1;
            g2.drawString(label, tx, ty);
        }
    }

    private String getUnitLabel(UnitType type) {
        return switch (type) {
            case MINER -> "M";
            case FIGHTER -> "F";
            case ROCKET -> "R";
            case MEDIC -> "D";
            case GUARDIAN -> "G";
        };
    }

    private void drawTarget(Graphics2D g2) {
        Position t = sim.getTarget();
        int cx = gridToScreenX(t.getX()) + cellSize / 2;
        int cy = gridToScreenY(t.getY()) + cellSize / 2;
        int r = cellSize / 2;

        g2.setColor(COLOR_TARGET);
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(cx - r, cy, cx + r, cy);
        g2.drawLine(cx, cy - r, cx, cy + r);
        g2.drawOval(cx - r, cy - r, r * 2, r * 2);
        g2.setStroke(new BasicStroke(1f));
    }

    public void resetView() {
        panX = 0;
        panY = 0;
        fitCellSize();
        repaint();
    }

    /** 根据可视区域自动计算合适的 cellSize，确保地图 + 坐标轴完整可见 */
    private void fitCellSize() {
        if (sim == null || sim.getWorld() == null) {
            cellSize = 24;
            return;
        }
        int w = sim.getWorld().getWidth();
        int h = sim.getWorld().getHeight();

        // 获取可视区域大小（JScrollPane 的 viewport 或面板自身）
        Container parent = getParent();
        int viewW, viewH;
        if (parent instanceof JViewport vp) {
            viewW = vp.getExtentSize().width;
            viewH = vp.getExtentSize().height;
        } else {
            viewW = getWidth();
            viewH = getHeight();
        }

        // 减去坐标轴边距
        int availW = viewW - AXIS_MARGIN - 4;
        int availH = viewH - AXIS_MARGIN - 4;

        if (availW <= 0 || availH <= 0) {
            cellSize = 24;
            return;
        }

        int fitW = availW / w;
        int fitH = availH / h;
        cellSize = Math.max(8, Math.min(48, Math.min(fitW, fitH)));

        // 更新面板 preferred size，确保 ScrollPane 滚动条覆盖坐标轴区域
        updatePreferredSize(w, h);
    }

    /** 根据地图大小 + 坐标轴边距更新面板的 preferred size */
    private void updatePreferredSize(int w, int h) {
        int pw = w * cellSize + AXIS_MARGIN + 40; // +40 给右侧留空
        int ph = h * cellSize + AXIS_MARGIN + 40;
        setPreferredSize(new Dimension(pw, ph));
        revalidate();
    }

    public Position getHoveredCell() { return hoveredCell; }
    public MovementSimulator getSim() { return sim; }
}
