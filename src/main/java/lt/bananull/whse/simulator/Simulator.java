package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.event.events.ShipmentIsReadyEvent;
import lt.bananull.whse.event.events.TruckArrivalEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.service.TruckArrivalService;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.utils.RandomnessResolver;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

@Slf4j
public class Simulator {

    private static final long DEFAULT_RANDOM_SEED = 1L;

    @Getter private final Instant simulationStartTime;
    @Getter private final Instant simulationEndTime;
    @Getter private final long simulationDurationSeconds;
    @Getter private final ZoneId zoneId = ZoneId.of("UTC"); // for now just hardcoded the zone

    @Getter private long simTime = 0;
    @Getter private Instant now;
    @Getter private final SimulationState state;
    @Getter private final SimulationParameters parameters;
    @Getter private final EventHandler eventHandler;

    private final RandomnessResolver randomnessResolver;
    private final PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();
    private final PriorityQueue<Event> events = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState,
                     Instant startTime, Instant endTime, SimulationParameters parameters) {
        this.simulationStartTime = startTime;
        this.simulationEndTime = endTime;
        this.now = startTime;
        this.simulationDurationSeconds = endTime.getEpochSecond() - simulationStartTime.getEpochSecond();
        this.parameters = parameters;
        this.eventHandler = new EventHandler(this);
        this.randomnessResolver = new RandomnessResolver(new SplittableRandom(DEFAULT_RANDOM_SEED));

        this.state = SimulationState.from(initialState, parameters, getSimulationStartTime(), getSimulationEndTime(),
            zoneId);

        enqueueEvent(new RouterTickEvent(0, routerClient));
    }

    public void enqueueEvent(Event e) {
        if (e.getSimTime() < simTime) {
            log.warn("Enqueueing event in the past: nowSimTime={}, eventSimTime={}, event={}",
                simTime, e.getSimTime(), e);
        }
        events.add(e);
    }

    public void updateAssignments(Collection<AssignmentDto> newAssignments) {
        assignments.clear();
        assignments.addAll(newAssignments);
        for (AssignmentDto assignment : assignments) {
            Shipment shipment = state.getShipment(assignment.shipmentId());
            shipment.routeToGrid(assignment.packingGrid(), assignment.picks());
        }
    }

    private void setSimTime(long newSimTimeSeconds) {
        this.simTime = newSimTimeSeconds;
        this.now = simulationStartTime.plusSeconds(simTime);
    }

    public void dispatchAll() {
        while (!assignments.isEmpty()) {
            AssignmentDto a = assignments.poll();

            enqueueEvent(new ShipmentIsReadyEvent(simTime, a.shipmentId()));
            // long doneAt = simTime + TRAVEL_SECONDS;
            // enqueueEvent(new BinArrivesAtPort(doneAt, a));

            // TODO: later (deffo not now) we should create a dispacher/scheduler for the logic
            // then we will need: Dispatcher (or PortScheduler) that:
            // - takes AssignmentDto
            // - decides which port/grid can start now
            // - reserves bin/port
            // - schedules BinArrivedAtPort, BinPickCompleted, etc.
        }
    }

    public void run() {
        enqueueTruckEvents();

        while (!events.isEmpty()) {
            Event e = events.poll();
            setSimTime(e.getSimTime());
            if (simTime > simulationDurationSeconds) break;
            eventHandler.handle(e);
        }
    }

    public double resolveMultiplier(SimulationParameters.Randomness randomness) {
        return randomnessResolver.resolveMultiplier(randomness);
    }

    private void enqueueTruckEvents() {
        List<TruckArrivalEvent> truckEvents = TruckArrivalService.generateTruckArrivalEvents(simulationStartTime,
            simulationEndTime,
            parameters.truckArrivalSchedules());
        truckEvents.forEach(this::enqueueEvent);
    }
}
