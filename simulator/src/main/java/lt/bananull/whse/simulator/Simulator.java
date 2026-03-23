package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.PortOpensEvent;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.event.events.TruckArrivalEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.service.PortShiftService;
import lt.bananull.whse.service.TruckArrivalService;
import lt.bananull.whse.simulator.entity.Bin;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.utils.DateTimeResolver;
import lt.bananull.whse.utils.RandomnessResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

@Slf4j
public class Simulator {

    private static final long DEFAULT_RANDOM_SEED = 1L;

    @Getter private final Instant simulationStart;
    @Getter private final long simulationDurationSeconds;
    @Getter private final ZoneId zoneId = ZoneId.of("UTC"); // for now just hardcoded the zone

    @Getter private volatile long simTime = 0;
    @Getter private final SimulationState state;
    @Getter private final SimulationParameters parameters;
    @Getter private final EventHandler eventHandler;

    private final RandomnessResolver randomnessResolver;
    private final PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();
    private final PriorityQueue<Event> events = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState, SimulationParameters parameters) {
        this.parameters = parameters;
        this.simulationStart = parameters.simulationStartTime();
        this.simulationDurationSeconds = parameters.simulationEndTime().getEpochSecond() - parameters.simulationStartTime().getEpochSecond();
        this.eventHandler = new EventHandler(this);
        this.randomnessResolver = new RandomnessResolver(new SplittableRandom(DEFAULT_RANDOM_SEED));

        this.state = SimulationState.from(initialState, parameters, parameters.simulationStartTime(), parameters.simulationEndTime(),
            zoneId);

        enqueueEvent(new RouterTickEvent(0, routerClient));
    }

    public void enqueueEvent(Event e) {
        if (e.getSimTime() <= simulationDurationSeconds) {
            if (e.getSimTime() < simTime) {
                log.warn("Enqueueing event in the past: nowSimTime={}, eventSimTime={}, event={}",
                    simTime, e.getSimTime(), e);
            }
            events.add(e);
        }
    }

    public void updateAssignments(Collection<AssignmentDto> newAssignments) {
        assignments.clear();
        assignments.addAll(newAssignments);
    }

    public List<AssignmentDto> pollAssignmentsToDispatch() {
        List<AssignmentDto> assignmentsToDispatch = new ArrayList<>();
        Map<String, String> neededBinsInGrids = new HashMap<>();

        while (!assignments.isEmpty()) {
            AssignmentDto assignmentDto = assignments.peek();
            String packingGrid = assignmentDto.packingGrid();

            List<Bin> requiredBins = assignmentDto.picks().stream()
                .map(PickDto::binId)
                .distinct()
                .map(state::getBin)
                .toList();

            boolean allBinsAreAvailable = true;
            for (Bin bin : requiredBins) {
                if (
                    (!neededBinsInGrids.containsKey(bin.getId()) || neededBinsInGrids.get(bin.getId()).equals(packingGrid))
                    && (!bin.isNeededInCurrentGrid() || bin.getCurrentGridId().equals(packingGrid))
                ) {
                    neededBinsInGrids.put(bin.getId(), packingGrid);
                } else {
                    allBinsAreAvailable = false;
                    break;
                }
            }

            if (!allBinsAreAvailable) {
                break;
            }

            assignments.poll();
            assignmentsToDispatch.add(assignmentDto);
        }

        return assignmentsToDispatch;
    }

    public void run() {
        enqueueTruckEvents();
        startPorts();

        while (!events.isEmpty()) {
            Event e = events.poll();
            long eventSimTime = e.getSimTime();
            if (simTime != eventSimTime) {
                simTime = eventSimTime;
            }
            if (simTime > simulationDurationSeconds) break;
            eventHandler.handle(e);
        }
    }

    public double resolveMultiplier(SimulationParameters.Randomness randomness) {
        return randomnessResolver.resolveMultiplier(randomness);
    }

    public HealthData getHealthData() {
        return new HealthData(
            simTime,
            String.format("%dd %02d:%02d", simTime / (24 * 3600), simTime % (24 * 3600) / 3600, simTime % 3600 / 60),
            simulationDurationSeconds == 0 ? 100.0 : simTime * 100.0 / simulationDurationSeconds,
            events.size()
        );
    }

    public record HealthData(long simTimeSeconds, String simTimeReadable, double progressPercent,
                             int scheduledEvents) {}

    private void enqueueTruckEvents() {
        List<TruckArrivalEvent> truckEvents = TruckArrivalService.generateTruckArrivalEvents(parameters.simulationStartTime(),
            parameters.simulationEndTime(),
            parameters.truckArrivalSchedules());
        truckEvents.forEach(this::enqueueEvent);
    }

    private void startPorts() {
        for (Grid grid : state.grids().values()) {
            String gridId = grid.getId();
            for (String portId : grid.getPorts().keySet()) {
                Shift shift = PortShiftService.findCurrentOrNextShift(state.getGrid(gridId), portId,
                    simulationStart.plusSeconds(simTime));

                long openAt = 0;
                try {
                    openAt = DateTimeResolver.resolveSimTimeFromTimestamp(shift.getStartAt()
                        , parameters.simulationStartTime());
                } catch (IllegalArgumentException e) {
                    // IllegalArgumentException thrown when getStartAt is older than simulation start time meaning
                    // port was already running when simulation started
                } finally {
                    enqueueEvent(new PortOpensEvent(openAt, gridId, portId, false));
                }
            };
        };
    }
}
