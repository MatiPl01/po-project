package my.project.simulation.stats;

import javafx.application.Platform;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StatsMeter {
    private static final String DEFAULT_PATH = "./output/";
    private static final String ERROR_MESSAGE = "Error writing statistics to the file";

    private final String defaultName;
    private Path tempFile;

    private final List<StatsRecord> dailyStatistics = new ArrayList<>();
    private StatsRecord statsSum = new StatsRecord(0, 0, 0, 0, 0);
    private int savesCount = 0;

    public StatsMeter(String defaultName) {
        this.defaultName = defaultName;

        try {
            this.tempFile = Files.createTempFile("stats", ".tmp");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateStatistics(long aliveAnimalsCount,
                                 long diedAnimalsCount,
                                 long plantsCount,
                                 Set<List<Integer>> dominantGenomes,
                                 double averageAliveEnergy,
                                 double averageDiedLifespan,
                                 double averageAliveChildren) {
        // TODO - send updates to GUI components (to implement)

        // Save daily statistics
        StatsRecord record = new StatsRecord(aliveAnimalsCount,
                                             plantsCount,
                                             averageAliveEnergy,
                                             averageDiedLifespan,
                                             averageAliveChildren);

        // Add a record to the dailyStatistics if cannot write to the temporary file
        if (tempFile == null || !writeToTempFile(record)) dailyStatistics.add(record);

        // Update sums of statistic which will be used to calculate an average
        statsSum = statsSum.add(record);
        savesCount++;
    }

    public void updateTrackedAnimalDeath(long dayNum) {
        // TODO - send updates to GUI components (to implement)
    }

    public void updateTrackedAnimalDescendants(long descendantsCount) {
        // TODO - send updates to GUI components (to implement)
    }

    public void updateTrackedAnimalChildren(int childrenCount) {
        // TODO - send updates to GUI components (to implement)
    }

    private boolean writeToTempFile(StatsRecord record) {
        try {
            Files.write(tempFile, new ArrayList<>() {{ add(record.toString()); }}, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
            // Return false if operation failed
            return false;
        }
        return true;
    }

    private void writeLinesFromTempFile(BufferedWriter bw) throws IOException {
        Files.lines(tempFile, StandardCharsets.UTF_8).forEach(line -> {
            try {
                bw.write(line + "\n");
            } catch (IOException e) {
                System.out.println(ERROR_MESSAGE);
                e.printStackTrace();
            } finally {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    System.out.println("Cannot delete a temporary file");
                    e.printStackTrace();
                }
            }
        });
    }

    private void writeLinesFromArray(BufferedWriter bw) throws IOException {
        for (StatsRecord record: dailyStatistics) bw.write(record.toString() + "\n");
    }

    private void writeAverageStats(BufferedWriter bw) throws IOException {
        String sep = statsSum.getSeparator();
        bw.write(1. * statsSum.aliveAnimalsCount() / savesCount + sep +
                1. * statsSum.plantsCount() / savesCount + sep +
                statsSum.averageAliveEnergy() / savesCount + sep +
                statsSum.averageDiedLifespan() / savesCount + sep +
                statsSum.averageAliveChildren() / savesCount);
    }

    public void generateCSVFile() {
        Platform.runLater(() -> {
            String outputPath = DEFAULT_PATH + defaultName;
            System.out.println("Generating statistics CSV file in: " + outputPath);

            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(outputPath));
                // If statistics were stored in a temporary file
                if (dailyStatistics.size() == 0 && tempFile != null) writeLinesFromTempFile(bw);
                // If statistics were stored in a dailyStatistics list
                else writeLinesFromArray(bw);
                writeAverageStats(bw);
                bw.close();
                System.out.println("Statistics file: " + outputPath + " was successfully generated");
            } catch (IOException e) {
                System.out.println(ERROR_MESSAGE);
                e.printStackTrace();
            }
        });
    }
}
