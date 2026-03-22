import { create } from "zustand";
import { buildLoadedSimulation, parseSimulationLog } from "../lib/parser";
import { replayToEventIndex } from "../lib/replay";
import { DerivedReplayState, LoadedSimulation, ParseIssue } from "../lib/types";

interface SimulationStore {
  loadedSimulation: LoadedSimulation | null;
  parseIssues: ParseIssue[];
  replayState: DerivedReplayState;
  isPlaying: boolean;
  playbackSpeed: number;
  loadFromText: (sourceName: string, text: string) => void;
  clearIssues: () => void;
  setPlaybackSpeed: (speed: number) => void;
  play: () => void;
  pause: () => void;
  stepNextEvent: () => void;
  stepPreviousEvent: () => void;
  stepNextGroup: () => void;
  stepPreviousGroup: () => void;
  scrubToEvent: (eventIndex: number) => void;
}

function getStateForEventIndex(simulation: LoadedSimulation, eventIndex: number) {
  return replayToEventIndex(
    simulation.events,
    simulation.artifacts.checkpoints,
    eventIndex,
  );
}

function getCurrentGroup(simulation: LoadedSimulation, currentEventIndex: number) {
  if (currentEventIndex < 0) {
    return null;
  }

  return simulation.eventGroups.find(
    (group) =>
      currentEventIndex >= group.startEventIndex &&
      currentEventIndex <= group.endEventIndex,
  ) ?? null;
}

export const useSimulationStore = create<SimulationStore>((set, get) => ({
  loadedSimulation: null,
  parseIssues: [],
  replayState: {
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
  },
  isPlaying: false,
  playbackSpeed: 2,
  loadFromText: (sourceName, text) => {
    const parsed = parseSimulationLog(text);
    if (parsed.issues.some((issue) => issue.severity === "error")) {
      set({
        loadedSimulation: null,
        parseIssues: parsed.issues,
        isPlaying: false,
        replayState: {
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
        },
      });
      return;
    }

    const loadedSimulation = buildLoadedSimulation(
      sourceName,
      text,
      parsed.rawEvents,
    );
    set({
      loadedSimulation,
      parseIssues: parsed.issues,
      isPlaying: false,
      replayState: getStateForEventIndex(loadedSimulation, -1),
    });
  },
  clearIssues: () => set({ parseIssues: [] }),
  setPlaybackSpeed: (speed) => set({ playbackSpeed: speed }),
  play: () => set({ isPlaying: true }),
  pause: () => set({ isPlaying: false }),
  stepNextEvent: () => {
    const simulation = get().loadedSimulation;
    if (!simulation) {
      return;
    }

    const nextIndex = Math.min(
      get().replayState.currentEventIndex + 1,
      simulation.events.length - 1,
    );

    set({
      replayState: getStateForEventIndex(simulation, nextIndex),
      isPlaying: nextIndex >= simulation.events.length - 1 ? false : get().isPlaying,
    });
  },
  stepPreviousEvent: () => {
    const simulation = get().loadedSimulation;
    if (!simulation) {
      return;
    }

    const previousIndex = Math.max(get().replayState.currentEventIndex - 1, -1);
    set({
      replayState: getStateForEventIndex(simulation, previousIndex),
      isPlaying: false,
    });
  },
  stepNextGroup: () => {
    const simulation = get().loadedSimulation;
    if (!simulation) {
      return;
    }

    const currentIndex = get().replayState.currentEventIndex;
    const currentGroup = getCurrentGroup(simulation, currentIndex);
    const currentGroupIndex = currentGroup?.groupIndex ?? -1;
    const targetGroup =
      currentGroup && currentIndex < currentGroup.endEventIndex
        ? currentGroup
        : simulation.eventGroups[currentGroupIndex + 1];

    if (!targetGroup) {
      return;
    }

    set({
      replayState: getStateForEventIndex(simulation, targetGroup.endEventIndex),
      isPlaying: false,
    });
  },
  stepPreviousGroup: () => {
    const simulation = get().loadedSimulation;
    if (!simulation) {
      return;
    }

    const currentIndex = get().replayState.currentEventIndex;
    const currentGroup = getCurrentGroup(simulation, currentIndex);
    const targetGroupIndex = (currentGroup?.groupIndex ?? 0) - 1;
    const targetEventIndex =
      targetGroupIndex >= 0
        ? simulation.eventGroups[targetGroupIndex]?.endEventIndex ?? -1
        : -1;

    set({
      replayState: getStateForEventIndex(simulation, targetEventIndex),
      isPlaying: false,
    });
  },
  scrubToEvent: (eventIndex) => {
    const simulation = get().loadedSimulation;
    if (!simulation) {
      return;
    }

    set({
      replayState: getStateForEventIndex(simulation, eventIndex),
      isPlaying: false,
    });
  },
}));
