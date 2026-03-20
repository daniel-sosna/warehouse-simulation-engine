package lt.bananull.whse.simulator;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lt.bananull.whse.event.Event;
import lt.bananull.whse.event.EventHandler;
import lt.bananull.whse.event.events.BinRequestedAtPortEvent;
import lt.bananull.whse.event.events.RouterTickEvent;
import lt.bananull.whse.event.events.ShipmentIsReadyEvent;
import lt.bananull.whse.load.dto.SimulationStateDto;
import lt.bananull.whse.router.RouterClient;
import lt.bananull.whse.router.dto.AssignmentDto;
import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.entity.Port;
import lt.bananull.whse.simulator.entity.Shipment;
import lt.bananull.whse.simulator.entity.SimulationState;
import lt.bananull.whse.utils.RandomnessResolver;

import java.time.Instant;
import java.util.Collection;
import java.util.PriorityQueue;
import java.util.SplittableRandom;

@Slf4j
public class Simulator {

    private static final long DEFAULT_RANDOM_SEED = 1L;

    @Getter private final Instant simulationStartTime;
    @Getter private final long simulationDurationSeconds;

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
        this.state = SimulationState.from(initialState, parameters);
        this.simulationStartTime = startTime;
        this.now = startTime;
        this.simulationDurationSeconds = endTime.getEpochSecond() - simulationStartTime.getEpochSecond();
        this.parameters = parameters;
        this.eventHandler = new EventHandler(this);
        this.randomnessResolver = new RandomnessResolver(new SplittableRandom(DEFAULT_RANDOM_SEED));

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

    /**
     * Tries to find an available bin for the next queued shipment of the given port.
     * <ul>
     *   <li>If an available bin is found, {@link BinRequestedAtPortEvent} is scheduled.</li>
     *   <li>If no bin is currently available, the port is enqueued in every bin from the
     *       shipment's picks so that it will be notified when one becomes free.</li>
     * </ul>
     * The port must be {@link lt.bananull.whse.simulator.enums.PortStatus#IDLE} and have at
     * least one shipment in its queue when this method is called.
     *
     * @param gridId  grid that the port belongs to.
     * @param portId  port waiting for a bin.
     * @param simTime current simulation time to use when scheduling the event.
     */
    public void requestNextBinForPort(String gridId, String portId, long simTime) {
        Port port = state.getPort(gridId, portId);
        String nextShipmentId = port.peekNextShipmentId();
        if (nextShipmentId == null) return;

        Shipment shipment = state.getShipment(nextShipmentId);
        String binId = port.findAvailableBin(shipment, state::getBin);

        if (binId != null) {
            PickDto pick = shipment.getPicks().stream()
                    .filter(p -> p.binId().equals(binId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Bin %s not found in picks of shipment %s".formatted(binId, nextShipmentId)));
            enqueueEvent(new BinRequestedAtPortEvent(simTime, binId, gridId, portId, pick.ean(), pick.qty()));
        } else {
            for (PickDto pick : shipment.getPicks()) {
                state.getBin(pick.binId()).enqueuePort(gridId, portId);
            }
        }
    }
}
