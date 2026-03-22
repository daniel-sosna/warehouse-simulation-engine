package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.BinRequestedAtPortEvent;
import lt.bananull.whse.event.events.PortOpensEvent;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.event.events.ShipmentIsReadyEvent;
import lt.bananull.whse.event.events.TruckArrivalEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.service.BinReservationService;
import lt.bananull.whse.service.PortShiftService;
import lt.bananull.whse.service.TruckArrivalService;
import lt.bananull.whse.simulator.entity.Grid;
import lt.bananull.whse.simulator.entity.Shift;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.utils.DateTimeResolver;
import lt.bananull.whse.utils.RandomnessResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.SplittableRandom;

@Slf4j
public class Simulator {

    private static final long DEFAULT_RANDOM_SEED = 1L;

    @Getter private final Instant simulationStart;
    @Getter private final long simulationDurationSeconds;
    @Getter private final ZoneId zoneId = ZoneId.of("UTC"); // for now just hardcoded the zone

    @Getter private long simTime = 0;
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
        for (AssignmentDto assignment : assignments) {
            Shipment shipment = state.getShipment(assignment.shipmentId());
            shipment.routeToGrid(assignment.packingGrid(), assignment.picks());
        }
    }

    public void dispatchAll() {
        while (!assignments.isEmpty()) {
            AssignmentDto a = assignments.poll();
            Set<String> binIds = a.picks().stream()
                .map(PickDto::binId)
                .collect(java.util.stream.Collectors.toSet());
            if (!BinReservationService.canReserveAllPicks(this, binIds)) {
                continue; // leave it for next router tick
            } else {
                BinReservationService.reserveAllPicks(this, a.shipmentId(), binIds);
            }
            enqueueEvent(new ShipmentIsReadyEvent(simTime, a.shipmentId()));

        }
    }

    public void run() {
        System.out.println("Simulator started");
        enqueueTruckEvents();
        startPorts();

        while (!events.isEmpty()) {
            Event e = events.poll();
            long eventSimTime = e.getSimTime();
            if (simTime != eventSimTime) {
                if (eventSimTime % (3600 * 6) == 0) {
                    System.out.printf("Sim time: %s hours\t| scheduled events: %d\t| progress: ~%.1f%%%n",
                        eventSimTime / 3600, events.size(), simTime * 100.0 / simulationDurationSeconds);
                }
                simTime = eventSimTime;
            }
            if (simTime > simulationDurationSeconds) break;
            eventHandler.handle(e);
        }
        System.out.println("Simulator finished");
    }

    public double resolveMultiplier(SimulationParameters.Randomness randomness) {
        return randomnessResolver.resolveMultiplier(randomness);
    }

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
