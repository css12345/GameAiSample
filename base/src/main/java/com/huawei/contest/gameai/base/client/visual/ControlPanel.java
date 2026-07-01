package com.huawei.contest.gameai.base.client.visual;

import com.huawei.contest.gameai.base.client.entity.Position;
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
        // 只约束最大宽度，高度由内容自然决定（让 JScrollPane 正确滚动）
        setMaximumSize(new Dimension(290, Integer.MAX_VALUE));
        setAlignmentX(LEFT_ALIGNMENT);

        addMapSection();
        addUnitSection();
        addSquadSection();
        addConfigSection();
        addLayerSection();
        addStepSection();

        // 防止 BoxLayout 在 JScrollPane 中拉伸/挤压各 section：
        // 每个 section 限高为自身自然高度，宽度不限
        for (Component c : getComponents()) {
            if (c instanceof JComponent jc) {
                Dimension pref = jc.getPreferredSize();
                jc.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            }
        }

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

    private JRadioButton placeModeBtn;
    private JRadioButton selectModeBtn;
    private JLabel selectedUnitLabel;

    private void addUnitSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("单位放置"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 模式切换：放置 / 选择
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        placeModeBtn = new JRadioButton("放置", true);
        selectModeBtn = new JRadioButton("选择");
        ButtonGroup modeGroup = new ButtonGroup();
        modeGroup.add(placeModeBtn);
        modeGroup.add(selectModeBtn);
        placeModeBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        selectModeBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        placeModeBtn.addActionListener(e -> { renderPanel.setPlaceMode(true); updateSelectedUnitLabel(); });
        selectModeBtn.addActionListener(e -> { renderPanel.setPlaceMode(false); updateSelectedUnitLabel(); });
        modePanel.add(placeModeBtn);
        modePanel.add(selectModeBtn);
        panel.add(modePanel);

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
                    renderPanel.setPlacingEnemy(true);
                }
            });
            if (i == 1) btn.setSelected(true);
            typeGroup.add(btn);
            typePanel.add(btn);
        }
        panel.add(typePanel);

        // 己方/敌方切换
        JPanel sidePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JCheckBox enemyCheck = new JCheckBox("敌方");
        enemyCheck.setFont(new Font("SansSerif", Font.PLAIN, 11));
        enemyCheck.addActionListener(e -> renderPanel.setPlacingEnemy(enemyCheck.isSelected()));
        sidePanel.add(enemyCheck);

        JButton clearBtn = new JButton("清空己方");
        clearBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
        clearBtn.setMargin(new Insets(2, 4, 2, 4));
        clearBtn.addActionListener(e -> {
            for (var u : sim.getMyUnits().toArray(new com.huawei.contest.gameai.base.client.entity.GameUnit[0])) {
                sim.removeMyUnit(u.getId());
            }
            renderPanel.repaint();
        });
        sidePanel.add(clearBtn);
        panel.add(sidePanel);

        // 选中单位信息
        selectedUnitLabel = new JLabel("未选中单位");
        selectedUnitLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        selectedUnitLabel.setForeground(new Color(0x33, 0x66, 0x99));
        panel.add(selectedUnitLabel);

        // 提示标签
        JLabel hint = new JLabel("<html><small>放置模式:左键放单位<br>选择模式:左键选单位<br>右键=设目标 滚轮=缩放</small></html>");
        hint.setForeground(Color.GRAY);
        panel.add(hint);

        add(panel);
    }

    void updateSelectedUnitLabel() {
        var u = sim.getSelectedUnit();
        if (u != null) {
            int sid = sim.getUnitSquad(u.getId());
            String sq = sid > 0 ? " 编队" + sid : " 未编队";
            selectedUnitLabel.setText(String.format("选中: %s id=%d (%d,%d)%s",
                    u.type.name(), u.getId(), u.getPos().getX(), u.getPos().getY(), sq));
        } else {
            selectedUnitLabel.setText("未选中单位");
        }
    }

    // ==================== 编队管理 ====================

    private JLabel squadInfoLabel;
    private JLabel targetModeLabel;

    private void addSquadSection() {
        JPanel panel = new JPanel();
        panel.setBorder(new TitledBorder("编队"));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // 当前选中编队
        squadInfoLabel = new JLabel("当前: 无编队(全局)");
        squadInfoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        squadInfoLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(squadInfoLabel);

        // 右键目标模式提示（醒目）
        targetModeLabel = new JLabel("右键=设置全局目标");
        targetModeLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        targetModeLabel.setForeground(new Color(0xCC, 0x44, 0x00));
        targetModeLabel.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(targetModeLabel);

        // 创建/选择编队按钮
        JPanel selPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        JButton createBtn = new JButton("+编队");
        createBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        createBtn.setMargin(new Insets(1, 4, 1, 4));
        createBtn.addActionListener(e -> {
            int id = sim.createSquad("编队" + (sim.getSquads().size() + 1)).id();
            sim.setSelectedSquadId(id);
            refreshSquadList();
        });
        selPanel.add(createBtn);

        JButton delBtn = new JButton("删除编队");
        delBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        delBtn.setMargin(new Insets(1, 4, 1, 4));
        delBtn.addActionListener(e -> {
            int sid = sim.getSelectedSquadId();
            if (sid > 0) { sim.removeSquad(sid); sim.setSelectedSquadId(0); refreshSquadList(); renderPanel.repaint(); }
        });
        selPanel.add(delBtn);
        panel.add(selPanel);

        // 编队列表（带颜色 + 成员数 + 目标，点击选中）
        squadListModel = new DefaultListModel<>();
        squadList = new JList<>(squadListModel);
        squadList.setFont(new Font("SansSerif", Font.PLAIN, 11));
        squadList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        squadList.setFixedCellHeight(20);
        squadList.setCellRenderer(new SquadListCellRenderer());
        squadList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || refreshing) return;
            int idx = squadList.getSelectedIndex();
            if (idx >= 0 && idx < cachedSquadIds.size()) {
                sim.setSelectedSquadId(cachedSquadIds.get(idx));
                renderPanel.repaint();
                refreshSquadInfo();
            }
        });
        JScrollPane listScroll = new JScrollPane(squadList);
        listScroll.setPreferredSize(new Dimension(260, 100));
        listScroll.setAlignmentX(LEFT_ALIGNMENT);
        panel.add(listScroll);

        // 分配选中单位到当前编队
        JPanel assignPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 2));
        JButton assignBtn = new JButton("分配选中单位→编队");
        assignBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        assignBtn.setMargin(new Insets(1, 4, 1, 4));
        assignBtn.addActionListener(e -> {
            int sid = sim.getSelectedSquadId();
            int uid = sim.getSelectedUnitId();
            if (sid > 0 && uid >= 0) {
                sim.assignToSquad(uid, sid);
                refreshSquadList();
                updateSelectedUnitLabel();
                renderPanel.repaint();
            }
        });
        assignPanel.add(assignBtn);

        JButton removeBtn = new JButton("移出编队");
        removeBtn.setFont(new Font("SansSerif", Font.PLAIN, 10));
        removeBtn.setMargin(new Insets(1, 4, 1, 4));
        removeBtn.addActionListener(e -> {
            int uid = sim.getSelectedUnitId();
            if (uid >= 0) {
                sim.assignToSquad(uid, 0);
                refreshSquadList();
                updateSelectedUnitLabel();
                renderPanel.repaint();
            }
        });
        assignPanel.add(removeBtn);
        panel.add(assignPanel);

        JLabel hint2 = new JLabel("<html><small>①选择模式点单位 ②点编队 ③点\"分配\"<br>④选中编队→右键地图设目标<br>⑤点\"重新计算路径\"移动</small></html>");
        hint2.setForeground(Color.GRAY);
        panel.add(hint2);

        add(panel);
    }

    private DefaultListModel<String> squadListModel;
    private JList<String> squadList;
    private final java.util.List<Integer> cachedSquadIds = new java.util.ArrayList<>();
    /** 防重入标志：刷新列表时避免触发 ListSelectionListener 回调 */
    private boolean refreshing = false;

    /** 刷新编队列表显示，保留选中 */
    void refreshSquadList() {
        refreshing = true;
        try {
            int selected = sim.getSelectedSquadId();
            squadListModel.clear();
            cachedSquadIds.clear();
            for (var s : sim.getSquads()) {
            int count = sim.getSquadMembers(s.id()).size();
            String targetStr = s.target() != null
                    ? String.format("→(%d,%d)", s.target().getX(), s.target().getY())
                    : "无目标";
            squadListModel.addElement(String.format("编队%d %s  %d人 %s",
                    s.id(), s.name(), count, targetStr));
            cachedSquadIds.add(s.id());
        }
        // 选中当前编队
        for (int i = 0; i < cachedSquadIds.size(); i++) {
            if (cachedSquadIds.get(i) == selected) {
                squadList.setSelectedIndex(i);
                break;
            }
        }
        // 更新标签
        refreshSquadInfo();
        } finally {
            refreshing = false;
        }
    }

    /** 只更新编队信息标签 + 右键模式提示（不重建列表） */
    void refreshSquadInfo() {
        int selected = sim.getSelectedSquadId();
        if (selected > 0) {
            var s = sim.getSquad(selected);
            if (s != null) {
                squadInfoLabel.setText("当前: 编队" + selected + " \"" + s.name() + "\"");
                String t = s.target() != null
                        ? String.format("→(%d,%d)", s.target().getX(), s.target().getY())
                        : "未设目标";
                targetModeLabel.setText("右键=设置 编队" + selected + " 目标 (现" + t + ")");
            }
        } else {
            squadInfoLabel.setText("当前: 无编队(全局)");
            String t = sim.getTarget() != null
                    ? String.format("→(%d,%d)", sim.getTarget().getX(), sim.getTarget().getY())
                    : "未设目标";
            targetModeLabel.setText("右键=设置全局目标 (现" + t + ")");
        }
    }

    /** 编队列表渲染器：用编队颜色做左侧色块 */
    private class SquadListCellRenderer extends JPanel implements ListCellRenderer<String> {
        private Color squadColor = Color.GRAY;
        private String text = "";
        private boolean selected = false;

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value,
                                                     int index, boolean isSelected, boolean cellHasFocus) {
            text = value;
            selected = isSelected;
            if (index >= 0 && index < cachedSquadIds.size()) {
                int sid = cachedSquadIds.get(index);
                var s = sim.getSquad(sid);
                if (s != null) squadColor = s.color();
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(selected ? new Color(0xCC, 0xDD, 0xFF) : Color.WHITE);
            g.fillRect(0, 0, getWidth(), getHeight());
            // 左侧色块
            g.setColor(squadColor);
            g.fillRect(2, 2, 14, getHeight() - 4);
            g.setColor(Color.BLACK);
            g.drawRect(2, 2, 14, getHeight() - 4);
            // 文字
            g.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g.drawString(text, 22, getHeight() / 2 + 4);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(240, 18);
        }
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
