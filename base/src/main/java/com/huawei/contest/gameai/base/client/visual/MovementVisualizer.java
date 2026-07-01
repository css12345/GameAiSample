package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.entity.Position;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Map;

/**
 * 移动系统可视化测试工具 — 主窗口入口。
 */
public class MovementVisualizer extends JFrame {

    private final MovementSimulator sim;
    private final MapRenderPanel renderPanel;
    private final ControlPanel controlPanel;
    private final JLabel statusLabel;
    private final JLabel coordLabel;

    public MovementVisualizer() {
        super("移动系统可视化测试工具 — MovementCoordinator Visualizer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        sim = new MovementSimulator();
        renderPanel = new MapRenderPanel();
        renderPanel.setSimulator(sim);

        controlPanel = new ControlPanel(sim, renderPanel);
        controlPanel.setOnPlanNeeded(this::planAndRefresh);
        renderPanel.setOnSelectionChange(() -> {
            controlPanel.updateSelectedUnitLabel();
            controlPanel.refreshSquadList();
            controlPanel.refreshSquadInfo();
        });

        // 状态栏：坐标信息 + 状态
        coordLabel = new JLabel("x=— y=—");
        coordLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 16));
        statusLabel = new JLabel("就绪 — 请选择地图并放置单位");
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        JPanel statusBar = new JPanel(new BorderLayout());
        statusBar.add(statusLabel, BorderLayout.CENTER);
        statusBar.add(coordLabel, BorderLayout.EAST);

        // 悬停坐标回调
        renderPanel.setOnHoverCallback((x, y) -> {
            coordLabel.setText("x=" + x + " y=" + y);
            StringBuilder sb = new StringBuilder();
            sb.append("x=").append(x).append(" y=").append(y);
            sb.append("  地形=").append(describeTerrain(sim.getTerrainChar(x, y)));
            double danger = sim.getDanger(x, y);
            if (danger > 0) sb.append("  威胁=").append(String.format("%.1f", danger));
            Position globalTarget = sim.getTarget();
            if (globalTarget != null && globalTarget.getX() == x && globalTarget.getY() == y) {
                sb.append("  【全局目标】");
            }
            for (var s : sim.getSquads()) {
                if (s.target() != null && s.target().getX() == x && s.target().getY() == y) {
                    sb.append("  【编队").append(s.id()).append("目标】");
                }
            }
            // 显示编队信息
            for (var u : sim.getMyUnits()) {
                if (u.getPos().getX() == x && u.getPos().getY() == y) {
                    int sid = sim.getUnitSquad(u.getId());
                    if (sid > 0) sb.append("  [编队").append(sid).append("]");
                    break;
                }
            }
            coordLabel.setText(sb.toString());
        });

        // 布局
        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(renderPanel);
        scrollPane.getVerticalScrollBar().setUnitIncrement(24);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(24);
        add(scrollPane, BorderLayout.CENTER);

        // 右侧: 控制面板 + 图例（包裹在滚动面板中，内容多时可滚动）
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.add(controlPanel);
        rightPanel.add(createLegendPanel());

        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setPreferredSize(new Dimension(290, 600));
        rightScroll.setMinimumSize(new Dimension(290, 400));
        rightScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        rightScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        rightScroll.getVerticalScrollBar().setUnitIncrement(16);
        // 让内部面板随滚动条宽度自适应，避免内容被裁剪
        rightPanel.setAutoscrolls(true);
        add(rightScroll, BorderLayout.EAST);

        add(statusBar, BorderLayout.SOUTH);
        setJMenuBar(createMenuBar());

        setSize(1280, 800);
        setLocationRelativeTo(null);
    }

    /** 创建图例面板 */
    private JPanel createLegendPanel() {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder("图例"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setAlignmentX(LEFT_ALIGNMENT);

        // 地形
        panel.add(legendTitle("══ 地形 ══"));
        panel.add(legendItem("   空地", new Color(0xF0, 0xF0, 0xF0)));
        panel.add(legendItem("   树木", new Color(0x22, 0x8B, 0x22)));
        panel.add(legendItem("   高山", new Color(0x69, 0x69, 0x69)));
        panel.add(legendItem("   金矿(障碍)", new Color(0xFF, 0xD7, 0x00)));
        panel.add(legendItem("   宝石矿(障碍)", new Color(0x41, 0x69, 0xE1)));
        panel.add(legendItem("   基地", new Color(0x00, 0xCD, 0xCD)));
        panel.add(legendItem("   守护者(障碍)", new Color(0x8B, 0x00, 0x8B)));

        // 单位
        panel.add(legendTitle("══ 单位 ══"));
        panel.add(legendItem("   己方单位", new Color(0x1E, 0x90, 0xFF)));
        panel.add(legendItem("   敌方单位", new Color(0xDC, 0x14, 0x3C)));
        panel.add(legendItem("   守护者(中立)", new Color(0x8B, 0x00, 0x8B)));

        // 移动规划
        panel.add(legendTitle("══ 移动规划 ══"));
        panel.add(legendItem("   威胁热力(低→高)", new Color(0xFF, 0xFF, 0x00)));
        panel.add(legendItemSmall("   (低=黄 → 中=橙 → 高=红)"));
        panel.add(legendItem("   A*探索节点", new Color(0xFF, 0xD7, 0x00, 0x60)));
        panel.add(legendItem("   预留表标记", new Color(0x00, 0x99, 0xCC, 0x80)));
        panel.add(legendItem("   路径线", new Color(0x00, 0x99, 0x00)));
        panel.add(legendItem("   目标点", new Color(0xFF, 0x45, 0x00)));
        panel.add(legendItemSmall("   路径颜色: 每单位一种颜色"));

        // 单位字母
        panel.add(legendTitle("══ 单位字母 ══"));
        panel.add(legendItemSmall("   M=矿工 F=战士 R=火箭兵"));
        panel.add(legendItemSmall("   D=医疗兵 G=守护者"));

        // 操作提示
        panel.add(legendTitle("══ 操作 ══"));
        panel.add(legendItemSmall("   左键=放单位 右键=设目标"));
        panel.add(legendItemSmall("   Shift+拖拽=平移 滚轮=缩放"));

        return panel;
    }

    private JLabel legendTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setBorder(new EmptyBorder(6, 4, 2, 4));
        l.setForeground(new Color(0x55, 0x55, 0x55));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    private JLabel legendItem(String text, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        l.setBorder(new EmptyBorder(1, 4, 1, 4));
        l.setIcon(new ColorIcon(color, 14, 14));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    private JLabel legendItemSmall(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 10));
        l.setForeground(new Color(0x77, 0x77, 0x77));
        l.setBorder(new EmptyBorder(0, 20, 0, 4));
        l.setAlignmentX(LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, l.getPreferredSize().height));
        return l;
    }

    /** 简单的颜色方块图标 */
    private record ColorIcon(Color color, int w, int h) implements Icon {
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillRect(x + 1, y + 1, w - 2, h - 2);
            g.setColor(Color.BLACK);
            g.drawRect(x + 1, y + 1, w - 2, h - 2);
        }
        @Override public int getIconWidth() { return w; }
        @Override public int getIconHeight() { return h; }
    }

    private static String describeTerrain(char code) {
        return switch (code) {
            case '0' -> "空地";
            case '1' -> "基地";
            case '2' -> "矿工位";
            case '3' -> "战士位";
            case '4' -> "火箭位";
            case '5' -> "医疗位";
            case '6' -> "守护者";
            case '7' -> "金矿";
            case '8' -> "宝石矿";
            case '9' -> "树木";
            case 'a' -> "高山";
            default -> "未知" + code;
        };
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        // 文件菜单 — 动态加载地图列表
        JMenu fileMenu = new JMenu("地图");
        Map<String, MapDataLoader.MapInfo> maps = MapDataLoader.getMaps();
        for (Map.Entry<String, MapDataLoader.MapInfo> e : maps.entrySet()) {
            String name = e.getKey();
            MapDataLoader.MapInfo info = e.getValue();
            JMenuItem item = new JMenuItem(name + "  (" + info.width() + "×" + info.height() + ")");
            item.addActionListener(ae -> loadAndRefresh(name));
            fileMenu.add(item);
        }
        fileMenu.addSeparator();
        JMenuItem reloadItem = new JMenuItem("重新扫描地图目录");
        reloadItem.addActionListener(e -> {
            MapDataLoader.reload();
            setJMenuBar(createMenuBar()); // 重建菜单
            statusLabel.setText("已重新扫描地图目录，共 " + MapDataLoader.getMaps().size() + " 个地图");
        });
        fileMenu.add(reloadItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("退出");
        exitItem.addActionListener(e -> dispose());
        fileMenu.add(exitItem);
        mb.add(fileMenu);

        JMenu viewMenu = new JMenu("视图");
        JMenuItem resetViewItem = new JMenuItem("重置视图 (缩放/平移)");
        resetViewItem.addActionListener(e -> renderPanel.resetView());
        viewMenu.add(resetViewItem);

        JCheckBoxMenuItem coordItem = new JCheckBoxMenuItem("显示坐标轴", true);
        coordItem.addActionListener(e -> renderPanel.setShowCoords(coordItem.isSelected()));
        viewMenu.add(coordItem);

        mb.add(viewMenu);

        return mb;
    }

    private void loadAndRefresh(String mapName) {
        try {
            sim.loadMap(mapName);
            // 延迟调用 fitCellSize，确保布局已完成
            SwingUtilities.invokeLater(() -> {
                renderPanel.resetView();
                renderPanel.repaint();
                controlPanel.updateStepLabel();
            });
            statusLabel.setText("已加载地图: " + mapName + " ("
                    + sim.getWorld().getWidth() + "×" + sim.getWorld().getHeight() + ")");
        } catch (Exception ex) {
            statusLabel.setText("加载失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void planAndRefresh() {
        try {
            if (sim.getWorld() == null) {
                statusLabel.setText("请先加载地图");
                return;
            }
            if (sim.getMyUnits().isEmpty()) {
                statusLabel.setText("请先在地图上放置己方单位（放置模式左键点击）");
                return;
            }
            // 有全局目标 或 任意编队有目标 才能规划
            boolean hasGlobalTarget = sim.getTarget() != null;
            boolean hasSquadTarget = sim.getSquads().stream().anyMatch(s -> s.target() != null);
            if (!hasGlobalTarget && !hasSquadTarget) {
                statusLabel.setText("请先设置目标：选中编队(或全局)→右键地图");
                return;
            }
            statusLabel.setText("正在计算路径...");
            controlPanel.stopAutoPlay();
            sim.planMovement();
            renderPanel.repaint();
            controlPanel.updateStepLabel();
            controlPanel.refreshSquadList();
            controlPanel.refreshSquadInfo();
            statusLabel.setText("路径计算完成 — " + sim.getMyUnits().size()
                    + " 个单位, 最长 " + sim.getMaxPathLen() + " 步");
        } catch (Exception ex) {
            statusLabel.setText("计算失败: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            MovementVisualizer frame = new MovementVisualizer();
            frame.setVisible(true);
        });
    }
}
