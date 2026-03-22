import {
  EventGroup,
  EventData,
  EventImportance,
  LoadedSimulation,
  NormalizedEvent,
  ParseIssue,
  RawLogEvent,
} from "./types";
import { buildReplayArtifacts } from "./replay";

const HIGH_IMPORTANCE_EVENTS = new Set([
  "ShipmentReceived",
  "ShipmentIsReady",
  "PortStartsShipment",
  "BinArrivesAtPort",
  "BinArrivedAtPort",
  "BinPickCompleted",
  "ShipmentPacked",
  "ShipmentShipped",
]);

const MEDIUM_IMPORTANCE_EVENTS = new Set([
  "PortOpens",
  "PortCloses",
  "TruckArrival",
  "TruckArrived",
  "BinTransferStarted",
  "BinTransferCompleted",
  "BinRequestedAtPort",
]);

function getImportance(eventName: string): EventImportance {
  if (HIGH_IMPORTANCE_EVENTS.has(eventName)) {
    return "high";
  }

  if (MEDIUM_IMPORTANCE_EVENTS.has(eventName)) {
    return "medium";
  }

  if (eventName === "RouterTick") {
    return "low";
  }

  return "unknown";
}

function normalizeEventKey(eventName: string): string {
  if (eventName === "TruckArrived") {
    return "TruckArrival";
  }

  if (eventName === "BinArrivedAtPort") {
    return "BinArrivesAtPort";
  }

  return eventName;
}

function parseObjectRecord(value: unknown): EventData {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return {};
  }

  return value as EventData;
}

export function parseSimulationLog(text: string): {
  rawEvents: RawLogEvent[];
  issues: ParseIssue[];
} {
  const rawEvents: RawLogEvent[] = [];
  const issues: ParseIssue[] = [];
  const lines = text.split(/\r?\n/);

  lines.forEach((line, zeroIndex) => {
    const lineNumber = zeroIndex + 1;
    const trimmed = line.trim();

    if (!trimmed) {
      return;
    }

    let parsed: unknown;
    try {
      parsed = JSON.parse(trimmed);
    } catch (error) {
      issues.push({
        lineNumber,
        severity: "error",
        message: error instanceof Error ? error.message : "Invalid JSON",
        rawLine: line,
      });
      return;
    }

    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) {
      issues.push({
        lineNumber,
        severity: "error",
        message: "Each JSONL line must be a JSON object.",
        rawLine: line,
      });
      return;
    }

    const record = parsed as Record<string, unknown>;
    const simTime = record.simTime;
    const event = typeof record.event === "string" ? record.event : record.type;

    if (typeof simTime !== "number" || !Number.isFinite(simTime)) {
      issues.push({
        lineNumber,
        severity: "error",
        message: "Missing or invalid numeric simTime.",
        rawLine: line,
      });
      return;
    }

    if (typeof event !== "string" || !event.trim()) {
      issues.push({
        lineNumber,
        severity: "error",
        message: "Missing event name.",
        rawLine: line,
      });
      return;
    }

    const timestamp =
      typeof record.timestamp === "string" && record.timestamp.trim()
        ? record.timestamp
        : null;

    const duration =
      typeof record.duration === "number" && Number.isFinite(record.duration)
        ? record.duration
        : null;

    rawEvents.push({
      lineNumber,
      rawLine: line,
      simTime,
      timestamp,
      event: event.trim(),
      duration,
      data: parseObjectRecord(record.data),
    });
  });

  return {
    rawEvents,
    issues,
  };
}

export function buildEventGroups(events: NormalizedEvent[]): EventGroup[] {
  const groups: EventGroup[] = [];

  for (const event of events) {
    const previousGroup = groups[groups.length - 1];
    if (previousGroup && previousGroup.simTime === event.simTime) {
      previousGroup.events.push(event);
      previousGroup.endEventIndex = event.index;
      previousGroup.eventCount += 1;
      if (event.importance === "high" || previousGroup.importance === "low") {
        previousGroup.importance = event.importance;
      }
      continue;
    }

    groups.push({
      groupIndex: groups.length,
      simTime: event.simTime,
      timestamp: event.timestamp,
      startEventIndex: event.index,
      endEventIndex: event.index,
      eventCount: 1,
      importance: event.importance,
      events: [event],
    });
  }

  return groups;
}

export function normalizeEvents(rawEvents: RawLogEvent[]): NormalizedEvent[] {
  const normalized: NormalizedEvent[] = rawEvents.map((rawEvent, index) => ({
    ...rawEvent,
    index,
    eventKey: normalizeEventKey(rawEvent.event),
    importance: getImportance(normalizeEventKey(rawEvent.event)),
    groupIndex: -1,
  }));

  const groups = buildEventGroups(normalized);
  groups.forEach((group) => {
    group.events.forEach((event) => {
      event.groupIndex = group.groupIndex;
    });
  });

  return normalized;
}

export function buildLoadedSimulation(
  sourceName: string,
  rawText: string,
  rawEvents: RawLogEvent[],
): LoadedSimulation {
  const events = normalizeEvents(rawEvents);
  const eventGroups = buildEventGroups(events);

  eventGroups.forEach((group) => {
    group.events.forEach((event) => {
      event.groupIndex = group.groupIndex;
    });
  });

  return {
    sourceName,
    rawText,
    rawEvents,
    events,
    eventGroups,
    artifacts: buildReplayArtifacts(events, eventGroups),
  };
}
