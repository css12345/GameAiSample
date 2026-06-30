package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.entity.UnitType;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.Map;

/**
 * 控制面板 — 地图选择、单位放置、参数配置、步进控制。
 */
public class ControlPanel extends JPanel {

    private MovementSimulator sim;
    private MapRenderPanel renderPanel;
    private JLabel stepLabel;
    private JSlider speedSlider;
    private Timer autoPlayTimer;

    // 引用到主窗口的回调
    private Runnable onPlanNeeded;

    public ControlPanel(MovementSimulator sim, MapRenderPanel renderPanel) {
        this.sim = sim;
        this.renderPanel = renderPanel;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(260, 600));

        addMapSection();
        addUnitSection();
        addConfigSection();
        addLayerSection();
        addStepSection();

        // 自动播放定时器
        autoPlayTimer = new Timer(500, e -> {
            if (sim.getCurrentStep() < sim.getMaxPathLen()) {
                sim.stepForward();
                renderPanel.repaint();
                updateStepLabel();
            } else {
                autoPlayTimer.stop();
            }
        });
    }

    public void setOnPlanNeeded(Runnable r) { this.onPlanNeeded = r; }

    // ==================== 地图选择 ====================

    private void addMapSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("地图"));
        panel.setLayout(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> mapCombo = new JComboBox<>();
        for (Map.Entry<String, MapDataLoader.MapInfo> e : MapDataLoader.getMaps().entrySet()) {
            mapCombo.addItem(e.getKey() + " (" + e.getValue().width() + "x" + e.getValue().height() + ")");
        }
        mapCombo.setMaximumSize(new Dimension(240, 25));
        mapCombo.setAlignmentX(LEFT_ALIGNMENT);

        JButton loadBtn = new JButton("加载地图");
        loadBtn.addActionListener(e -> {
            int idx = mapCombo.getSelectedIndex();
            String mapName = MapDataLoader.getMaps().keySet().toArray(new String[0])[idx];
            sim.loadMap(mapName);
            renderPanel.resetView();
            renderPanel.repaint();
            updateStepLabel();
        });

        panel.add(mapCombo);
        panel.add(loadBtn);
        add(panel);
    }

    // ==================== 单位放置 ====================

    private void addUnitSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("单位放置"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 单位类型按钮组
        JPanel typePanel = new JPanel(new GridLayout(3, 2, 4, 4));
        ButtonGroup typeGroup = new ButtonGroup();
        UnitType[] types = {UnitType.MINER, UnitType.FIGHTER, UnitType.ROCKET, UnitType.MEDIC, UnitType.GUARDIAN};
        String[] labels = {"矿工 M", "战士 F", "火箭 R", "医疗 D", "守护 G"};

        for (int i = 0; i < types.length; i++) {
            JToggleButton btn = new JToggleButton(labels[i]);
            btn.setFont(new Font("SansSerif", Font.PLAIN, 11));
            btn.setMargin(new Insets(2, 4, 2, 4));
            UnitType type = types[i];
            btn.addActionListener(e -> {
                renderPanel.setSelectedUnitType(type);
                if (type == UnitType.GUARDIAN) {
                    renderPanel.setPlacingEnemy(true); // 守护者是中立的
                }
            });
            if (i == 1) btn.setSelected(true); // 默认选战士
            typeGroup.add(btn);
            typePanel.add(btn);
        }
        panel.add(typePanel);

        // 己方/敌方切换
        JPanel sidePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox enemyCheck = new JCheckBox("放置敌方单位");
        enemyCheck.addActionListener(e -> renderPanel.setPlacingEnemy(enemyCheck.isSelected()));
        sidePanel.add(enemyCheck);

        JButton clearBtn = new JButton("清除己方单位");
        clearBtn.addActionListener(e -> {
            for (var u : sim.getMyUnits().toArray(new com.huawei.contest.gameai.base.client.entity.GameUnit[0])) {
                sim.removeMyUnit(u.getId());
            }
            renderPanel.repaint();
        });
        sidePanel.add(clearBtn);

        panel.add(sidePanel);

        // 提示标签
        JLabel hint = new JLabel("<html><small>左键=放置 | 右键=设目标<br>Shift+左键=拖拽平移 | 滚轮=缩放</small></html>");
        hint.setForeground(Color.GRAY);
        panel.add(hint);

        add(panel);
    }

    // ==================== 参数配置 ====================

    private void addConfigSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("AI 参数"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 攻击性
        JPanel aggPanel = new JPanel(new BorderLayout());
        aggPanel.add(new JLabel("攻击性: "), BorderLayout.WEST);
        JSlider aggSlider = new JSlider(0, 100, 50);
        JLabel aggVal = new JLabel("0.50");
        aggSlider.addChangeListener(e -> {
            double v = aggSlider.getValue() / 100.0;
            aggVal.setText(String.format("%.2f", v));
            sim.setAggressiveness(v);
        });
        aggPanel.add(aggSlider, BorderLayout.CENTER);
        aggPanel.add(aggVal, BorderLayout.EAST);
        panel.add(aggPanel);

        JButton recalcBtn = new JButton("重新计算路径");
        recalcBtn.setAlignmentX(CENTER_ALIGNMENT);
        recalcBtn.setMaximumSize(new Dimension(200, 30));
        recalcBtn.addActionListener(e -> {
            if (onPlanNeeded != null) onPlanNeeded.run();
        });
        panel.add(Box.createVerticalStrut(5));
        panel.add(recalcBtn);

        add(panel);
    }

    // ==================== 显示层切换 ====================

    private void addLayerSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("显示层"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        JCheckBox threatCB = new JCheckBox("威胁热力图", true);
        threatCB.addActionListener(e -> renderPanel.setShowThreatMap(threatCB.isSelected()));
        panel.add(threatCB);

        JCheckBox resCB = new JCheckBox("预留表", true);
        resCB.addActionListener(e -> renderPanel.setShowReservations(resCB.isSelected()));
        panel.add(resCB);

        JCheckBox pathCB = new JCheckBox("路径线", true);
        pathCB.addActionListener(e -> renderPanel.setShowPaths(pathCB.isSelected()));
        panel.add(pathCB);

        JCheckBox visitedCB = new JCheckBox("A* 探索节点", false);
        visitedCB.addActionListener(e -> renderPanel.setShowVisitedNodes(visitedCB.isSelected()));
        panel.add(visitedCB);

        add(panel);
    }

    // ==================== 步进控制 ====================

    private void addStepSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("移动回放"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 步数标签
        stepLabel = new JLabel("步数: 0 / 0");
        stepLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(stepLabel);

        // 步进按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 2));

        JButton firstBtn = new JButton("|<");
        firstBtn.setToolTipText("回到起点");
        firstBtn.addActionListener(e -> { sim.stepTo(0); renderPanel.repaint(); updateStepLabel(); });

        JButton prevBtn = new JButton("<");
        prevBtn.setToolTipText("上一步");
        prevBtn.addActionListener(e -> { sim.stepBackward(); renderPanel.repaint(); updateStepLabel(); });

        JButton playBtn = new JButton("▶");
        playBtn.setToolTipText("自动播放");
        playBtn.addActionListener(e -> {
            if (autoPlayTimer.isRunning()) {
                autoPlayTimer.stop();
                playBtn.setText("▶");
            } else {
                if (sim.getCurrentStep() >= sim.getMaxPathLen()) sim.stepTo(0);
                autoPlayTimer.start();
                playBtn.setText("⏸");
            }
        });

        JButton nextBtn = new JButton(">");
        nextBtn.setToolTipText("下一步");
        nextBtn.addActionListener(e -> { sim.stepForward(); renderPanel.repaint(); updateStepLabel(); });

        JButton lastBtn = new JButton(">|");
        lastBtn.setToolTipText("到最后一步");
        lastBtn.addActionListener(e -> { sim.stepTo(sim.getMaxPathLen()); renderPanel.repaint(); updateStepLabel(); });

        btnPanel.add(firstBtn);
        btnPanel.add(prevBtn);
        btnPanel.add(playBtn);
        btnPanel.add(nextBtn);
        btnPanel.add(lastBtn);
        panel.add(btnPanel);

        // 播放速度
        JPanel speedPanel = new JPanel(new BorderLayout());
        speedPanel.add(new JLabel("速度: "), BorderLayout.WEST);
        speedSlider = new JSlider(1, 10, 5);
        speedSlider.addChangeListener(e -> {
            int speed = speedSlider.getValue();
            autoPlayTimer.setDelay(1100 - speed * 100); // 100ms ~ 1000ms
        });
        speedPanel.add(speedSlider, BorderLayout.CENTER);
        panel.add(speedPanel);

        add(panel);
    }

    // ==================== 公共方法 ====================

    public void updateStepLabel() {
        stepLabel.setText("步数: " + sim.getCurrentStep() + " / " + sim.getMaxPathLen());
        // 刷新按钮状态
        repaint();
    }

    public void stopAutoPlay() {
        if (autoPlayTimer.isRunning()) autoPlayTimer.stop();
    }
}
