package lt.bananull.whse.load;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lt.bananull.whse.dto.dataset.GridDto;
import lt.bananull.whse.dto.dataset.PortDto;
import lt.bananull.whse.dto.dataset.ShiftDto;
import lt.bananull.whse.json.JacksonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestParse {
    public static void main(String[] args) throws Exception {
        ObjectMapper om = JacksonMapper.create();

        try (var in = Files.newInputStream(Path.of("data/1/grids.json"))) {
            List<GridDto> grids = om.readValue(in, new TypeReference<>() {});
            System.out.println("Loaded grids: " + grids.size());
            for (GridDto grid : grids) {
                System.out.println(grid.id());
                for (ShiftDto shift : grid.shifts()) {
                    System.out.println(shift.start());
                    System.out.println(shift.end());
                    for (PortDto port : shift.portConfig()) {
                        System.out.println(port.id());
                        System.out.println(port.handlingFlags());
                    }
                }
            }
        }
    }
}