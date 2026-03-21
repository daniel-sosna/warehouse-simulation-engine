package lt.bananull.whse.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.router.dto.RouterRequestDto;
import lt.bananull.whse.router.dto.RouterResponseDto;
import lt.bananull.whse.utils.JacksonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

    public RouterResponseDto route(RouterRequestDto request) {
        Process process = startProcess();

        try {
            // Stream JSON directly into child stdin
            try (OutputStream out = process.getOutputStream()) {
                mapper.writeValue(out, request);
                out.flush();
            }

            // Stream JSON directly from child stdout
            RouterResponseDto response;
            try (InputStream in = process.getInputStream()) {
                response = mapper.readValue(in, RouterResponseDto.class);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Router failed, exit=" + exitCode);
            }

            return response;
        } catch (IOException e) {
            throw new RuntimeException("Communication with router failed", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Communication with router was interrupted", e);
        } finally {
            process.destroy();
        }
    }

    private Process startProcess() {
        try {
            ProcessBuilder pb = new ProcessBuilder(new ArrayList<>(command));
            pb.redirectErrorStream(true);
            return pb.start();
        } catch (IOException e) {
            throw new RuntimeException("Failed to start router process: " + String.join(" ", command), e);
        }
    }
}
