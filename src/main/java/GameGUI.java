import map.GameMap;
import map.VisualizeMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.MapSearchProblem;
import search.SearchState;
import search.algorithms.DBAStar;
import search.algorithms.HillClimbing;
import search.algorithms.PRAStar;
import search.algorithms.visual.VisualAStar;
import stats.SearchStats;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * Implements a GUI that allows the user to load a map, and show how different algorithms would find a path between two
 * user selected points.
 */
public class GameGUI extends JFrame {
    final int DEFAULT_FRAME_WIDTH = 800;
    final int DEFAULT_FRAME_HEIGHT = 800;

    final int GRID_SIZE = 32;

    private static final String A_STAR = "A*";
    private static final String HILL_CLIMBING = "Hill-climbing";
    private static final String PRA_STAR = "PRA*";
    private static final String DBA_STAR = "DBA*";

    String fileName = "src/main/resources/maps/012.map";

    private final JComboBox<String> searchMethodComboBox;
    private final JTextField locationField;
    private final MapPanel mapPanel;

    private static final Logger logger = LogManager.getLogger(GameGUI.class);

    public GameGUI() {
        setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);

        Container contentPane = getContentPane();

        // TODO: Let user select map
        GameMap map = new GameMap(fileName);
        mapPanel = new MapPanel(map);

        // Add map to contentPane inside centerPanel
        JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        centerPanel.add(mapPanel);
        contentPane.add(centerPanel, BorderLayout.CENTER);

        // Add location field
        this.locationField = new JTextField(15);

        // Add combo box to select algorithm / search method
        this.searchMethodComboBox = new JComboBox<>();
        this.searchMethodComboBox.addItem(A_STAR);
        this.searchMethodComboBox.addItem(HILL_CLIMBING);
        this.searchMethodComboBox.addItem(PRA_STAR);
        this.searchMethodComboBox.addItem(DBA_STAR);

        this.locationField.addActionListener(e -> {
            String input = e.getActionCommand();
            try {
                int sid = Integer.parseInt(input);

                int row = map.getRowFromStateId(sid);
                int col = map.getColFromStateId(sid);

                System.out.println(sid);

                if (map.isInBounds(row, col)) {
                    System.out.println("HERE");
                    mapPanel.drawPointsAndFindPath(new Point(row, col), row, col);
                } else {
                    JOptionPane.showMessageDialog(this, sid + " is not within bounds");
                }
            } catch (NumberFormatException ignored) {
            }
        });

        // Add controls to contentPane inside JPanel
        JPanel controls = new JPanel();
        controls.add(new JLabel("Location on Map: "));
        controls.add(this.locationField);
        controls.add(new JLabel("Search Method:"));
        controls.add(this.searchMethodComboBox);
        contentPane.add(controls, BorderLayout.SOUTH);

        this.setResizable(false);

        // Stops the program if the window is closed
        WindowCloser windowCloser = new WindowCloser();
        addWindowListener(windowCloser);
    }

    public static void main(String[] args) {
        GameGUI frame = new GameGUI();
        frame.setTitle("Game Pathfinding Visualization");
        frame.setVisible(true);
    }

    private static class WindowCloser extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            System.exit(0);
        }
    }

    private class MapPanel extends JPanel implements MouseListener, MouseMotionListener {
        private final int HEIGHT_OFFSET = 0;

        private final VisualizeMap visualizeMap;
        private final GameMap map;
        private Point startPoint, goalPoint;

        public MapPanel(GameMap map) {
            int DEFAULT_PANEL_WIDTH = DEFAULT_FRAME_WIDTH - 50;
            int DEFAULT_PANEL_HEIGHT = DEFAULT_FRAME_HEIGHT - 50;
            this.map = map;
            this.visualizeMap = new VisualizeMap(map, DEFAULT_PANEL_WIDTH, DEFAULT_PANEL_HEIGHT, HEIGHT_OFFSET);

            // FIXME: Not sure where these numbers come from, found them experimentally
            this.setPreferredSize(new Dimension(DEFAULT_PANEL_WIDTH - 10, DEFAULT_PANEL_HEIGHT - 55));

            addMouseListener(this);
            addMouseMotionListener(this);
        }


        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            visualizeMap.draw((Graphics2D) g, false);
        }

        /**
         * Convert clicked point to point on map
         *
         * @param clickedPoint point clicked by cursor
         * @return point on map (x and y coordinates now correspond to row and column in the 2D states array in the GameMap)
         */
        private Point getPointOnMap(Point clickedPoint) {
            int x = (int) clickedPoint.getX();
            int y = (int) clickedPoint.getY() - this.HEIGHT_OFFSET;

            int row = y / visualizeMap.getCellHeight();
            int col = x / visualizeMap.getCellWidth();

            if (row >= map.getNumRows() || col >= map.getNumCols()) {
                return null;
            }

            return new Point(row, col);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            Point mapPoint = getPointOnMap(e.getPoint());

            if (mapPoint == null) return;

            int r = mapPoint.x;
            int c = mapPoint.y;

            logger.info(String.format("Selected point: %d, %d (%s)", r, c, map.getStateId(r, c)));

            drawPointsAndFindPath(mapPoint, r, c);
        }

        private void drawPointsAndFindPath(Point mapPoint, int r, int c) {
            if (!map.isWall(r, c)) {
                if (startPoint == null) {
                    startPoint = mapPoint;
                    visualizeMap.initPathMask();
                    visualizeMap.addToPathMask(r, c, Color.GREEN);
                } else if (goalPoint == null) {
                    goalPoint = mapPoint;

                    visualizeMap.addToPathMask(r, c, Color.BLUE);

                    // Get pathfinding algorithm
                    String searchMethod = (String) searchMethodComboBox.getSelectedItem();

                    // Find id
                    int startStateId = map.getStateId(startPoint.x, startPoint.y);
                    int goalStateId = map.getStateId(goalPoint.x, goalPoint.y);

                    SearchState startState = new SearchState(startStateId);
                    SearchState goalState = new SearchState(goalStateId);
                    MapSearchProblem mapSearchProblem = new MapSearchProblem(map);

                    List<SearchState> path;
                    List<SearchState> subgoals = null;
                    List<SearchState> expanded = null;

                    SearchStats searchStats = new SearchStats();

                    switch (searchMethod) {
                        case HILL_CLIMBING -> {
                            HillClimbing hillClimbing = new HillClimbing(mapSearchProblem);
                            path = hillClimbing.findPath(startState, goalState);
                        }
                        case A_STAR -> {
                            VisualAStar aStar = new VisualAStar(mapSearchProblem);
                            path = aStar.findPath(startState, goalState);
                            expanded = aStar.getStatesExpanded();
                        }
                        case PRA_STAR -> {
                            // FIXME: Pass MapSearchProblem instead of map for consistency?
                            PRAStar praStar = new PRAStar(map, GRID_SIZE);
                            path = praStar.findPath(startState, goalState, searchStats);
                            subgoals = praStar.getSubgoals();
                        }
                        case DBA_STAR -> {
                            // FIXME: Pass MapSearchProblem instead of map for consistency?
                            DBAStar dbaStar = new DBAStar(map, GRID_SIZE, true);
                            path = dbaStar.findPath(startState, goalState, searchStats);
                            subgoals = dbaStar.getSubgoals();
                        }
                        case null -> throw new RuntimeException("Search method was null!");
                        default -> throw new IllegalStateException("Unexpected value: " + searchMethod);
                    }

                    // Give user feedback if no path could be found
                    if (path == null) {
                        JOptionPane.showMessageDialog(this, "No path found between: " + startPoint + " and " + goalPoint);
                        return;
                    }

                    // Draw states that were expanded during the search in yellow
                    if (expanded != null) {
                        draw(expanded, Color.YELLOW);
                    }

                    // Draw the path in red
                    draw(path, Color.RED);

                    if (subgoals != null) {
                        draw(subgoals, Color.ORANGE);
                    }
                } else {
                    goalPoint = null;
                    startPoint = mapPoint;
                    visualizeMap.clearPathMask();
                    visualizeMap.addToPathMask(mapPoint.x, mapPoint.y, Color.GREEN);
                }
                repaint();
            } else {
                JOptionPane.showMessageDialog(this, map.getStateId(r, c) + " is a wall!");
            }
        }

        private void draw(List<SearchState> searchStates, Color color) {
            for (SearchState current : searchStates) {
                int row = map.getRowFromStateId(current.getStateId());
                int col = map.getColFromStateId(current.getStateId());

                // Don't overwrite start and goal (they have already been marked)
                if ((row == goalPoint.x && col == goalPoint.y) || (row == startPoint.x && col == startPoint.y)) {
                    continue;
                }

                visualizeMap.addToPathMask(row, col, color);
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            Point mapPoint = getPointOnMap(e.getPoint());

            if (mapPoint == null) return;

            int r = mapPoint.x;
            int c = mapPoint.y;

            locationField.setText(String.format("%d, %d (%s)", r, c, map.getStateId(r, c)));
        }

        @Override
        public void mousePressed(MouseEvent e) {
        }

        @Override
        public void mouseReleased(MouseEvent e) {
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        }
    }
}
