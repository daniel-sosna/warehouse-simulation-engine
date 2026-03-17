package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.BinArrivesAtPort;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.simulator.entity.SimulationState;

import java.time.Instant;
import java.util.Collection;
import java.util.PriorityQueue;

@Slf4j
public class Simulator {

    private final Instant simulationStartTime;
    private final Instant simulationEndTime;
    @Getter private final long simulationDurationSeconds;

    @Getter private long simTime = 0;
    @Getter private Instant now;
    @Getter private final SimulationState state;
    @Getter private final SimulationParameters parameters;

    private final PriorityQueue<AssignmentDto> assignments = new PriorityQueue<>();
    private final PriorityQueue<Event> events = new PriorityQueue<>();

    public Simulator(RouterClient routerClient, SimulationStateDto initialState, Instant startTime, Instant endTime, SimulationParameters parameters) {
        this.state = SimulationState.from(initialState);
        this.simulationStartTime = startTime;
        this.simulationEndTime = endTime;
        this.now = startTime;
        this.simulationDurationSeconds = simulationEndTime.getEpochSecond() - simulationStartTime.getEpochSecond();
        this.parameters = parameters;

        enqueueEvent(new RouterTickEvent(0, routerClient));
    }

    public void enqueueEvent(Event e) {
        events.add(e);
    }

    public void updateAssignments(Collection<AssignmentDto> newAssignments) {
        assignments.clear();
        assignments.addAll(newAssignments);
    }

    private void setSimTime(long newSimTimeSeconds) {
        this.simTime = newSimTimeSeconds;
        this.now = simulationStartTime.plusSeconds(simTime);
        log.info("Time is: {}", simTime);
    }

    public void dispatchAll() {
        while (!assignments.isEmpty()) {
            AssignmentDto a = assignments.poll();

            Integer deliverySeconds = parameters.gridBinDelivery().deliveryTimes().get(a.packingGrid());
            if (deliverySeconds == null) {
                throw new IllegalArgumentException("No delivery time configured for grid: " + a.packingGrid());
            }
            long doneAt = simTime + deliverySeconds;
            enqueueEvent(new BinArrivesAtPort(doneAt, a));

            // TODO: later (deffo not now) we should create a dispacher/scheduler for the logic
            // then we will need: Dispatcher (or PortScheduler) that:
            // - takes AssignmentDto
            // - decides which port/grid can start now
            // - reserves bin/port
            // - schedules BinArrivedAtPort, BinPickCompleted, etc.
        }
    }

    public void run() {
        EventHandler eventHandler = new EventHandler(this);

        while (!events.isEmpty()) {
            Event e = events.poll();
            setSimTime(e.getSimTime());
            if (now.isAfter(simulationEndTime)) break;
            eventHandler.handle(e);
        }
    }
}
