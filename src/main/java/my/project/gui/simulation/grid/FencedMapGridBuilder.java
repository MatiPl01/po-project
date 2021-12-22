package my.project.gui.simulation.grid;

import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Pane;
import javafx.scene.layout.RowConstraints;
import my.project.simulation.maps.IMap;
import my.project.simulation.utils.Vector2D;

import java.util.List;

public class FencedMapGridBuilder extends AbstractGridBuilder{
    protected static final String FENCE_TOP_PATH = "src/main/resources/images/fence/fence-top.jpg";
    protected static final String FENCE_LEFT_PATH = "src/main/resources/images/fence/fence-left.jpg";
    protected static final String FENCE_RIGHT_PATH = "src/main/resources/images/fence/fence-right.jpg";
    protected static final String FENCE_BOTTOM_PATH = "src/main/resources/images/fence/fence-bottom.jpg";
    protected static final String FENCE_TOP_LEFT_PATH = "src/main/resources/images/fence/fence-top-left.jpg";
    protected static final String FENCE_TOP_RIGHT_PATH = "src/main/resources/images/fence/fence-top-right.jpg";
    protected static final String FENCE_BOTTOM_LEFT_PATH = "src/main/resources/images/fence/fence-bottom-left.jpg";
    protected static final String FENCE_BOTTOM_RIGHT_PATH = "src/main/resources/images/fence/fence-bottom-right.jpg";

    private static final int FENCE_WIDTH = CELL_SIZE / 2;

    public FencedMapGridBuilder(IMap map, ScrollPane parentContainer) {
        super(map, parentContainer);
        // Store dimensions of a grid
        gridHeight = mapHeight + 3;
        gridWidth = mapWidth + 3;
    }

    public void buildGrid() {
        // Build grids
        buildMapGrid();
        buildWrapperGrid();
        // Add columns numbers
        addColumnsNumbers();
        // Add rows numbers
        addRowsNumbers();
        // Add grids to the wrapper grid
        wrapperGrid.add(mapGrid, 2, 1, mapWidth, mapHeight);
        // Load fence textures
        loadFenceTextures();
    }

    private void buildWrapperGrid() {
        // Create columns
        for (int i = 0; i < gridWidth; i++) {
            if (i == 1 || i == gridWidth - 1) wrapperGrid.getColumnConstraints().add(new ColumnConstraints(FENCE_WIDTH));
            else wrapperGrid.getColumnConstraints().add(new ColumnConstraints(CELL_SIZE));
        }
        // Create rows
        for (int i = 0; i < gridHeight; i++) {
            if (i == 0 || i == gridHeight - 2) wrapperGrid.getRowConstraints().add(new RowConstraints(FENCE_WIDTH));
            else wrapperGrid.getRowConstraints().add(new RowConstraints(CELL_SIZE));
        }
    }

    protected void addColumnsNumbers() {
        if (mapWidth % 2 == 1) {
            for (int i = 0; i < mapWidth; i++) addLabel(String.valueOf(i - mapWidth / 2), i + 2, gridHeight - 1);
        } else {
            for (int i = 0; i < mapWidth / 2; i++) addLabel(String.valueOf(i - mapWidth / 2), i + 2, gridHeight - 1);
            for (int i = mapWidth / 2; i < mapWidth; i++) addLabel(String.valueOf(i - mapWidth / 2 + 1),  i + 2, gridHeight - 1);
        }
    }

    protected void addRowsNumbers() {
        if (mapHeight % 2 == 1) {
            for (int i = 0; i < mapHeight; i++) addLabel(String.valueOf(i - mapHeight / 2), 0, mapHeight - i);
        } else {
            for (int i = 0; i < mapHeight / 2; i++) addLabel(String.valueOf(i - mapHeight / 2), 0, mapHeight - i);
            for (int i = mapHeight / 2; i < mapHeight; i++) addLabel(String.valueOf(i - mapHeight / 2 + 1),  0, mapHeight - i);
        }
    }

    @Override
    public void renderGrid() {
        super.renderGrid(2 * PADDING_SIZE + CELL_SIZE * (gridWidth - 2) + 2 * FENCE_WIDTH,
                2 * PADDING_SIZE + CELL_SIZE * (gridHeight - 2) + 2 * FENCE_WIDTH);
    }

    private void loadFenceTextures() {
        // Add horizontal fences
        for (int i = 2; i < gridWidth - 1; i++) {
            loadTexture(wrapperGrid, FENCE_BOTTOM_PATH, i, gridHeight - 2, CELL_SIZE, FENCE_WIDTH, null);
            loadTexture(wrapperGrid, FENCE_TOP_PATH, i, 0, CELL_SIZE, FENCE_WIDTH, null);
        }
        // Add vertical fences
        for (int i = 1; i < gridHeight - 2; i++) {
            loadTexture(wrapperGrid, FENCE_LEFT_PATH, 1, i, FENCE_WIDTH, CELL_SIZE, null);
            loadTexture(wrapperGrid, FENCE_RIGHT_PATH, gridWidth - 1, i, FENCE_WIDTH, CELL_SIZE, null);
        }
        // Corners
        loadTexture(wrapperGrid, FENCE_TOP_LEFT_PATH, 1, 0, FENCE_WIDTH, FENCE_WIDTH, null);
        loadTexture(wrapperGrid, FENCE_TOP_RIGHT_PATH, gridWidth - 1, 0, FENCE_WIDTH, FENCE_WIDTH, null);
        loadTexture(wrapperGrid, FENCE_BOTTOM_LEFT_PATH, 1, gridHeight - 2, FENCE_WIDTH, FENCE_WIDTH, null);
        loadTexture(wrapperGrid, FENCE_BOTTOM_RIGHT_PATH, gridWidth - 1, gridHeight - 2, FENCE_WIDTH, FENCE_WIDTH, null);
    }
}
