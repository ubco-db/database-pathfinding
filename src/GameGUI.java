/**
 * Implements a GUI that allows the user to load a map, compute hill-climbable regions, and show how A* or LRTA* would find a path between two user selected points.
 */

import database.DBStatsRecord;
import database.GameDB;
import database.SubgoalDB;
import database.SubgoalDBRecord;
import map.ChangeRecord;
import map.GameMap;
import map.SparseMask;
import search.AStar;
import search.AStarHeuristic;
import search.GenHillClimbing;
import search.MapSearchProblem;
import search.SearchState;
import search.SearchUtil;
import search.StatsRecord;
import util.HeuristicFunction;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;


/**
 * Implements a GUI that allows the user to load a map, and show how A* or would find a path between two user selected points.
 */
public class GameGUI extends JFrame {
    private final MapPanel panel;

    // Menu items
    private final JMenuItem exitMenuItem;
    private final JMenuItem computeMenuItem;
    private final JMenuItem visualizeMenuItem;
    private final JMenuItem nextMenuItem;
    private final JMenuItem prevMenuItem;
    private final JMenuItem loadMenuItem;
    private final JMenuItem rotateMenuItem;
    private final JMenuItem exportMapMenuItem;
    private final JMenuItem exportDBMenuItem;
    private final JMenuItem computeDBMenuItem;
    private final JMenuItem coverageDBMenuItem;

    private final JTextField speedField;
    private final JComboBox<String> cbxSearchMethod;
    private String currentPath = System.getProperty("user.dir");
    private SparseMask pathMask;
    private final JTextField locationField;
    private ArrayList<HeuristicFunction> heuristicList;

    /*
     * Configuration constants
     */
    private final int CUTOFF = 25;                // Maximum # of HC moves

    public static void main(String[] args) {
        GameGUI frame = new GameGUI();
        frame.setTitle("Map Abstraction GUI");
        frame.setVisible(true);
    }

    public GameGUI() {
        final int DEFAULT_FRAME_WIDTH = 1000;
        final int DEFAULT_FRAME_HEIGHT = 1000;
        setSize(DEFAULT_FRAME_WIDTH, DEFAULT_FRAME_HEIGHT);

        // Add menu
        MenuListener listener = new MenuListener();
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        menuBar.add(fileMenu);
        loadMenuItem = new JMenuItem("Load...");
        loadMenuItem.addActionListener(listener);
        fileMenu.add(loadMenuItem);
        computeMenuItem = new JMenuItem("Compute Abstractions");
        computeMenuItem.addActionListener(listener);
        fileMenu.add(computeMenuItem);
        computeDBMenuItem = new JMenuItem("Compute Database");
        computeDBMenuItem.addActionListener(listener);
        fileMenu.add(computeDBMenuItem);
        coverageDBMenuItem = new JMenuItem("Database Coverage");
        coverageDBMenuItem.addActionListener(listener);
        fileMenu.add(coverageDBMenuItem);
        visualizeMenuItem = new JMenuItem("Visualize Greedy Abstraction");
        visualizeMenuItem.addActionListener(listener);
        fileMenu.add(visualizeMenuItem);
        nextMenuItem = new JMenuItem("Next");
        nextMenuItem.addActionListener(listener);
        fileMenu.add(nextMenuItem);
        prevMenuItem = new JMenuItem("Previous");
        prevMenuItem.addActionListener(listener);
        fileMenu.add(prevMenuItem);
        rotateMenuItem = new JMenuItem("Rotate Map");
        rotateMenuItem.addActionListener(listener);
        fileMenu.add(rotateMenuItem);
        exportMapMenuItem = new JMenuItem("Export Map");
        fileMenu.add(exportMapMenuItem);
        exportMapMenuItem.addActionListener(listener);
        exportDBMenuItem = new JMenuItem("Export DB");
        fileMenu.add(exportDBMenuItem);
        exportDBMenuItem.addActionListener(listener);
        exitMenuItem = new JMenuItem("Exit");
        fileMenu.add(exitMenuItem);

        exitMenuItem.addActionListener(listener);

        // add components to content pane
        Container contentPane = getContentPane();
        locationField = new JTextField(15);
        speedField = new JTextField(10);
        speedField.addActionListener(new TextFieldListener());
        JPanel p = new JPanel();
        p.add(new JLabel("Map location: "));
        p.add(locationField);
        p.add(new JLabel("Animation speed:"));
        p.add(speedField);
        cbxSearchMethod = new JComboBox<>();
        cbxSearchMethod.addItem("A*");
        cbxSearchMethod.addItem("Hill-climbing");
        cbxSearchMethod.addItem("Wall hugging");
        cbxSearchMethod.addItem("orz103d");
        cbxSearchMethod.addItem("ost000a");
        cbxSearchMethod.addItem("lak505d");
        p.add(new JLabel("Search method:"));
        p.add(cbxSearchMethod);
        contentPane.add(p, "South");

        // Load heuristics
        heuristicList = new ArrayList<HeuristicFunction>();
        heuristicList = HeuristicCompare.createHeuristics();

        GameMap mp = new GameMap("maps/smallMaps/012.map");
        //GameMap mp = new GameMap("maps/wc3maps/divideandconquer.map");
        panel = new MapPanel(mp);
        contentPane.add(panel, "Center");
        WindowCloser winlist = new WindowCloser();
        addWindowListener(winlist);

        addKeyListener(new KeyAdapt());
    }

    private class TextFieldListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {    // get user input
            String input = speedField.getText();
            if (input != null) {
                int speed = Integer.parseInt(input);
                panel.changeSpeed(speed);
            }
        }
    }

    private static class WindowCloser extends WindowAdapter {
        public void windowClosing(WindowEvent event) {
            System.exit(0);
        }
    }

    private class MenuListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {  // find the menu that was selected
            Object source = event.getSource();
            if (source == exitMenuItem)
                System.exit(0);
            else if (source == computeMenuItem)
                panel.compute();
            else if (source == visualizeMenuItem)
                panel.visualize();
            else if (source == nextMenuItem)
                panel.next();
            else if (source == prevMenuItem)
                panel.prev();
            else if (source == computeDBMenuItem)
                panel.computeDB();
            else if (source == coverageDBMenuItem)
                panel.coverageDB();
            else if (source == rotateMenuItem)
                panel.rotate();
            else if (source == exportMapMenuItem || source == exportDBMenuItem) {
                JFileChooser jfile = new JFileChooser(currentPath);
                int returnVal = jfile.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String fileName = jfile.getSelectedFile().getAbsolutePath();
                    if (source == exportMapMenuItem)
                        panel.exportMap(fileName);
                    else
                        panel.exportDB(fileName);
                }
                currentPath = jfile.getSelectedFile().getPath();
            } else if (source == loadMenuItem) {
                JFileChooser jfile = new JFileChooser(currentPath);
                int returnVal = jfile.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String fileName = jfile.getSelectedFile().getAbsolutePath();
                    panel.load(fileName);
                    currentPath = jfile.getSelectedFile().getPath();
                }
            }
            panel.requestFocusInWindow();
        }
    }

    private class KeyAdapt extends KeyAdapter {
        public void keyPressed(KeyEvent evt) {
            int key = evt.getKeyCode();

            switch (key) {
                case 38:        // Up arrow (back to prev)
                    panel.prev();
                    break;
                case 40:        // Down arrow (advance to next)
                    panel.next();
                    break;
            }
        }
    }

    private class MapPanel extends JPanel implements MouseListener, MouseMotionListener {
        private final GameMap map;                      // Current base map being displayed
        private SubgoalDB subgoalDB = null;             // Stores database of subgoals
        private final ArrayList<GameMap> maps;          // Stores list of maps which may be derived from base map
        private final ArrayList<String> mapDesc;        // One line text description associated with each derived map
        private int currentIndex;                       // Current index of map in list being displayed

        private Timer timer;                            // Timer is used when animating and implements delay between moves
        private int speed = 10;                         // Delay in ms between moves that are displayed.  Set at 10, but controllable by the user.

        public MapPanel(GameMap mp) {
            map = mp;
            maps = new ArrayList<GameMap>();
            mapDesc = new ArrayList<String>();
            maps.add(map);
            mapDesc.add("Original Map. States: " + map.states);
            currentIndex = 0;
            addMouseListener(this);
            this.addMouseMotionListener(this);
            addKeyListener(new KeyAdapt());
        }

        public void rotate() {
            maps.get(currentIndex).rotate();
            repaint();
        }

        public void exportMap(String fileName) {
            maps.get(currentIndex).save(fileName);
        }

        public void exportDB(String fileName) {
            map.outputImage(fileName, null, null);
        }

        public void computeDB() {
            GameMap tmp = maps.get(currentIndex);
            MapSearchProblem problem = new MapSearchProblem(tmp);
            // Stores groups of cells
            GameDB database = new GameDB(problem);
            Object[] possibilities = {"RANDOM - 1000", "RANDOM - 10000", "RANDOM - 20000", "ABSTRACT", "HC REGIONS"};
            String s = (String) JOptionPane.showInputDialog(this, "Enter database type:", "Compute a Database", JOptionPane.PLAIN_MESSAGE, null, possibilities, "RANDOM - 1000");
            DBStatsRecord dbstat = new DBStatsRecord(20);

            //If a string was returned, say so.
            if ((s != null) && (s.length() > 0)) {
                if (s.contains("RANDOM")) { // Random database
                    int size = Integer.parseInt(s.substring(9));
                    subgoalDB = new SubgoalDB();
                    subgoalDB = database.computeRandomDB(size, new GenHillClimbing(problem, 25), dbstat, subgoalDB);
                }
//			    else if(s.equals("ABSTRACT"))
//			    {
//			    	subgoalDB = new SubgoalDBDLRTA();
//			    	subgoalDB = (SubgoalDB) database.computeAbstractDB(dbstat, (SubgoalDBDLRTA) subgoalDB);
//			    }
                else {    // HC Regions
                    subgoalDB = database.computeDBDP2(subgoalDB, new GenHillClimbing(problem, 25), dbstat, 2);
                }
                JFileChooser jfile = new JFileChooser(currentPath);
                jfile.setDialogTitle("Save subgoal database file");
                int returnVal = jfile.showSaveDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    String fileName = jfile.getSelectedFile().getAbsolutePath();
                    this.subgoalDB.exportDB(fileName);
                    currentPath = jfile.getSelectedFile().getPath();
                }
            }
        }

        public void coverageDB() {
            // Prompt for a database
            JFileChooser jfile = new JFileChooser(currentPath);
            jfile.setDialogTitle("Load subgoal database file");
            int returnVal = jfile.showOpenDialog(null);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String fileName = jfile.getSelectedFile().getAbsolutePath();
                currentPath = jfile.getSelectedFile().getPath();

                // Compute the coverage
                byte[][] coverage = new byte[map.rows][map.cols];
                subgoalDB = new SubgoalDB();
                subgoalDB.load(fileName);
                Cursor hourglassCursor = new Cursor(Cursor.WAIT_CURSOR);
                setCursor(hourglassCursor);
                // int cutoff = 25;
                // double coverageVal = subgoalDB.computeCoverage(map, coverage, cutoff);
                double coverageVal = 0;    //TODO:
                Cursor normalCursor = new Cursor(Cursor.DEFAULT_CURSOR);
                setCursor(normalCursor);

                System.out.println("Overall coverage percentage: " + coverageVal);
                SparseMask currentMask = map.createCoverageMask(coverage);
                map.clearMasks();
                map.addMask(currentMask);
                map.resetMask();
                this.repaint();
            }    // end successful DB load
        }

        public void load(String fileName) {
            map.load(fileName);
            maps.clear();
            mapDesc.clear();
            maps.add(map);
            mapDesc.add("Original Map. States: " + map.states);
            currentIndex = 0;
            this.subgoalDB = null;
            repaint();
        }

        public void visualize() {
            maps.clear();
            mapDesc.clear();
            maps.add(map);
            mapDesc.add("Original Map. States: " + map.states);
            currentIndex = 0;

            TimerAction action = new TimerAction();
            timer = new Timer(speed, action);
            timer.start();
        }

        public void changeSpeed(int speed) {
            this.speed = speed;
            if (timer != null)
                timer.setDelay(speed);
        }

        private class TimerAction implements ActionListener {
            public void actionPerformed(ActionEvent evt) {

                if (!map.nextMask()) {
                    timer.stop();
                    map.prevMask();
                    repaint();
                    return;
                }
                repaint();
            }
        }

        public void compute() {
            String st;
			/*
			// This code computes clique abstraction at various levels
			GameMap prev = map;
			for (int k = 1; k <= 5; k++)
			{				
				GameMap nmap = prev.cliqueAbstract();
				MapSearchProblem problem = new MapSearchProblem(nmap);
				st = "Abstraction Level "+k+".  States: "+nmap.states;
				GameDB database = new GameDB(problem);
				// Remove these next three lines if you do not want to see centroid in group abstraction
				database.computeGroups();
				database.computeCentroids();
				GameMap nmapCentroid = database.computeCentroidMap();
				
				maps.add(nmapCentroid);
				mapDesc.add(st);
				// nmap.print();
				prev = nmap;
			}			
			*/
            // Computer greedy abstraction
            DBStatsRecord dbstat = new DBStatsRecord();
            MapSearchProblem problem = new MapSearchProblem(map);
            GameMap greedyMap = map.reachableAbstract(new GenHillClimbing(problem, CUTOFF * 2), dbstat);
            // GameMap greedyMap = map.reachableAbstract(new GenHillClimbing(map, cutoff*2), dbstat);
            st = "Greedy abstraction.  States: " + greedyMap.states;
            maps.add(greedyMap);
            mapDesc.add(st);
            // currentIndex = maps.size()-1;

            // A map that takes greedy map and condensed groups that only have one neighbour
            GameMap neighborMap = greedyMap.neighbourCondense();
            st = "Neighbor condense.  States: " + neighborMap.states;
            maps.add(neighborMap);
            mapDesc.add(st);
			/* 	
			// A map that displays all border points in the map
			GameMap borderMap = map.computeBorder();
			st = "Map border points.  States: "+borderMap.states;
			maps.add(borderMap);
			mapDesc.add(st);
			repaint();
			*/

            repaint();
        }

        public void next() {
            // Advance first by mask then by map
            if (!maps.get(currentIndex).nextMask()) {
                if (currentIndex < maps.size() - 1)
                    currentIndex++;
            }
            repaint();
        }

        public void prev() {
            if (currentIndex > 0)
                currentIndex--;
            repaint();
        }

        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            Font f = new Font("Serif", Font.BOLD, 18);
            g2.setFont(f);
            if (currentIndex >= mapDesc.size())
                return;
            g2.drawString(mapDesc.get(currentIndex), 10, 15);
            GameMap map = maps.get(currentIndex);
            map.draw(g2);
        }

        public void mouseClicked(MouseEvent arg0) {
            // Add point
            // Check if a valid square
            Point p = map.getSquare(arg0.getPoint());
            if (p == null) // Invalid point selected
                return;
            System.out.println("In click. Point: " + p.x + " , " + p.y);
            MapSearchProblem problem = new MapSearchProblem(map);
            if (map.squares[p.x][p.y] != GameMap.WALL_CHAR) {
                if (map.startPoint == null) {
                    map.startPoint = p;
                    pathMask = new SparseMask();
                    pathMask.add(new ChangeRecord(p.x, p.y, Color.GREEN, 1));
                    map.addMask(pathMask);
                    map.resetMask();
                } else if (map.goalPoint == null) {
                    map.goalPoint = p;
                    pathMask.add(new ChangeRecord(p.x, p.y, Color.BLUE, 1));
                    String searchMethod = (String) cbxSearchMethod.getSelectedItem();

                    if (searchMethod.contains("Query") && subgoalDB != null) {    // Database lookup query
                        ArrayList<SubgoalDBRecord> records;
                        StatsRecord stats = new StatsRecord();
                        if (searchMethod.equals("Query subgoals")) {    // Show closest 10 subgoals in database
                            records = subgoalDB.findNearest(problem, new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), 10);
                        } else {    // Show up to 10 closes hill-climbable reachable subgoals in the database
                            records = subgoalDB.findNearest(problem, new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), new GenHillClimbing(problem, CUTOFF), 10, stats, null);
                        }
                        // Create the masks to show these choices
                        // Just modify existing mask
                        SparseMask currentMask = pathMask;
                        int max = 5;
                        Color[] colors = new Color[]{Color.RED, Color.yellow, Color.orange, Color.green, Color.pink, Color.cyan, Color.magenta, Color.lightGray, Color.darkGray, Color.gray};
                        for (int l = 0; l < records.size() && l < max; l++) {
                            SubgoalDBRecord rec = records.get(l);
                            ChangeRecord r = new ChangeRecord(map.getRow(rec.getStartId()), map.getCol(rec.getStartId()), colors[l], 1);
                            currentMask.add(r);
                            r = new ChangeRecord(map.getRow(rec.getGoalId()), map.getCol(rec.getGoalId()), colors[l], 1);
                            currentMask.add(r);
                        }
                        // map.addMask(currentMask);
                    } else {    // Path search cases
                        ArrayList<SearchState> path, subgoalPath = new ArrayList<>(50), subgoals = new ArrayList<>(), expanded = null;
                        // Auto compute A*
                        StatsRecord stats = new StatsRecord();
                        switch (searchMethod) {
                            case "A*":
                                AStar astar = new AStar(problem);
                                path = astar.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                SearchUtil.printPath(path);
                                expanded = astar.getStatesExpanded();
                                break;
                            case "Wall hugging": {
                                AStarHeuristic alg = new AStarHeuristic(problem, heuristicList.get(7));
                                path = alg.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                SearchUtil.printPath(path);
                                expanded = alg.getStatesExpanded();
                                break;
                            }
                            case "orz103d": {
                                AStarHeuristic alg = new AStarHeuristic(problem, heuristicList.get(5));
                                path = alg.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                SearchUtil.printPath(path);
                                expanded = alg.getStatesExpanded();
                                break;
                            }
                            case "lak505d": {
                                AStarHeuristic alg = new AStarHeuristic(problem, heuristicList.get(4));
                                path = alg.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                SearchUtil.printPath(path);
                                expanded = alg.getStatesExpanded();
                                break;
                            }
                            case "ost000a": {
                                AStarHeuristic alg = new AStarHeuristic(problem, heuristicList.get(6));
                                path = alg.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                SearchUtil.printPath(path);
                                expanded = alg.getStatesExpanded();
                                break;
                            }
                            case "Hill-climbing":
                                GenHillClimbing hc = new GenHillClimbing(problem, 10000);
                                path = hc.computePath(new SearchState(map.getId(map.startPoint.x, map.startPoint.y)), new SearchState(map.getId(map.goalPoint.x, map.goalPoint.y)), stats);
                                break;
                            default:
                                path = null;
                                break;
                        }

                        map.path = path;
                        SparseMask currentMask = new SparseMask(pathMask);
                        ChangeRecord last = null;
                        Color pathColor = Color.RED;
                        if (path == null) {
                            JOptionPane.showMessageDialog(this, "No path found between: " + map.startPoint + " and " + map.goalPoint);
                            // return;
                        }
                        if (expanded != null) {
                            for (SearchState current : expanded) {
                                int row = map.getRow(current.id);
                                int col = map.getCol(current.id);

                                // Visualize changes
                                if ((row == map.goalPoint.x && col == map.goalPoint.y) || (row == map.startPoint.x && col == map.startPoint.y))
                                    continue;    // Do not overwrite special characters

                                currentMask = new SparseMask(currentMask);

                                //if (AStar.inStateList(path, current))
                                //	continue;

                                ChangeRecord rec = new ChangeRecord(row, col, Color.yellow, 1);

                                currentMask.add(rec);
                                last = rec;
                                map.addMask(currentMask);
                            }
                        }
                        if (path != null) {
                            for (SearchState current : path) {
                                int row = map.getRow(current.id);
                                int col = map.getCol(current.id);

                                // Visualize changes
                                if ((row == map.goalPoint.x && col == map.goalPoint.y) || (row == map.startPoint.x && col == map.startPoint.y))
                                    continue;    // Do not overwrite special characters

                                currentMask = new SparseMask(currentMask);
                                if (last != null) last.color = Color.orange;

                                Color color = pathColor;
                                if (subgoalPath != null) {    // Color subgoals a slightly different color within the path
                                    // Shows subgoals that would be produced in a DB entry
                                    if (subgoalPath.contains(current)) color = Color.orange;
                                }
                                if (SearchUtil.inStateList(subgoals, current))    // This shows subgoals actually used by the various search algorithms
                                    color = Color.orange;

                                ChangeRecord rec = new ChangeRecord(row, col, color, 1);

                                currentMask.add(rec);
                                last = rec;
                                map.addMask(currentMask);
                            }
                        }
                        System.out.println("Stats: " + stats);    // Print out the stats to the console (TODO: May want to print to textbox in GUI.)
                        TimerAction action = new TimerAction();
                        timer = new Timer(speed, action);
                        timer.start();
                    } // end of path search cases
                } else {
                    map.goalPoint = null;
                    map.path = null;
                    map.startPoint = p;
                    pathMask.clear();
                    pathMask.add(new ChangeRecord(p.x, p.y, Color.GREEN, 1));
                    map.clearMasks();
                    map.addMask(pathMask);
                    map.resetMask();
                }
                repaint();
            }
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseDragged(MouseEvent arg0) {
        }

        public void mouseMoved(MouseEvent e) {
            Point p = map.getSquare(e.getPoint());
            if (p == null) return;    // Invalid point selected
            locationField.setText(p.x + ", " + p.y + " (" + map.getId(p.x, p.y) + ")");
        }
    }
}

