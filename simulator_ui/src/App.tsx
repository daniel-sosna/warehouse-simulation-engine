import {
  ChangeEvent,
  PointerEvent as ReactPointerEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import {
  BinState,
  EventData,
  ItemQuantities,
  NormalizedEvent,
  PortState,
  ShipmentState,
} from "./lib/types";
import { useSimulationStore } from "./store/useSimulationStore";

type AppTab = "timeline" | "statistics";

function formatTimestamp(timestamp: string | null) {
  return timestamp ?? "Unavailable";
}

function formatSimTime(simTime: number | null) {
  if (simTime === null) {
    return "Not started";
  }

  return `${simTime.toLocaleString()}s`;
}

function cn(...values: Array<string | false | null | undefined>) {
  return values.filter(Boolean).join(" ");
}

function formatStringList(values: string[] | null | undefined) {
  return values && values.length ? values.join(", ") : "Unavailable";
}

function formatItemQuantities(items: ItemQuantities | null | undefined) {
  if (!items || !Object.keys(items).length) {
    return "Unavailable";
  }

  return Object.entries(items)
    .map(([itemId, quantity]) => `${itemId}: ${quantity}`)
    .join("\n");
}

function getStringValue(value: unknown) {
  return typeof value === "string" && value.trim() ? value : null;
}

function getStringArrayValue(value: unknown) {
  if (!Array.isArray(value)) {
    return null;
  }

  const values = value.filter((item): item is string => typeof item === "string");
  return values.length ? values : [];
}

function getItemQuantityValue(value: unknown) {
  if (!value || typeof value !== "object" || Array.isArray(value)) {
    return null;
  }

  const result: ItemQuantities = {};
  for (const [key, itemValue] of Object.entries(value)) {
    if (typeof itemValue === "number" && Number.isFinite(itemValue)) {
      result[key] = itemValue;
    }
  }

  return Object.keys(result).length ? result : null;
}

function buildEventStructuredFields(event: NormalizedEvent) {
  const data = event.data as EventData;
  const fields: Array<{ label: string; value: string }> = [];
  const shipmentId = getStringValue(data.shipmentId);
  const sortingDirection = getStringValue(data.sortingDirection);
  const portId = getStringValue(data.portId);
  const gridId = getStringValue(data.gridId);
  const binId = getStringValue(data.binId);
  const handlingFlags = getStringArrayValue(data.handlingFlags);
  const items = getItemQuantityValue(data.items);
  const shipmentItems = getItemQuantityValue(data.shipmentItems);
  const itemsPicked = getItemQuantityValue(data.itemsPicked);
  const binStock = getItemQuantityValue(data.binStock);

  if (shipmentId) {
    fields.push({ label: "Shipment ID", value: shipmentId });
  }
  if (sortingDirection) {
    fields.push({ label: "Sorting direction", value: sortingDirection });
  }
  if (handlingFlags) {
    fields.push({ label: "Handling flags", value: formatStringList(handlingFlags) });
  }
  if (portId) {
    fields.push({ label: "Port ID", value: portId });
  }
  if (gridId) {
    fields.push({ label: "Grid ID", value: gridId });
  }
  if (binId) {
    fields.push({ label: "Bin ID", value: binId });
  }
  if (items) {
    fields.push({ label: "Items", value: formatItemQuantities(items) });
  }
  if (shipmentItems) {
    fields.push({
      label: "Shipment items",
      value: formatItemQuantities(shipmentItems),
    });
  }
  if (itemsPicked) {
    fields.push({
      label: "Items picked",
      value: formatItemQuantities(itemsPicked),
    });
  }
  if (binStock) {
    fields.push({ label: "Bin stock", value: formatItemQuantities(binStock) });
  }
  if (typeof data.shipmentsTakenForShipping === "number") {
    fields.push({
      label: "Shipments taken",
      value: String(data.shipmentsTakenForShipping),
    });
  }

  return fields;
}

function findLatestRevealedItem<T extends { revealed: boolean }>(groups: T[]) {
  for (let index = groups.length - 1; index >= 0; index -= 1) {
    const group = groups[index];
    if (group?.revealed) {
      return group;
    }
  }

  return null;
}

export default function App() {
  const {
    loadedSimulation,
    parseIssues,
    replayState,
    isPlaying,
    playbackSpeed,
    loadFromText,
    setPlaybackSpeed,
    play,
    pause,
    stepNextEvent,
    stepPreviousEvent,
    scrubToEvent,
  } = useSimulationStore();
  const [activeTab, setActiveTab] = useState<AppTab>("timeline");
  const [selectedEvent, setSelectedEvent] = useState<NormalizedEvent | null>(null);
  const [mapOffset, setMapOffset] = useState({ x: 120, y: 120 });
  const [isPanning, setIsPanning] = useState(false);
  const [autoFollow, setAutoFollow] = useState(true);
  const dragStateRef = useRef<{
    startX: number;
    startY: number;
    originX: number;
    originY: number;
    moved: boolean;
    pointerId: number;
  } | null>(null);
  const pendingOffsetRef = useRef({ x: 120, y: 120 });
  const animationFrameRef = useRef<number | null>(null);
  const timelineViewportRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isPlaying || !loadedSimulation) {
      return;
    }

    if (replayState.currentEventIndex >= loadedSimulation.events.length - 1) {
      pause();
      return;
    }

    const interval = window.setInterval(() => {
      const { loadedSimulation: simulation, replayState: currentReplayState } =
        useSimulationStore.getState();
      if (!simulation) {
        return;
      }

      if (currentReplayState.currentEventIndex >= simulation.events.length - 1) {
        useSimulationStore.getState().pause();
        return;
      }

      useSimulationStore.getState().stepNextEvent();
    }, Math.max(120, Math.floor(1000 / playbackSpeed)));

    return () => window.clearInterval(interval);
  }, [
    isPlaying,
    loadedSimulation,
    pause,
    playbackSpeed,
    replayState.currentEventIndex,
  ]);

  useEffect(() => {
    setActiveTab("timeline");
    setSelectedEvent(null);
    setMapOffset({ x: 120, y: 120 });
    pendingOffsetRef.current = { x: 120, y: 120 };
    setAutoFollow(true);
  }, [loadedSimulation?.sourceName]);

const timelineEvents =
  loadedSimulation
    ? (() => {
        const groupSpacing = 360;
        const cardVerticalSpacing = 156;
        const baseStartX = 220;
        const baseCenterY = 300;
        const topPadding = 120;

        const rawEvents = loadedSimulation.eventGroups.flatMap((group, groupIndex) => {
          const groupEvents = loadedSimulation.events.slice(
            group.startEventIndex,
            group.endEventIndex + 1,
          );

          const groupSize = groupEvents.length;

          // one column per simTime group
          const columnX = baseStartX + groupIndex * groupSpacing;

          // slight overall wave across columns
          const columnCenterY = baseCenterY + Math.sin(groupIndex * 0.45) * 28;

          return groupEvents.map((event, eventIndexWithinGroup) => {
            const centeredOffset =
              (eventIndexWithinGroup - (groupSize - 1) / 2) * cardVerticalSpacing;

            return {
              ...event,
              x: columnX,
              y: columnCenterY + centeredOffset,
              revealed: replayState.currentEventIndex >= event.index,
            };
          });
        });

        const minY = Math.min(...rawEvents.map((event) => event.y), topPadding);
        const shiftY = minY < topPadding ? topPadding - minY : 0;

        return rawEvents.map((event) => ({
          ...event,
          y: event.y + shiftY,
        }));
      })()
    : [];

  const maxGroupSize = loadedSimulation
    ? Math.max(
        ...loadedSimulation.eventGroups.map(
          (group) => group.endEventIndex - group.startEventIndex + 1,
        ),
        1,
      )
    : 1;

  const timelineWidth = Math.max(
    2200,
    (timelineEvents[timelineEvents.length - 1]?.x ?? 0) + 700,
  );

  const timelineHeight = Math.max(
    980,
    Math.max(...timelineEvents.map((event) => event.y + 180), 980),
  );

  const currentTimelineEvent =
    replayState.currentEventIndex >= 0
      ? timelineEvents[replayState.currentEventIndex] ?? null
      : null;

  function scheduleMapOffset(nextOffset: { x: number; y: number }) {
    pendingOffsetRef.current = nextOffset;
    if (animationFrameRef.current !== null) {
      return;
    }

    animationFrameRef.current = window.requestAnimationFrame(() => {
      setMapOffset(pendingOffsetRef.current);
      animationFrameRef.current = null;
    });
  }

  useEffect(() => {
    if (
      activeTab !== "timeline" ||
      !autoFollow ||
      !loadedSimulation ||
      replayState.currentEventIndex < 0
    ) {
      return;
    }

    const event = timelineEvents[replayState.currentEventIndex];
    if (!event) {
      return;
    }

    focusEventInViewport(event);
  }, [
    activeTab,
    autoFollow,
    loadedSimulation,
    replayState.currentEventIndex,
    timelineEvents,
  ]);

  useEffect(() => {
    return () => {
      if (animationFrameRef.current !== null) {
        window.cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, []);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    const text = await file.text();
    loadFromText(file.name, text);
    event.target.value = "";
  }

  function jumpToLatestEvent() {
    if (!loadedSimulation) {
      return;
    }

    const latest =
      currentTimelineEvent ?? findLatestRevealedItem(timelineEvents);

    if (!latest) {
      return;
    }

    setAutoFollow(true);
    focusEventInViewport(latest);
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    if (event.button !== 0 || isNoPanTarget(event.target)) {
      return;
    }

    event.currentTarget.setPointerCapture(event.pointerId);
    dragStateRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      originX: mapOffset.x,
      originY: mapOffset.y,
      moved: false,
      pointerId: event.pointerId,
    };
    setIsPanning(true);
  } 

  function handlePointerMove(event: ReactPointerEvent<HTMLDivElement>) {
    const dragState = dragStateRef.current;
    if (!dragState) {
      return;
    }

    const deltaX = event.clientX - dragState.startX;
    const deltaY = event.clientY - dragState.startY;

    if (Math.abs(deltaX) > 3 || Math.abs(deltaY) > 3) {
      dragState.moved = true;
      if (autoFollow) {
        setAutoFollow(false);
      }
    }

    scheduleMapOffset({
      x: dragState.originX + deltaX,
      y: dragState.originY + deltaY,
    });
  }

  function handlePointerUp(event?: ReactPointerEvent<HTMLDivElement>) {
    if (
      event &&
      dragStateRef.current &&
      event.currentTarget.hasPointerCapture(dragStateRef.current.pointerId)
    ) {
      event.currentTarget.releasePointerCapture(dragStateRef.current.pointerId);
    }

    setIsPanning(false);
    window.setTimeout(() => {
      dragStateRef.current = null;
    }, 0);
  }

  function handlePlayPause() {
    if (isPlaying) {
      pause();
      return;
    }

    setAutoFollow(true);
    play();
  }

  function handlePreviousEvent() {
    setAutoFollow(true);
    stepPreviousEvent();
  }

  function handleNextEvent() {
    setAutoFollow(true);
    stepNextEvent();
  }

  function handleScrubToEvent(eventIndex: number) {
    setAutoFollow(true);
    scrubToEvent(eventIndex);
  }

  function handleSelectEvent(event: NormalizedEvent) {
    if (dragStateRef.current?.moved) {
      return;
    }

    setSelectedEvent(event);
  }

  function handleJumpReplayToSelectedEvent() {
    if (!selectedEvent) {
      return;
    }

    setAutoFollow(true);
    scrubToEvent(selectedEvent.index);
  }

  function getFollowOffsetForEvent(event: { x: number; y: number }) {
    const viewportWidth =
      timelineViewportRef.current?.clientWidth ?? window.innerWidth;
    const viewportHeight =
      timelineViewportRef.current?.clientHeight ?? window.innerHeight;

    return {
      x: viewportWidth * 0.3 - event.x,
      y: viewportHeight * 0.35 - event.y,
    };
  }

  function focusEventInViewport(event: { x: number; y: number }) {
    scheduleMapOffset(getFollowOffsetForEvent(event));
  }

  function isNoPanTarget(target: EventTarget | null) {
    return (
      target instanceof HTMLElement &&
      Boolean(target.closest("[data-no-pan='true']"))
    );
  }

  if (!loadedSimulation) {
  return (
    <div className="relative min-h-screen overflow-hidden bg-[linear-gradient(180deg,#f8fafc_0%,#eff6ff_45%,#eef2ff_100%)] text-slate-900">
      <div className="absolute inset-0 bg-[radial-gradient(circle_at_top,rgba(14,165,233,0.08),transparent_35%)]" />

      <div className="relative flex min-h-screen flex-col items-center justify-center px-6 text-center">
        <h1 className="mt-4 max-w-4xl text-4xl font-semibold tracking-tight text-slate-950 md:text-6xl">
          Upload a log file to start simulation visualization
        </h1> 

        <label className="mt-12 flex cursor-pointer flex-col items-center justify-center gap-5 rounded-[2rem] border-2 border-dashed border-sky-200 bg-white/70 px-10 py-14 shadow-[0_30px_80px_rgba(148,163,184,0.14)] backdrop-blur transition hover:border-sky-400 hover:bg-white hover:shadow-[0_36px_90px_rgba(14,165,233,0.10)] md:px-16">
          <input
            className="hidden"
            type="file"
            accept=".log,.jsonl,.txt,application/json"
            onChange={handleFileChange}
          />

          <div className="flex h-16 w-16 items-center justify-center rounded-full bg-sky-100 text-sky-700">
            <svg
              className="h-8 w-8"
              viewBox="0 0 24 24"
              fill="none"
              aria-hidden="true"
            >
              <path
                d="M12 16V5"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
              <path
                d="M8 9L12 5L16 9"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path
                d="M5 19H19"
                stroke="currentColor"
                strokeWidth="1.8"
                strokeLinecap="round"
              />
            </svg>
          </div>

          <div className="space-y-2">
            <p className="text-lg font-semibold text-slate-950">
              Drop file here or click to browse
            </p>
            <p className="text-sm text-slate-500">
              JSONL only. Unknown or sparse events will still load safely.
            </p>
          </div>

          <span className="rounded-full bg-sky-600 px-5 py-2 text-sm font-medium text-white transition hover:bg-sky-700">
            Choose log file
          </span>
        </label>

        {parseIssues.length > 0 && (
          <div className="mt-10 w-full max-w-3xl rounded-[1.5rem] border border-rose-200 bg-rose-50/90 p-5 text-left shadow-sm">
            <div className="flex items-center justify-between gap-3">
              <h2 className="text-sm font-semibold uppercase tracking-[0.18em] text-rose-700">
                Parse errors
              </h2>
              <span className="text-sm text-rose-600">
                {parseIssues.length} issue(s)
              </span>
            </div>

            <div className="mt-4 space-y-3">
              {parseIssues.map((issue) => (
                <div
                  key={`${issue.lineNumber}-${issue.message}`}
                  className="rounded-2xl border border-rose-200 bg-white p-3"
                >
                  <p className="text-sm font-medium text-rose-800">
                    Line {issue.lineNumber}: {issue.message}
                  </p>
                  <pre className="mt-2 overflow-auto text-xs text-rose-700">
                    {issue.rawLine}
                  </pre>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

  if (activeTab === "timeline") {
    const latestRevealedEvent = currentTimelineEvent
      ? timelineEvents[currentTimelineEvent.index]
      : findLatestRevealedItem(timelineEvents);

    return (
      <div className="relative h-screen overflow-hidden bg-[radial-gradient(circle_at_top_left,rgba(14,165,233,0.10),transparent_22%),linear-gradient(180deg,#f8fbff_0%,#edf4ff_100%)] text-slate-900">
        <div className="pointer-events-none absolute inset-x-0 top-5 z-20 flex justify-center px-4">
          <div className="pointer-events-auto flex items-center gap-2 rounded-full border border-slate-200 bg-white/92 p-2 shadow-[0_24px_70px_rgba(148,163,184,0.22)] backdrop-blur">
            <TabButton
              active
              label="Timeline"
              onClick={() => setActiveTab("timeline")}
            />
            <TabButton
              active={false}
              label="Statistics"
              onClick={() => setActiveTab("statistics")}
            />
          </div>
        </div>

        <div
          ref={timelineViewportRef}
          className={cn(
            "absolute inset-0 overflow-hidden",
            isPanning ? "cursor-grabbing" : "cursor-grab",
          )}
          onPointerDown={handlePointerDown}
          onPointerMove={handlePointerMove}
          onPointerUp={handlePointerUp}
          onPointerLeave={handlePointerUp}
        >
          <div
            className={cn(
              "absolute left-0 top-0 will-change-transform",
              autoFollow && !isPanning ? "transition-transform duration-200 ease-out" : "",
            )}
            style={{
              width: timelineWidth,
              height: timelineHeight,
              transform: `translate(${mapOffset.x}px, ${mapOffset.y}px)`,
            }}
          >
            <svg
              className="absolute inset-0 h-full w-full"
              viewBox={`0 0 ${timelineWidth} ${timelineHeight}`}
              fill="none"
            >
              {timelineEvents.slice(0, -1).map((event, index) => {
                const nextEvent = timelineEvents[index + 1];
                if (!nextEvent) {
                  return null;
                }

                const lineVisible = event.revealed && nextEvent.revealed;
                const sameGroup = event.groupIndex === nextEvent.groupIndex;

                const d = sameGroup
                  ? `M ${event.x + 104} ${event.y + 124} L ${nextEvent.x + 104} ${nextEvent.y}`
                  : `M ${event.x + 208} ${event.y + 62} C ${event.x + 280} ${event.y + 62}, ${nextEvent.x - 72} ${nextEvent.y + 62}, ${nextEvent.x} ${nextEvent.y + 62}`;

                return (
                  <path
                    key={`line-${event.index}`}
                    d={d}
                    stroke={lineVisible ? "#0ea5e9" : "#dbeafe"}
                    strokeDasharray={lineVisible ? "0" : "8 12"}
                    strokeLinecap="round"
                    strokeWidth={lineVisible ? 3 : 2}
                    style={{
                      opacity: lineVisible ? 0.72 : 0.5,
                      transition: "all 260ms ease",
                    }}
                  />
                );
              })}
            </svg>

            {timelineEvents.map((event) => {
              const activeReplayEvent = event.index === replayState.currentEventIndex;
              const selectedTimelineEvent = selectedEvent?.index === event.index;

              return (
                <button
                  key={`event-${event.index}`}
                  data-no-pan="true"
                  onPointerDown={(pointerEvent) => {
                    pointerEvent.stopPropagation();
                  }}
                  className={cn("absolute w-[208px] rounded-[1.55rem] border bg-white p-4 text-left shadow-[0_22px_55px_rgba(148,163,184,0.14)] transition-all duration-300",
                    
                    activeReplayEvent
                      ? "border-sky-400 shadow-[0_24px_60px_rgba(14,165,233,0.24)]"
                      : selectedTimelineEvent
                        ? "border-violet-300 shadow-[0_20px_48px_rgba(139,92,246,0.16)]"
                        : "border-slate-200 hover:border-sky-300",
                    event.revealed
                      ? "pointer-events-auto translate-y-0 opacity-100"
                      : "pointer-events-none translate-y-4 opacity-0",
                  )}
                  style={{ left: event.x, top: event.y }}
                  onClick={() => {
                    handleSelectEvent(event);
                  }}
                  type="button"
                >
                  <div className="flex items-center justify-between gap-2">
                    <span className="rounded-full bg-sky-50 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-sky-700">
                      Event
                    </span>
                    <span className="text-xs text-slate-400">
                      #{event.index + 1}
                    </span>
                  </div>

                  <p className="mt-3 text-base font-semibold text-slate-950">
                    {event.event}
                  </p>
                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    simTime {event.simTime}
                  </p>
                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    {event.timestamp ?? "No timestamp"}
                  </p>

                </button>
              );
            })}

            {replayState.currentSimTime === null && (
              <div className="absolute left-24 top-28 rounded-[1.5rem] border border-slate-200 bg-white/96 px-5 py-4 text-sm text-slate-600 shadow-lg">
                Start replaying to reveal the timeline map.
              </div>
            )}
          </div>
        </div>

        {selectedEvent && (
          <div className="pointer-events-none absolute inset-y-5 right-5 z-20 flex w-full max-w-md justify-end pb-28">
            <div className="pointer-events-auto max-h-full w-full overflow-auto rounded-[1.8rem] border border-slate-200 bg-white/96 p-5 shadow-[0_28px_80px_rgba(148,163,184,0.24)] backdrop-blur">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">
                    Event detail
                  </p>
                  <h3 className="mt-1 text-xl font-semibold text-slate-950">
                    {selectedEvent.event}
                  </h3>
                </div>
                <button
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-sm text-slate-500 transition hover:border-slate-300 hover:text-slate-900"
                  onClick={() => setSelectedEvent(null)}
                  type="button"
                >
                  Close
                </button>
              </div>

              <div className="mt-5 grid gap-3 sm:grid-cols-2">
                <DetailCard label="simTime" value={String(selectedEvent.simTime)} />
                <DetailCard
                  label="Timestamp"
                  value={formatTimestamp(selectedEvent.timestamp)}
                />
                <DetailCard label="Line" value={String(selectedEvent.lineNumber)} />
                <DetailCard
                  label="Replay position"
                  value={
                    replayState.currentGroupIndex === selectedEvent.groupIndex
                      ? "Current replay point"
                      : selectedEvent.simTime <
                          (replayState.currentSimTime ?? Number.NEGATIVE_INFINITY)
                        ? "Past event"
                        : "Future event"
                  }
                />
              </div>

              <div className="mt-4">
                <button
                  className="w-full rounded-full bg-sky-600 px-4 py-3 text-sm font-medium text-white transition hover:bg-sky-700"
                  onClick={handleJumpReplayToSelectedEvent}
                  type="button"
                >
                  Jump replay to this event
                </button>
              </div>

              <div className="mt-5">
                <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                  Raw payload
                </p>
                <pre className="mt-2 overflow-auto rounded-[1.25rem] bg-slate-950 px-4 py-4 text-xs leading-6 text-slate-100">
                  {JSON.stringify(selectedEvent.data, null, 2)}
                </pre>
              </div>
            </div>
          </div>
        )}

        <div className="pointer-events-none absolute inset-x-0 bottom-0 z-20 flex justify-center px-4 pb-4">
          <div className="pointer-events-auto w-[1600px] max-w-[calc(100vw-2rem)] rounded-[1.8rem] border border-slate-200 bg-white/96 px-5 py-4 shadow-[0_30px_80px_rgba(148,163,184,0.26)] backdrop-blur">
            <div className="grid items-center gap-4 grid-cols-[320px_1fr_120px_210px_180px]">
              <div className="flex items-center justify-center gap-2">
                <div className="w-[110px]">
                  <ControlButton
                    label={isPlaying ? "Pause" : "Play"}
                    onClick={handlePlayPause}
                    primary
                    fullWidth
                  />
                </div>

                <div className="w-[110px]">
                  <ControlButton
                    label="Prev event"
                    onClick={handlePreviousEvent}
                    fullWidth
                  />
                </div>

                <div className="w-[110px]">
                  <ControlButton
                    label="Next event"
                    onClick={handleNextEvent}
                    fullWidth
                  />
                </div>
              </div>

              <div className="min-w-0 space-y-2">
                <div className="flex items-center justify-center gap-4 text-sm text-slate-500 tabular-nums">
                  <span className="whitespace-nowrap">
                    Event {Math.max(replayState.currentEventIndex + 1, 0)} / {loadedSimulation.events.length}
                  </span>

                  <span className="inline-flex min-w-[180px] justify-center whitespace-nowrap">
                    Current simTime: {formatSimTime(replayState.currentSimTime)}
                  </span>

                  <span className="inline-flex min-w-[260px] justify-center whitespace-nowrap">
                    Current timestamp: {formatTimestamp(replayState.currentTimestamp)}
                  </span>
                </div>

                <input
                  className="w-full"
                  type="range"
                  min={-1}
                  max={Math.max(loadedSimulation.events.length - 1, -1)}
                  step={1}
                  value={replayState.currentEventIndex}
                  onChange={(event) => handleScrubToEvent(Number(event.target.value))}
                />
              </div>

              <div className="flex justify-center">
                <label className="flex h-[42px] w-[110px] items-center justify-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
                  <span>Speed</span>
                  <select
                    className="bg-transparent text-slate-900 outline-none"
                    value={playbackSpeed}
                    onChange={(event) => setPlaybackSpeed(Number(event.target.value))}
                  >
                    {[1, 2, 4, 8, 16].map((speed) => (
                      <option key={speed} value={speed}>
                        {speed}x
                      </option>
                    ))}
                  </select>
                </label>
              </div>

              <div className="flex justify-center">
                <div className="w-[210px]">
                  <ControlButton
                    label="Go to the latest event"
                    onClick={jumpToLatestEvent}
                    fullWidth
                  />
                </div>
              </div>

              <div className="flex justify-center">
                <label className="flex h-[42px] w-[180px] cursor-pointer items-center justify-center rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 transition hover:border-sky-300 hover:bg-sky-50">
                  <input
                    className="hidden"
                    type="file"
                    accept=".log,.jsonl,.txt,application/json"
                    onChange={handleFileChange}
                  />
                  Upload another log
                </label>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f8fbff_30%,#eef4ff_100%)] px-4 py-4 text-slate-900 md:px-6">
      <div className="pointer-events-none sticky top-5 z-20 flex justify-center">
        <div className="pointer-events-auto flex items-center gap-2 rounded-full border border-slate-200 bg-white/92 p-2 shadow-[0_24px_70px_rgba(148,163,184,0.22)] backdrop-blur">
          <TabButton
            active={false}
            label="Timeline"
            onClick={() => setActiveTab("timeline")}
          />
          <TabButton
            active
            label="Statistics"
            onClick={() => setActiveTab("statistics")}
          />
        </div>
      </div>

      <div className="mx-auto mt-4 max-w-[1500px] rounded-[2rem] border border-slate-200 bg-white/92 p-5 shadow-[0_32px_100px_rgba(148,163,184,0.16)]">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <h2 className="mt-1 text-3xl font-semibold tracking-tight text-slate-950">
              Statistics
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              Replay derived performance metrics and entity state at the current point in time.
            </p>
          </div>

          <div className="rounded-[1.4rem] border border-sky-200 bg-sky-50 px-5 py-4 shadow-sm">
            <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-sky-700">
              Current replay
            </p>
            <p className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
              {formatSimTime(replayState.currentSimTime)}
            </p>
            <p className="mt-1 text-sm text-slate-500">
              {formatTimestamp(replayState.currentTimestamp)}
            </p>
          </div>
        </div>

        <div className="mt-6 grid gap-4 lg:grid-cols-3">
          <KpiCard
            label="Shipments received"
            value={replayState.metrics.shipmentsReceived}
            accent="sky"
          />
          <KpiCard
            label="Shipments ready"
            value={replayState.metrics.shipmentsReady}
            accent="amber"
          />
          <KpiCard
            label="Shipments packed"
            value={replayState.metrics.shipmentsPacked}
            accent="emerald"
          />
          <KpiCard
            label="Shipments shipped"
            value={replayState.metrics.shipmentsShipped}
            accent="violet"
          />
          <KpiCard
            label="Truck arrivals"
            value={replayState.metrics.truckArrivals}
            accent="rose"
          />
          <KpiCard
            label="Events processed"
            value={replayState.metrics.eventsProcessed}
            accent="slate"
          />
        </div>

        <div className="mt-6 space-y-4">
          <EntityAccordion
            defaultOpen
            title="Shipments"
            count={Object.keys(replayState.shipmentsById).length}
            rows={Object.values(replayState.shipmentsById).map((shipment) =>
              buildShipmentRow(shipment),
            )}
          />
          <EntityAccordion
            title="Ports"
            count={Object.keys(replayState.portsById).length}
            rows={Object.values(replayState.portsById).map((port) =>
              buildPortRow(port),
            )}
          />
          <EntityAccordion
            title="Bins"
            count={Object.keys(replayState.binsById).length}
            rows={Object.values(replayState.binsById).map((bin) => buildBinRow(bin))}
          />
        </div>
      </div>
    </div>
  );
}

function buildShipmentRow(shipment: ShipmentState) {
  return {
    key: shipment.id,
    title: shipment.id,
    badge: shipment.status,
    fields: [
      { label: "Status", value: shipment.status },
      {
        label: "Sorting direction",
        value: shipment.sortingDirection ?? "Unavailable",
      },
      {
        label: "Handling flags",
        value: formatStringList(shipment.handlingFlags),
      },
      { label: "Grid", value: shipment.gridId ?? "Unavailable" },
      { label: "Active port", value: shipment.activePortId ?? "Unavailable" },
      { label: "Items", value: formatItemQuantities(shipment.items) },
      {
        label: "Received at",
        value:
          shipment.receivedAtSimTime === null
            ? "Unavailable"
            : `${shipment.receivedAtSimTime}s`,
      },
      {
        label: "Ready at",
        value:
          shipment.readyAtSimTime === null ? "Unavailable" : `${shipment.readyAtSimTime}s`,
      },
      {
        label: "Packed at",
        value:
          shipment.packedAtSimTime === null
            ? "Unavailable"
            : `${shipment.packedAtSimTime}s`,
      },
      {
        label: "Picked bins",
        value: shipment.pickedBinIds.length
          ? shipment.pickedBinIds.join(", ")
          : "None recorded",
      },
    ],
  };
}

function buildPortRow(port: PortState) {
  return {
    key: port.id,
    title: port.id,
    badge: port.status,
    fields: [
      { label: "Status", value: port.status },
      { label: "Grid", value: port.gridId ?? "Unavailable" },
      {
        label: "Active shipment",
        value: port.activeShipmentId ?? "Unavailable",
      },
      { label: "Active bin", value: port.activeBinId ?? "Unavailable" },
      {
        label: "Handling flags",
        value: port.handlingFlags.length
          ? port.handlingFlags.join(", ")
          : "None",
      },
      {
        label: "Last transition",
        value: port.lastTransition ?? "Unavailable",
      },
    ],
  };
}

function buildBinRow(bin: BinState) {
  return {
    key: bin.id,
    title: bin.id,
    badge: bin.status,
    fields: [
      { label: "Status", value: bin.status },
      { label: "Grid", value: bin.gridId ?? "Unavailable" },
      { label: "Port", value: bin.portId ?? "Unavailable" },
      { label: "Shipment", value: bin.shipmentId ?? "Unavailable" },
      { label: "Bin stock", value: formatItemQuantities(bin.binStock) },
      {
        label: "Items picked",
        value: formatItemQuantities(bin.itemsPicked),
      },
      {
        label: "Last event index",
        value:
          bin.lastEventIndex === null ? "Unavailable" : String(bin.lastEventIndex + 1),
      },
    ],
  };
}

function TabButton({
  active,
  label,
  onClick,
}: {
  active: boolean;
  label: string;
  onClick: () => void;
}) {
  return (
    <button
      className={cn(
        "rounded-full px-4 py-2 text-sm font-medium transition",
        active
          ? "bg-slate-950 text-white shadow-sm"
          : "bg-slate-100 text-slate-600 hover:bg-slate-200 hover:text-slate-950",
      )}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}

function ControlButton({
  label,
  onClick,
  primary = false,
  fullWidth = false,
}: {
  label: string;
  onClick: () => void;
  primary?: boolean;
  fullWidth?: boolean;
}) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center rounded-full px-4 py-2 text-sm font-medium transition whitespace-nowrap",
        fullWidth ? "w-full" : "",
        primary
          ? "bg-sky-600 text-white hover:bg-sky-700"
          : "border border-slate-200 bg-white text-slate-700 hover:border-sky-300 hover:text-slate-950",
      )}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}

function DetailCard({
  label,
  value,
  multiline = false,
}: {
  label: string;
  value: string;
  multiline?: boolean;
}) {
  return (
    <div className="rounded-[1.1rem] border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        {label}
      </p>
      <p
        className={cn(
          "mt-2 text-sm font-medium text-slate-900",
          multiline ? "whitespace-pre-line break-words" : "",
        )}
      >
        {value}
      </p>
    </div>
  );
}

function BottomMeta({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.2rem] border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">
        {label}
      </p>
      <p className="mt-2 text-sm font-medium text-slate-900">{value}</p>
    </div>
  );
}

function KpiCard({
  label,
  value,
  accent,
}: {
  label: string;
  value: number;
  accent: "sky" | "amber" | "emerald" | "violet" | "rose" | "slate";
}) {
  const accents = {
    sky: "from-sky-50 to-sky-100/70 text-sky-700",
    amber: "from-amber-50 to-amber-100/70 text-amber-700",
    emerald: "from-emerald-50 to-emerald-100/70 text-emerald-700",
    violet: "from-violet-50 to-violet-100/70 text-violet-700",
    rose: "from-rose-50 to-rose-100/70 text-rose-700",
    slate: "from-slate-50 to-slate-100/70 text-slate-700",
  };

  return (
    <div className="rounded-[1.6rem] border border-slate-200 bg-white p-5 shadow-sm">
      <p className="text-sm text-slate-500">{label}</p>
      <p className="mt-2 text-4xl font-semibold tracking-tight text-slate-950">
        {value.toLocaleString()}
      </p>
    </div>
  );
}

function EntityAccordion({
  title,
  count,
  rows,
  defaultOpen = false,
}: {
  title: string;
  count: number;
  rows: Array<{
    key: string;
    title: string;
    badge: string;
    fields: Array<{ label: string; value: string }>;
  }>;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="rounded-[1.6rem] border border-slate-200 bg-white p-4 shadow-sm">
      <button
        className="flex w-full items-center justify-between gap-3 rounded-[1.2rem] text-left transition hover:bg-slate-50"
        onClick={() => setIsOpen((current) => !current)}
        type="button"
      >
        <div>
          <p className="text-lg font-semibold text-slate-950">{title}</p>
          <p className="text-sm text-slate-500">{count} tracked entities</p>
        </div>
        <span
          className={cn(
            "flex h-10 w-10 items-center justify-center rounded-full border transition",
            isOpen
              ? "border-slate-900 bg-slate-900 text-white"
              : "border-slate-200 bg-slate-100 text-slate-500",
          )}
        >
          <svg
            className={cn("h-5 w-5 transition-transform duration-200", isOpen ? "rotate-180" : "rotate-0")}
            viewBox="0 0 20 20"
            fill="none"
            aria-hidden="true"
          >
            <path
              d="M5 7.5L10 12.5L15 7.5"
              stroke="currentColor"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
            />
          </svg>
        </span>
      </button>

      {isOpen && (
        <div className="mt-4 space-y-3">
          {rows.length ? (
            rows.map((row) => (
              <div
                key={row.key}
                className="rounded-[1.3rem] border border-slate-200 bg-slate-50 px-4 py-4"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-semibold text-slate-950">
                    {row.title}
                  </span>
                  <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {row.badge}
                  </span>
                </div>

                <div className="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                  {row.fields.map((field) => (
                    <div
                      key={`${row.key}-${field.label}`}
                      className="rounded-[1rem] border border-white bg-white px-3 py-3"
                    >
                      <p className="text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-400">
                        {field.label}
                      </p>
                      <p className="mt-2 whitespace-pre-line break-words text-sm leading-6 text-slate-700">
                        {field.value}
                      </p>
                    </div>
                  ))}
                </div>
              </div>
            ))
          ) : (
            <div className="rounded-[1.2rem] border border-dashed border-slate-200 px-4 py-5 text-sm text-slate-500">
              No entities are visible at the current replay point.
            </div>
          )}
        </div>
      )}
    </div>
  );
}
