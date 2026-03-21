package lt.bananull.whse.utils;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Utility class for sorting a simulation log file containing one JSON object per line by
 * {@code simTime} in ascending order. It uses an external natural merge sort algorithm:
 * already sorted runs are detected while scanning the file, written to temporary files,
 * and then merged into the final sorted result.
 *
 * <p>This approach is more memory-efficient than loading the entire file at once and keeps
 * each original log line unchanged. Lines without a numeric {@code simTime} are appended
 * to the end of the file in their original order.
 */
public final class LogFileSorter {

    private static final JsonFactory JSON_FACTORY = new JsonFactory();

    private LogFileSorter() {
        // Utility class
    }

    public static void sortSimulationLogBySimTime(Path filePath) throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        if (parent == null) {
            parent = Path.of(".");
        }

        List<Path> runFiles = new ArrayList<>();
        Path unsortableFile = Files.createTempFile(parent, "sim-log-unsortable-", ".tmp");
        Path outputFile = Files.createTempFile(parent, "sim-log-sorted-", ".tmp");

        try {
            boolean hasUnsortableLines = createNaturalRuns(filePath, parent, runFiles, unsortableFile);
            mergeRuns(runFiles, outputFile, unsortableFile, hasUnsortableLines);

            Files.move(outputFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(outputFile);
            Files.deleteIfExists(unsortableFile);

            for (Path runFile : runFiles) {
                Files.deleteIfExists(runFile);
            }
        }
    }

    private static boolean createNaturalRuns(
        Path inputPath,
        Path tempDirectory,
        List<Path> runFiles,
        Path unsortableFile
    ) throws IOException {
        BufferedWriter currentRunWriter = null;
        BufferedWriter unsortableWriter = null;

        Long previousSimTime = null;
        boolean hasUnsortableLines = false;

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            String line;

            while ((line = reader.readLine()) != null) {
                Long simTime = extractSimTime(line);

                if (simTime == null) {
                    if (unsortableWriter == null) {
                        unsortableWriter = Files.newBufferedWriter(unsortableFile, StandardCharsets.UTF_8);
                    }
                    unsortableWriter.write(line);
                    unsortableWriter.newLine();
                    hasUnsortableLines = true;
                    continue;
                }

                if (currentRunWriter == null || simTime < previousSimTime) {
                    if (currentRunWriter != null) {
                        currentRunWriter.close();
                    }

                    Path runFile = Files.createTempFile(tempDirectory, "sim-log-run-", ".tmp");
                    runFiles.add(runFile);
                    currentRunWriter = Files.newBufferedWriter(runFile, StandardCharsets.UTF_8);
                }

                writeRunEntry(currentRunWriter, simTime, line);
                previousSimTime = simTime;
            }
        } finally {
            if (currentRunWriter != null) {
                currentRunWriter.close();
            }
            if (unsortableWriter != null) {
                unsortableWriter.close();
            }
        }

        return hasUnsortableLines;
    }

    private static void mergeRuns(
        List<Path> runFiles,
        Path outputFile,
        Path unsortableFile,
        boolean hasUnsortableLines
    ) throws IOException {
        List<RunReader> readers = new ArrayList<>(runFiles.size());
        PriorityQueue<RunEntry> heap = new PriorityQueue<>(
            Comparator
                .comparingLong(RunEntry::simTime)
                .thenComparingInt(RunEntry::runIndex)
        );

        try {
            for (int i = 0; i < runFiles.size(); i++) {
                RunReader reader = new RunReader(i, runFiles.get(i));
                readers.add(reader);

                RunEntry firstEntry = reader.readNext();
                if (firstEntry != null) {
                    heap.add(firstEntry);
                }
            }

            try (BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
                while (!heap.isEmpty()) {
                    RunEntry entry = heap.poll();
                    writer.write(entry.rawLine());
                    writer.newLine();

                    RunEntry next = readers.get(entry.runIndex()).readNext();
                    if (next != null) {
                        heap.add(next);
                    }
                }

                if (hasUnsortableLines) {
                    appendFile(unsortableFile, writer);
                }
            }
        } finally {
            for (RunReader reader : readers) {
                reader.close();
            }
        }
    }

    private static void appendFile(Path sourceFile, BufferedWriter writer) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    private static void writeRunEntry(BufferedWriter writer, long simTime, String rawLine) throws IOException {
        writer.write(Long.toString(simTime));
        writer.write('\t');
        writer.write(rawLine);
        writer.newLine();
    }

    private static Long extractSimTime(String line) {
        try (JsonParser parser = JSON_FACTORY.createParser(line)) {
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                if (parser.currentToken() != JsonToken.FIELD_NAME) {
                    continue;
                }

                String fieldName = parser.currentName();
                JsonToken valueToken = parser.nextToken();

                if (!"simTime".equals(fieldName)) {
                    parser.skipChildren();
                    continue;
                }

                if (valueToken != null && valueToken.isNumeric()) {
                    return parser.getLongValue();
                }

                if (valueToken == JsonToken.VALUE_STRING) {
                    String text = parser.getText();
                    if (text != null) {
                        try {
                            return Long.parseLong(text.trim());
                        } catch (NumberFormatException ignored) {
                            return null;
                        }
                    }
                }

                return null;
            }
        } catch (Exception ignored) {
            return null;
        }

        return null;
    }

    private record RunEntry(int runIndex, long simTime, String rawLine) {
    }

    private static final class RunReader implements AutoCloseable {
        private final int runIndex;
        private final BufferedReader reader;

        private RunReader(int runIndex, Path file) throws IOException {
            this.runIndex = runIndex;
            this.reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
        }

        private RunEntry readNext() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            int separatorIndex = line.indexOf('\t');
            if (separatorIndex < 0) {
                throw new IOException("Corrupted temporary run entry: " + line);
            }

            long simTime = Long.parseLong(line.substring(0, separatorIndex));
            String rawLine = line.substring(separatorIndex + 1);

            return new RunEntry(runIndex, simTime, rawLine);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }
}
