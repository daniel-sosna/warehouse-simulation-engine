import {
  DerivedReplayState,
  EventGroup,
  GridState,
  MetricsSnapshot,
  NormalizedEvent,
  PortState,
  ReplayArtifacts,
  ReplayCheckpoint,
  ShipmentState,
} from "./types";

const CHECKPOINT_INTERVAL = 100;

function cloneMetrics(metrics: MetricsSnapshot): MetricsSnapshot {
  return { ...metrics };
}

function cloneShipments(
  shipments: Record<string, ShipmentState>,
): Record<string, ShipmentState> {
  return Object.fromEntries(
    Object.entries(shipments).map(([id, shipment]) => [
      id,
      {
        ...shipment,
        pickedBinIds: [...shipment.pickedBinIds],
        eventIndices: [...shipment.eventIndices],
      },
    ]),
  );
}

function clonePorts(ports: DerivedReplayState["portsById"]) {
  return Object.fromEntries(
    Object.entries(ports).map(([id, port]) => [id, { ...port, handlingFlags: [...port.handlingFlags] }]),
  );
}

function cloneBins(bins: DerivedReplayState["binsById"]) {
  return Object.fromEntries(Object.entries(bins).map(([id, bin]) => [id, { ...bin }]));
}

function cloneGrids(grids: Record<string, GridState>): Record<string, GridState> {
  return Object.fromEntries(
    Object.entries(grids).map(([id, grid]) => [
      id,
      {
        ...grid,
        portIds: [...grid.portIds],
      },
    ]),
  );
}

export function createInitialReplayState(): DerivedReplayState {
  return {
    currentEventIndex: -1,
    currentGroupIndex: -1,
    currentSimTime: null,
    currentTimestamp: null,
    shipmentsById: {},
    portsById: {},
    binsById: {},
    gridsById: {},
    metrics: {
      eventsProcessed: 0,
      groupsProcessed: 0,
      shipmentsReceived: 0,
      shipmentsReady: 0,
      shipmentsPacked: 0,
      shipmentsShipped: 0,
      truckArrivals: 0,
    },
    recentChanges: [],
  };
}

export function cloneReplayState(state: DerivedReplayState): DerivedReplayState {
  return {
    currentEventIndex: state.currentEventIndex,
    currentGroupIndex: state.currentGroupIndex,
    currentSimTime: state.currentSimTime,
    currentTimestamp: state.currentTimestamp,
    shipmentsById: cloneShipments(state.shipmentsById),
    portsById: clonePorts(state.portsById),
    binsById: cloneBins(state.binsById),
    gridsById: cloneGrids(state.gridsById),
    metrics: cloneMetrics(state.metrics),
    recentChanges: [...state.recentChanges],
  };
}

function getString(value: unknown): string | null {
  return typeof value === "string" && value.trim() ? value : null;
}

function getStringArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }

  return value.filter((item): item is string => typeof item === "string");
}

function ensureShipment(
  state: DerivedReplayState,
  shipmentId: string,
): ShipmentState {
  if (!state.shipmentsById[shipmentId]) {
    state.shipmentsById[shipmentId] = {
      id: shipmentId,
      status: "unknown",
      receivedAtSimTime: null,
      readyAtSimTime: null,
      packedAtSimTime: null,
      shippedAtSimTime: null,
      activePortId: null,
      gridId: null,
      pickedBinIds: [],
      eventIndices: [],
    };
  }

  return state.shipmentsById[shipmentId];
}

function ensurePort(state: DerivedReplayState, portId: string): PortState {
  if (!state.portsById[portId]) {
    state.portsById[portId] = {
      id: portId,
      gridId: null,
      status: "unknown",
      handlingFlags: [],
      activeShipmentId: null,
      activeBinId: null,
      lastEventIndex: null,
      lastEventType: null,
      lastTransition: null,
    };
  }

  return state.portsById[portId];
}

function ensureGrid(state: DerivedReplayState, gridId: string): GridState {
  if (!state.gridsById[gridId]) {
    state.gridsById[gridId] = {
      id: gridId,
      portIds: [],
      lastEventIndex: null,
    };
  }

  return state.gridsById[gridId];
}

function ensureBin(state: DerivedReplayState, binId: string) {
  if (!state.binsById[binId]) {
    state.binsById[binId] = {
      id: binId,
      gridId: null,
      status: "unknown",
      portId: null,
      shipmentId: null,
      lastEventIndex: null,
    };
  }

  return state.binsById[binId];
}

function linkPortToGrid(
  state: DerivedReplayState,
  portId: string,
  gridId: string,
  eventIndex: number,
) {
  const port = ensurePort(state, portId);
  const grid = ensureGrid(state, gridId);

  port.gridId = gridId;
  port.lastEventIndex = eventIndex;
  grid.lastEventIndex = eventIndex;

  if (!grid.portIds.includes(portId)) {
    grid.portIds.push(portId);
  }
}

export function applyEventToState(
  previousState: DerivedReplayState,
  event: NormalizedEvent,
): DerivedReplayState {
  const state = cloneReplayState(previousState);
  const data = event.data;
  const changes: string[] = [];
  const shipmentId = getString(data.shipmentId);
  const portId = getString(data.portId);
  const gridId = getString(data.gridId);
  const binId = getString(data.binId);

  state.currentEventIndex = event.index;
  state.currentGroupIndex = event.groupIndex;
  state.currentSimTime = event.simTime;
  state.currentTimestamp = event.timestamp;
  state.metrics.eventsProcessed += 1;
  state.metrics.groupsProcessed = event.groupIndex + 1;

  switch (event.eventKey) {
    case "ShipmentReceived": {
      if (shipmentId) {
        const shipment = ensureShipment(state, shipmentId);
        shipment.status = "received";
        shipment.receivedAtSimTime = event.simTime;
        shipment.eventIndices.push(event.index);
        state.metrics.shipmentsReceived += 1;
        changes.push(`Shipment ${shipmentId} received`);
      } else {
        changes.push("Shipment received with missing shipmentId");
      }
      break;
    }
    case "ShipmentIsReady": {
      if (shipmentId) {
        const shipment = ensureShipment(state, shipmentId);
        shipment.status = "ready";
        shipment.readyAtSimTime = event.simTime;
        shipment.eventIndices.push(event.index);
        state.metrics.shipmentsReady += 1;
        changes.push(`Shipment ${shipmentId} ready`);
      } else {
        changes.push("Shipment ready with missing shipmentId");
      }
      break;
    }
    case "PortStartsShipment": {
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "busy";
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
        port.lastTransition = "started-shipment";
        if (gridId) {
          linkPortToGrid(state, portId, gridId, event.index);
        }
        changes.push(`Port ${portId} started a shipment`);
      } else {
        changes.push("Port started shipment with missing portId");
      }
      break;
    }
    case "BinRequestedAtPort": {
      if (binId) {
        const bin = ensureBin(state, binId);
        bin.status = "requested";
        bin.portId = portId;
        bin.lastEventIndex = event.index;
        if (gridId) {
          bin.gridId = gridId;
        }
      }
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "waiting-bin";
        port.activeBinId = binId;
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
      }
      changes.push(
        binId && portId
          ? `Port ${portId} requested bin ${binId}`
          : "Bin requested at port",
      );
      break;
    }
    case "BinArrivesAtPort": {
      if (binId) {
        const bin = ensureBin(state, binId);
        bin.status = "at-port";
        bin.portId = portId;
        bin.lastEventIndex = event.index;
        if (gridId) {
          bin.gridId = gridId;
        }
      }
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "busy";
        port.activeBinId = binId;
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
      }
      changes.push(
        binId && portId
          ? `Bin ${binId} arrived at port ${portId}`
          : "Bin arrived at port",
      );
      break;
    }
    case "BinPickCompleted": {
      if (shipmentId) {
        const shipment = ensureShipment(state, shipmentId);
        shipment.status = "picking";
        shipment.activePortId = portId;
        shipment.gridId = gridId;
        shipment.eventIndices.push(event.index);
        if (binId && !shipment.pickedBinIds.includes(binId)) {
          shipment.pickedBinIds.push(binId);
        }
      }
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "busy";
        port.activeShipmentId = shipmentId;
        port.activeBinId = binId;
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
      }
      if (binId) {
        const bin = ensureBin(state, binId);
        bin.status = "available";
        bin.portId = null;
        bin.shipmentId = shipmentId;
        bin.lastEventIndex = event.index;
        if (gridId) {
          bin.gridId = gridId;
        }
      }
      changes.push(
        shipmentId
          ? `Bin pick completed for shipment ${shipmentId}`
          : "Bin pick completed",
      );
      break;
    }
    case "ShipmentPacked": {
      if (shipmentId) {
        const shipment = ensureShipment(state, shipmentId);
        shipment.status = "packed";
        shipment.packedAtSimTime = event.simTime;
        shipment.activePortId = portId;
        shipment.gridId = gridId;
        shipment.eventIndices.push(event.index);
        state.metrics.shipmentsPacked += 1;
      }
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "idle";
        port.activeShipmentId = null;
        port.activeBinId = null;
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
        port.lastTransition = "packed-shipment";
        if (gridId) {
          linkPortToGrid(state, portId, gridId, event.index);
        }
      }
      changes.push(
        shipmentId ? `Shipment ${shipmentId} packed` : "Shipment packed",
      );
      break;
    }
    case "ShipmentShipped": {
      if (shipmentId) {
        const shipment = ensureShipment(state, shipmentId);
        shipment.status = "shipped";
        shipment.shippedAtSimTime = event.simTime;
        shipment.eventIndices.push(event.index);
        state.metrics.shipmentsShipped += 1;
        changes.push(`Shipment ${shipmentId} shipped`);
      } else {
        changes.push("Shipment shipped with missing shipmentId");
      }
      break;
    }
    case "TruckArrival": {
      state.metrics.truckArrivals += 1;
      changes.push(
        `Truck arrival${getString(data.sortingDirection) ? ` for ${getString(data.sortingDirection)}` : ""}`,
      );
      break;
    }
    case "PortOpens": {
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "idle";
        port.handlingFlags = getStringArray(data.handlingFlags);
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
        port.lastTransition = data.fromBreak === true ? "reopened-from-break" : "opened";
        if (gridId) {
          linkPortToGrid(state, portId, gridId, event.index);
        }
        changes.push(
          `${portId} opened${data.fromBreak === true ? " from break" : ""}`,
        );
      } else {
        changes.push("Port opened");
      }
      break;
    }
    case "PortCloses": {
      if (portId) {
        const port = ensurePort(state, portId);
        port.status = "closed";
        port.handlingFlags = getStringArray(data.handlingFlags);
        port.activeShipmentId = null;
        port.activeBinId = null;
        port.lastEventIndex = event.index;
        port.lastEventType = event.eventKey;
        port.lastTransition = data.intoBreak === true ? "closed-into-break" : "closed";
        if (gridId) {
          linkPortToGrid(state, portId, gridId, event.index);
        }
        changes.push(
          `${portId} closed${data.intoBreak === true ? " for break" : ""}`,
        );
      } else {
        changes.push("Port closed");
      }
      break;
    }
    case "BinTransferStarted": {
      if (binId) {
        const bin = ensureBin(state, binId);
        bin.status = "in-transfer";
        bin.lastEventIndex = event.index;
      }
      changes.push(binId ? `Bin ${binId} transfer started` : "Bin transfer started");
      break;
    }
    case "BinTransferCompleted": {
      if (binId) {
        const bin = ensureBin(state, binId);
        bin.status = "available";
        bin.lastEventIndex = event.index;
        if (gridId) {
          bin.gridId = gridId;
        }
      }
      changes.push(
        binId ? `Bin ${binId} transfer completed` : "Bin transfer completed",
      );
      break;
    }
    case "RouterTick": {
      changes.push("Router tick processed");
      break;
    }
    default: {
      changes.push(`Processed ${event.event}`);
      break;
    }
  }

  state.recentChanges = changes;
  return state;
}

export function buildReplayArtifacts(
  events: NormalizedEvent[],
  eventGroups: EventGroup[],
): ReplayArtifacts {
  const checkpoints: ReplayCheckpoint[] = [
    {
      eventIndex: -1,
      snapshot: createInitialReplayState(),
    },
  ];

  let currentState = createInitialReplayState();
  for (const event of events) {
    currentState = applyEventToState(currentState, event);
    if ((event.index + 1) % CHECKPOINT_INTERVAL === 0 || event.index === events.length - 1) {
      checkpoints.push({
        eventIndex: event.index,
        snapshot: cloneReplayState(currentState),
      });
    }
  }

  if (!events.length) {
    const initialCheckpoint = checkpoints[0];
    if (initialCheckpoint) {
      initialCheckpoint.snapshot.metrics.groupsProcessed = eventGroups.length;
    }
  }

  return {
    checkpoints,
    checkpointInterval: CHECKPOINT_INTERVAL,
  };
}

export function replayToEventIndex(
  events: NormalizedEvent[],
  checkpoints: ReplayCheckpoint[],
  targetEventIndex: number,
): DerivedReplayState {
  if (targetEventIndex < 0 || events.length === 0) {
    return createInitialReplayState();
  }

  let checkpoint = checkpoints[0];
  if (!checkpoint) {
    return createInitialReplayState();
  }

  for (const candidate of checkpoints) {
    if (candidate.eventIndex <= targetEventIndex) {
      checkpoint = candidate;
    } else {
      break;
    }
  }

  let state = cloneReplayState(checkpoint.snapshot);
  for (let index = checkpoint.eventIndex + 1; index <= targetEventIndex; index += 1) {
    const event = events[index];
    if (!event) {
      break;
    }
    state = applyEventToState(state, event);
  }

  return state;
}
