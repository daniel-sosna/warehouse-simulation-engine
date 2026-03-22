export type EventImportance = "low" | "medium" | "high" | "unknown";

export interface ParseIssue {
  lineNumber: number;
  severity: "error" | "warning";
  message: string;
  rawLine: string;
}

export interface RawLogEvent {
  lineNumber: number;
  rawLine: string;
  simTime: number;
  timestamp: string | null;
  event: string;
  duration: number | null;
  data: Record<string, unknown>;
}

export interface NormalizedEvent extends RawLogEvent {
  index: number;
  importance: EventImportance;
  eventKey: string;
  groupIndex: number;
}

export interface EventGroup {
  groupIndex: number;
  simTime: number;
  timestamp: string | null;
  startEventIndex: number;
  endEventIndex: number;
  eventCount: number;
  importance: EventImportance;
  events: NormalizedEvent[];
}

export type ShipmentStatus =
  | "unknown"
  | "received"
  | "ready"
  | "picking"
  | "packed"
  | "shipped";

export type PortStatus =
  | "unknown"
  | "closed"
  | "idle"
  | "busy"
  | "waiting-bin"
  | "pending-close";

export type BinStatus =
  | "unknown"
  | "available"
  | "requested"
  | "at-port"
  | "in-transfer";

export interface ShipmentState {
  id: string;
  status: ShipmentStatus;
  receivedAtSimTime: number | null;
  readyAtSimTime: number | null;
  packedAtSimTime: number | null;
  shippedAtSimTime: number | null;
  activePortId: string | null;
  gridId: string | null;
  pickedBinIds: string[];
  eventIndices: number[];
}

export interface PortState {
  id: string;
  gridId: string | null;
  status: PortStatus;
  handlingFlags: string[];
  activeShipmentId: string | null;
  activeBinId: string | null;
  lastEventIndex: number | null;
  lastEventType: string | null;
  lastTransition: string | null;
}

export interface BinState {
  id: string;
  gridId: string | null;
  status: BinStatus;
  portId: string | null;
  shipmentId: string | null;
  lastEventIndex: number | null;
}

export interface GridState {
  id: string;
  portIds: string[];
  lastEventIndex: number | null;
}

export interface MetricsSnapshot {
  eventsProcessed: number;
  groupsProcessed: number;
  shipmentsReceived: number;
  shipmentsReady: number;
  shipmentsPacked: number;
  shipmentsShipped: number;
  truckArrivals: number;
}

export interface DerivedReplayState {
  currentEventIndex: number;
  currentGroupIndex: number;
  currentSimTime: number | null;
  currentTimestamp: string | null;
  shipmentsById: Record<string, ShipmentState>;
  portsById: Record<string, PortState>;
  binsById: Record<string, BinState>;
  gridsById: Record<string, GridState>;
  metrics: MetricsSnapshot;
  recentChanges: string[];
}

export interface ReplayCheckpoint {
  eventIndex: number;
  snapshot: DerivedReplayState;
}

export interface ReplayArtifacts {
  checkpoints: ReplayCheckpoint[];
  checkpointInterval: number;
}

export interface LoadedSimulation {
  sourceName: string;
  rawText: string;
  rawEvents: RawLogEvent[];
  events: NormalizedEvent[];
  eventGroups: EventGroup[];
  artifacts: ReplayArtifacts;
}
