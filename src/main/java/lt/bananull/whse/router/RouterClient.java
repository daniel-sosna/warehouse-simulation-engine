package lt.bananull.whse.router;

import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.json.JacksonMapper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class RouterClient {

    private final Process process;
    private final BufferedWriter stdin;
    private final BufferedReader stdout;
    private final ObjectMapper mapper;

    public RouterClient(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command.split(" "));
            pb.redirectErrorStream(false);
            this.process = pb.start();
            this.stdin  = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            this.stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
        } catch (IOException e) {
            throw new RuntimeException("Failed to start router process: " + command, e);
        }

        this.mapper = JacksonMapper.create();
    }

    public RouterResponse route(RouterRequest request) {
        try {
            String json = mapper.writeValueAsString(request);

            stdin.write(json);
            stdin.newLine();
            stdin.flush();

            String response = stdout.readLine();

            if (response == null) {
                throw new RuntimeException("Router process closed stdout unexpectedly. Process alive: " + process.isAlive());
            }

            if (response.isBlank()) {
                throw new RuntimeException("Router returned empty response");
            }

            return mapper.readValue(response, RouterResponse.class);
        } catch (IOException e) {
            throw new RuntimeException("Communication with router failed", e);
        }
    }

    public void close() {
        try { stdin.close();  } catch (IOException ignored) {}
        try { stdout.close(); } catch (IOException ignored) {}
        if (process.isAlive()) process.destroy();
    }
}
