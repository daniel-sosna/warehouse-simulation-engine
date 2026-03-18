package lt.bananull.whse.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.event.LogEvent;

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
 * {@code simTime} in ascending order. It uses an external merge sort algorithm: the file is
 * read in small chunks, each chunk is sorted and written to a temporary file, and the temporary
 * files are then merged into the final sorted result.
 * <p>
 * This approach is more memory-efficient than loading the entire file at once and replaces
 * the original file with the sorted output.
 */
public final class LogFileSorter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Tune this based on your available heap and average line size.
     * Larger chunk = fewer temp files, but more memory usage.
     */
    private static final int CHUNK_SIZE = 50_000;

    private LogFileSorter() {
    }

    public static void sortSimulationLogBySimTime(Path filePath) throws IOException {
        Path parent = filePath.toAbsolutePath().getParent();
        if (parent == null) {
            parent = Path.of(".");
        }

        List<Path> chunkFiles = new ArrayList<>();
        Path tempOutput = Files.createTempFile(parent, "sorted-sim-log-", ".log");

        try {
            createSortedChunks(filePath, parent, chunkFiles);
            mergeSortedChunks(chunkFiles, tempOutput);

            Files.move(
                tempOutput,
                filePath,
                StandardCopyOption.REPLACE_EXISTING
            );
        } finally {
            Files.deleteIfExists(tempOutput);

            for (Path chunkFile : chunkFiles) {
                Files.deleteIfExists(chunkFile);
            }
        }
    }

    private static void createSortedChunks(Path inputPath, Path parent, List<Path> chunkFiles) throws IOException {
        List<LogEvent> chunk = new ArrayList<>(CHUNK_SIZE);

        try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.isBlank()) {
                    continue;
                }

                try {
                    chunk.add(OBJECT_MAPPER.readValue(line, LogEvent.class));
                } catch (Exception e) {
                    throw new IOException("Failed to parse JSON on line " + lineNumber + ": " + line, e);
                }

                if (chunk.size() >= CHUNK_SIZE) {
                    chunkFiles.add(writeSortedChunk(chunk, parent));
                    chunk.clear();
                }
            }
        }

        if (!chunk.isEmpty()) {
            chunkFiles.add(writeSortedChunk(chunk, parent));
        }
    }

    private static Path writeSortedChunk(List<LogEvent> chunk, Path parent) throws IOException {
        chunk.sort(Comparator.comparingLong(LogEvent::simTime));

        Path chunkFile = Files.createTempFile(parent, "sim-log-chunk-", ".log");

        try (BufferedWriter writer = Files.newBufferedWriter(chunkFile, StandardCharsets.UTF_8)) {
            for (LogEvent event : chunk) {
                writer.write(OBJECT_MAPPER.writeValueAsString(event));
                writer.newLine();
            }
        }

        return chunkFile;
    }

    private static void mergeSortedChunks(List<Path> chunkFiles, Path outputPath) throws IOException {
        List<ChunkReader> readers = new ArrayList<>();
        PriorityQueue<ChunkEntry> queue = new PriorityQueue<>(
            Comparator
                .comparingLong((ChunkEntry e) -> e.event().simTime())
                .thenComparingInt(ChunkEntry::chunkIndex)
        );

        try {
            for (int i = 0; i < chunkFiles.size(); i++) {
                ChunkReader reader = new ChunkReader(i, chunkFiles.get(i));
                readers.add(reader);

                ChunkEntry first = reader.readNext();
                if (first != null) {
                    queue.add(first);
                }
            }

            try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                while (!queue.isEmpty()) {
                    ChunkEntry current = queue.poll();

                    writer.write(current.rawLine());
                    writer.newLine();

                    ChunkEntry next = readers.get(current.chunkIndex()).readNext();
                    if (next != null) {
                        queue.add(next);
                    }
                }
            }
        } finally {
            IOException closeException = null;

            for (ChunkReader reader : readers) {
                try {
                    reader.close();
                } catch (IOException e) {
                    if (closeException == null) {
                        closeException = e;
                    } else {
                        closeException.addSuppressed(e);
                    }
                }
            }

            if (closeException != null) {
                throw closeException;
            }
        }
    }

    private static final class ChunkReader implements AutoCloseable {
        private final int chunkIndex;
        private final BufferedReader reader;

        private ChunkReader(int chunkIndex, Path path) throws IOException {
            this.chunkIndex = chunkIndex;
            this.reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
        }

        private ChunkEntry readNext() throws IOException {
            String line = reader.readLine();
            if (line == null) {
                return null;
            }

            LogEvent event;
            try {
                event = OBJECT_MAPPER.readValue(line, LogEvent.class);
            } catch (Exception e) {
                throw new IOException("Failed to parse temp chunk line: " + line, e);
            }

            return new ChunkEntry(chunkIndex, line, event);
        }

        @Override
        public void close() throws IOException {
            reader.close();
        }
    }

    private record ChunkEntry(int chunkIndex, String rawLine, LogEvent event) {
    }
}
