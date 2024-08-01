import search.MapSearchProblem;
import search.SearchState;
import search.algorithms.CompressAStar;
import search.algorithms.HillClimbingWithClosedSet;
import search.algorithms.visual.VisualAStar;
import search.algorithms.visual.VisualGameMap;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class HCVsAStarVisualization extends JFrame {
    private static final int GRID_SIZE = 10;
    private static final int FRAME_SIZE = 800;

    private final AtomicInteger startSid = new AtomicInteger(-1);
    private final AtomicInteger goalSid = new AtomicInteger(-1);

    private final AtomicInteger count = new AtomicInteger();
    // Stores labels that are marked as red (will need to be reset so path is clearly visible)
    private final List<JLabel> redLabels = new ArrayList<>();
    // Stores the states set as current in AStar pathfinding
    private List<SearchState> currentStates;
    // Stores the states found as subgoals on the path
    private List<SearchState> currentSubgoals;
    // Stores the states expanded for each current state
    private List<List<SearchState>> statesExpandedPerMove;
    // Store visual map representation
    private JLabel[][] labelGrid;
    // Underlying map representation, manipulating it will change pathfinding
    private AtomicInteger[][] states;

    // AStar implementation that also stores statesExpandedPerMove and currentStates
    private VisualAStar aStar;

    private VisualGameMap map;

    private JButton forwardButton;
    private JButton backwardButton;

    public HCVsAStarVisualization() {
        setupFrame();
        setupGrid();
        setupControlPanel();
        this.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(HCVsAStarVisualization::new);
    }

    private void setupFrame() {
        this.setTitle("HCVsAStarVisualization");
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(FRAME_SIZE, FRAME_SIZE);
        this.setLayout(new BorderLayout());
    }

    private void setupGrid() {
        JPanel panel = new JPanel(new GridLayout(GRID_SIZE, GRID_SIZE));
        labelGrid = new JLabel[GRID_SIZE][GRID_SIZE];
        states = new AtomicInteger[GRID_SIZE][GRID_SIZE];

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                // Set all states to be empty (traversable) initially
                states[row][col] = new AtomicInteger(VisualGameMap.EMPTY_CHAR);
                JLabel label = createGridLabel();
                labelGrid[row][col] = label;
                panel.add(label);
            }
        }

        map = new VisualGameMap(states);
        aStar = new VisualAStar(new MapSearchProblem(map));

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        this.add(panel, BorderLayout.CENTER);
    }

    private JLabel createGridLabel() {
        JLabel label = new JLabel("");
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        return label;
    }

    private void setupControlPanel() {
        JPanel controlPanel = new JPanel(new FlowLayout());

        JButton resetButton = new JButton("Reset Grid");
        JButton resetPathButton = new JButton("Reset Path");
        JButton drawHCSubgoals = new JButton("Draw Subgoals");
        forwardButton = new JButton(">>");
        backwardButton = new JButton("<<");

        resetButton.addActionListener(e -> resetGrid());
        resetPathButton.addActionListener(e -> resetPath());
        drawHCSubgoals.addActionListener(e -> drawHCSubgoals());
        forwardButton.addActionListener(e -> moveForward());
        backwardButton.addActionListener(e -> moveBackward());

        controlPanel.add(backwardButton);
        controlPanel.add(resetButton);
        controlPanel.add(resetPathButton);
        controlPanel.add(drawHCSubgoals);
        controlPanel.add(forwardButton);

        this.add(controlPanel, BorderLayout.NORTH);
    }

    private void drawHCSubgoals() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (currentSubgoals.contains(new SearchState(map.getStateId(row, col)))) {
                    setLabel(row, col, Color.PINK);
                }
            }
        }
    }

    private void resetPath() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (states[row][col].get() != VisualGameMap.WALL_CHAR) {
                    setLabel(row, col, Color.WHITE, "");
                }
            }
        }

        startSid.set(-1);
        goalSid.set(-1);
        count.set(0);

        // Need to perform null check since these are only initialized if goal was set and path was found
        if (currentStates != null) currentStates.clear();
        if (statesExpandedPerMove != null) statesExpandedPerMove.clear();
        if (currentSubgoals != null) currentSubgoals.clear();

        redLabels.clear();
        forwardButton.setEnabled(true);
        backwardButton.setEnabled(true);
    }

    private void resetGrid() {
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                states[row][col].set(VisualGameMap.EMPTY_CHAR);
                setLabel(row, col, Color.WHITE, "");
            }
        }

        startSid.set(-1);
        goalSid.set(-1);
        count.set(0);

        // Need to perform null check since these are only initialized if goal was set and path was found
        if (currentStates != null) currentStates.clear();
        if (statesExpandedPerMove != null) statesExpandedPerMove.clear();
        if (currentSubgoals != null) currentSubgoals.clear();

        redLabels.clear();
        forwardButton.setEnabled(true);
        backwardButton.setEnabled(true);
    }

    private void moveForward() {
        if (count.get() < currentStates.size()) {
            count.incrementAndGet();

            for (JLabel colouredLabel : redLabels) {
                colouredLabel.setBackground(Color.YELLOW);
            }

            List<SearchState> statesExpanded = statesExpandedPerMove.get(count.get());
            for (SearchState state : statesExpanded) {
                int sid = state.getStateId();

                if (sid == startSid.get() || sid == goalSid.get()) {
                    continue;
                }

                int r = map.getRowFromStateId(sid);
                int c = map.getColFromStateId(sid);

                // Label expanded states with their cost, G-value, and H-value
                labelGrid[r][c].setText(String.format("<html>Cost: %d <br>G: %d<br>H: %d</html>", state.getCost(), state.getG(), state.getH()));
                labelGrid[r][c].setBackground(Color.YELLOW);
            }

            drawCurrentPath();

            // Disable forward button once the end of the path is reached
            if (count.get() == currentStates.size() - 1) {
                forwardButton.setEnabled(false);
            }
        }
    }

    private void moveBackward() {
        // TODO: Implement
//        if (count.get() > 0) {
//            count.decrementAndGet();
//        }
    }

    private void drawCurrentPath() {
        SearchState current = currentStates.get(count.get());
        while (current.getParent() != null) {
            int sid = current.getStateId();

            if (sid == goalSid.get()) {
                current = current.getParent();
                continue;
            }

            int r = map.getRowFromStateId(sid);
            int c = map.getColFromStateId(sid);

            labelGrid[r][c].setBackground(Color.RED);
            redLabels.add(labelGrid[r][c]);

            current = current.getParent();
        }
    }

    private void handleMouseClick(MouseEvent e) {
        int cellWidth = getWidth() / GRID_SIZE;
        int cellHeight = getHeight() / GRID_SIZE;

        int mouseX = e.getX();
        int mouseY = e.getY();

        int clickedRow = mouseY / cellHeight;
        int clickedCol = mouseX / cellWidth;

        int sid = map.getStateId(clickedRow, clickedCol);

        if (e.getButton() == MouseEvent.BUTTON1) {
            handleLeftClick(clickedRow, clickedCol, sid);
        } else if (e.getButton() == MouseEvent.BUTTON3) {
            handleRightClick(clickedRow, clickedCol, sid);
        }
    }

    private void handleLeftClick(int row, int col, int sid) {
        if (goalSid.get() == -1) {
            if (map.isWall(row, col)) {
                map.placeOpenStateAt(sid);
                setLabel(row, col, Color.WHITE, "");
            } else {
                map.placeWallAt(sid);
                setLabel(row, col, Color.BLACK, "");

                // If wall was place on start, remove start
                if (startSid.get() == sid) {
                    startSid.set(-1);
                }
            }
        } else {
            JOptionPane.showMessageDialog(this, "Path has been found! To place more walls, reset the map");
        }
    }

    private void handleRightClick(int row, int col, int sid) {
        if (startSid.get() == -1) {
            setStart(row, col, sid);
        } else if (goalSid.get() == -1) {
            setGoal(row, col, sid);
        }
    }

    private void setStart(int row, int col, int sid) {
        setLabel(row, col, Color.GREEN, "START");
        startSid.set(sid);
    }

    private void setGoal(int row, int col, int sid) {
        setLabel(row, col, Color.BLUE, "GOAL");
        goalSid.set(sid);

        findPath();
    }

    private void setLabel(int row, int col, Color color, String text) {
        labelGrid[row][col].setBackground(color);
        labelGrid[row][col].setText(text);
        labelGrid[row][col].setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void setLabel(int row, int col, Color color) {
        labelGrid[row][col].setBackground(color);
        labelGrid[row][col].setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void findPath() {
        List<SearchState> path = aStar.findPath(new SearchState(startSid.get()), new SearchState(goalSid.get()));

        currentStates = aStar.getCurrentStates();
        statesExpandedPerMove = aStar.getStatesExpandedPerMove();

        if (path != null) {
            CompressAStar compressAStar = new CompressAStar(aStar);
            currentSubgoals = compressAStar.findCompressedPath(path, new HillClimbingWithClosedSet(new MapSearchProblem(map)), null, true);
        }
    }
}
