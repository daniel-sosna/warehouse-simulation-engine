package lt.bananull.whse.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.utils.JacksonMapper;
import lt.bananull.whse.router.dto.RouterRequest;
import lt.bananull.whse.router.dto.RouterResponse;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RouterClient {

    private final List<String> command;
    private final ObjectMapper mapper;

    public RouterClient(String... command) {
        if (command == null || command.length == 0) {
            throw new IllegalArgumentException("Router command must not be empty");
        }

        this.command = List.of(command);
        this.mapper = JacksonMapper.create();
    }

    public RouterResponse route(RouterRequest request) {
        Process process = startProcess();

        try (BufferedWriter stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            String requestJson = mapper.writeValueAsString(request);

            stdin.write(requestJson);
            stdin.flush();
            stdin.close();

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Router process failed with exit code " + exitCode);
            }

            return mapper.readValue(process.getInputStream(), RouterResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Communication with router failed", e);
        }  finally {
            process.destroy();
        }
    }

    private Process startProcess() {
        try {
            ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
            pb.redirectErrorStream(false);
            return pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start router process: " + String.join(" ", command), e);
        }
    }
}
