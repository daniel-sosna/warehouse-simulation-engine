package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.event.events.ShipmentIsReadyEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.util.Collection;
import java.util.PriorityQueue;

@Slf4j
public class Simulator {

    @Getter private final Instant simulationStartTime;
    @Getter private final long simulationDurationSeconds;

    @Getter private long simTime = 0;
    @Getter private Instant now;
    @Getter private final SimulationState state;
    @Getter private final SimulationParameters parameters;

    private final PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();
    private final PriorityQueue<Event> events = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState,
                     Instant startTime, Instant endTime, SimulationParameters parameters) {
        this.state = SimulationState.from(initialState, parameters);
        this.simulationStartTime = startTime;
        this.now = startTime;
        this.simulationDurationSeconds = endTime.getEpochSecond() - simulationStartTime.getEpochSecond();
        this.parameters = parameters;

        enqueueEvent(new RouterTickEvent(0, routerClient));
    }

    public void enqueueEvent(Event e) {
        events.add(e);
    }

    public void updateAssignments(Collection<AssignmentDto> newAssignments) {
        assignments.clear();
        assignments.addAll(newAssignments);
        for (AssignmentDto assignment : assignments) {
            Shipment shipment = state.getShipment(assignment.shipmentId());
            shipment.routeToGrid(assignment.packingGrid());
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
        while (!events.isEmpty()) {
            Event e = events.poll();
            setSimTime(e.getSimTime());
            if (simTime > simulationDurationSeconds) break;
            EventHandler.getInstance(this).handle(e);
        }
    }
}
