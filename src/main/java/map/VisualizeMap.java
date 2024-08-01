package map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import search.SearchState;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class VisualizeMap {
    private final int heightOffSet;

    private final HashMap<Integer, Color> colors;

    private int cellHeight, cellWidth;
    private List<ChangeRecord> pathMask;
    private List<ChangeRecord> subgoalMask;

    private final GameMap map;

    private static final Logger logger = LogManager.getLogger(VisualizeMap.class);

    public VisualizeMap(GameMap gameMap, int panelWidth, int panelHeight, int heightOffset) {
        this.map = gameMap;

        this.heightOffSet = heightOffset;

        // Calculate cell dimensions based on panel size and number of rows/columns
        cellHeight = panelHeight / map.getNumRows();
        cellWidth = panelWidth / map.getNumCols();

        // Ensure minimum cell size is 1
        cellHeight = Math.max(1, cellHeight);
        cellWidth = Math.max(1, cellWidth);

        // Make cell sizes equal, favoring the smaller dimension
        if (cellHeight > cellWidth) {
            //noinspection SuspiciousNameCombination
            cellHeight = cellWidth;
        } else {
            //noinspection SuspiciousNameCombination
            cellWidth = cellHeight;
        }

        colors = new HashMap<>();

        Random generator = new Random();
        generator.setSeed(56256902);

        // Assign each region its own colour
        if (map instanceof AbstractedMap) {
            for (Region region : ((AbstractedMap) map).getRegionIdToRegionMap().values()) {
                Color color = new Color(generator.nextFloat(), generator.nextFloat(), generator.nextFloat());
                colors.put(region.getRegionId(), color);
            }
        }
    }

    public VisualizeMap(GameMap gameMap) {
        this(gameMap, 850, 850, 0);
    }

    public VisualizeMap computeCentroidMap() {
        if (!(map instanceof AbstractedMap)) {
            throw new IllegalArgumentException("Map is not abstracted");
        }

        for (Region region : ((AbstractedMap) map).getRegionIdToRegionMap().values()) {
            int regionRep = region.getRegionRepresentative();
            int regionRepRow = map.getRowFromStateId(regionRep);
            int regionRepCol = map.getColFromStateId(regionRep);

            // Mark region reps as empty states on the map
            map.states[regionRepRow][regionRepCol] = GameMap.EMPTY_CHAR;
        }
        return this;
    }

    public void outputImage(String fileName, List<SearchState> path, List<SearchState> subgoals) {
        // Draw subgoals in yellow
        if (subgoals != null) {
            subgoalMask = new ArrayList<>(subgoals.size());
            for (SearchState current : subgoals) {
                int row = map.getRowFromStateId(current.getStateId());
                int col = map.getColFromStateId(current.getStateId());

                subgoalMask.add(new ChangeRecord(row, col, Color.YELLOW));
            }
        }

        // Draw path in red
        if (path != null) {
            pathMask = new ArrayList<>(path.size());
            for (int i = 0; i < path.size(); i++) {
                SearchState current = path.get(i);
                int row = map.getRowFromStateId(current.getStateId());
                int col = map.getColFromStateId(current.getStateId());

                if (i == 0) {
                    pathMask.add(new ChangeRecord(row, col, Color.GREEN));
                } else if (i == path.size() - 1) {
                    pathMask.add(new ChangeRecord(row, col, Color.BLUE));
                } else {
                    pathMask.add(new ChangeRecord(row, col, Color.RED));
                }
            }
        }

        // Create an image to save
        RenderedImage rendImage = createImage();

        // Write generated image to a file
        try {
            // Save as PNG
            File file = new File("images/" + fileName);
            ImageIO.write(rendImage, "png", file);
        } catch (IOException e) {
            logger.error(e);
        }
    }

    public void outputImage(String fileName, List<SearchState> path) {
        outputImage(fileName, path, null);
    }

    public RenderedImage createImage() {
        BufferedImage bufferedImage = new BufferedImage(map.getNumCols() * cellWidth, map.getNumRows() * cellHeight, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = bufferedImage.createGraphics();

        this.draw(graphics, true);
        graphics.dispose();

        return bufferedImage;
    }

    public void draw(Graphics2D g, boolean abstracted) {
        int rows = map.getNumRows();
        int cols = map.getNumCols();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                // Get whether current state is wall or open
                int stateValue = map.getStateValue(r, c);

                // Colour walls in black, open states in white, and abstracted states in their region colour
                if (stateValue == GameMap.WALL_CHAR) {
                    g.setColor(Color.black);
                } else if (!abstracted || stateValue == GameMap.EMPTY_CHAR) {
                    g.setColor(Color.white);
                } else {
                    g.setColor(colors.get(stateValue));
                }

                // Draw rectangle to represent state value
                g.fillRect(c * cellWidth, r * cellHeight + heightOffSet, cellWidth, cellHeight);
            }
        }

        if (subgoalMask != null) {
            for (ChangeRecord rec : subgoalMask) {
                g.setColor(rec.color);
                g.fillRect(rec.col * cellWidth, rec.row * cellHeight + heightOffSet, cellWidth, cellHeight);
            }
        }

        if (pathMask != null) {
            for (ChangeRecord rec : pathMask) {
                g.setColor(rec.color);
                g.fillRect(rec.col * cellWidth, rec.row * cellHeight + heightOffSet, cellWidth, cellHeight);
            }
        }
    }

    public int getCellHeight() {
        return cellHeight;
    }

    public int getCellWidth() {
        return cellWidth;
    }

    public void initPathMask() {
        pathMask = new ArrayList<>();
    }

    public void addToPathMask(int row, int col, Color color) {
        pathMask.add(new ChangeRecord(row, col, color));
    }

    public void clearPathMask() {
        pathMask.clear();
    }

    public GameMap getMap() {
        return map;
    }

    private record ChangeRecord(int row, int col, Color color) {
    }
}
