import {
  ChangeEvent,
  PointerEvent as ReactPointerEvent,
  RefObject,
  useEffect,
  useMemo,
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
type ExplorerEntityType = "shipments" | "ports" | "bins";
type ExplorerRow = {
  key: string;
  title: string;
  badge: string;
  summary: [string, string, string];
  searchText: string;
  fields: Array<{ label: string; value: string }>;
};

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

function estimateTimelineCardHeight(eventName: string) {
  const charsPerLine = 18;
  const lineCount = Math.max(1, Math.ceil(eventName.length / charsPerLine));
  return 122 + Math.max(0, lineCount - 1) * 24;
}

function buildEventStructuredFields(event: NormalizedEvent) {
  const data = event.data as EventData;
  const fields: Array<{ label: string; value: string }> = [];
  const shipmentId = getStringValue(data.shipmentId);
  const packingGrid = getStringValue(data.packingGrid);
  const destGridId = getStringValue(data.destGridId);
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
  if (packingGrid) {
    fields.push({ label: "Packing grid", value: packingGrid });
  }
  if (destGridId) {
    fields.push({ label: "Destination grid", value: destGridId });
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
  const [explorerTab, setExplorerTab] = useState<ExplorerEntityType>("shipments");
  const [explorerSearch, setExplorerSearch] = useState("");
  const [selectedExplorerKey, setSelectedExplorerKey] = useState<string | null>(null);
  const [explorerScrollTop, setExplorerScrollTop] = useState(0);
  const [selectedEvent, setSelectedEvent] = useState<NormalizedEvent | null>(null);
  const [mapOffset, setMapOffset] = useState({ x: 120, y: 120 });
  const [isPanning, setIsPanning] = useState(false);
  const [autoFollow, setAutoFollow] = useState(true);
  const [viewportSize, setViewportSize] = useState({ width: 0, height: 0 });
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
  const explorerListRef = useRef<HTMLDivElement | null>(null);
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
    setExplorerTab("shipments");
    setExplorerSearch("");
    setSelectedExplorerKey(null);
    setExplorerScrollTop(0);
    setSelectedEvent(null);
    setMapOffset({ x: 120, y: 120 });
    pendingOffsetRef.current = { x: 120, y: 120 };
    setAutoFollow(true);
  }, [loadedSimulation?.sourceName]);

  const shipmentRows = useMemo(
    () => Object.values(replayState.shipmentsById).map((shipment) => buildShipmentRow(shipment)),
    [replayState.shipmentsById],
  );
  const portRows = useMemo(
    () => Object.values(replayState.portsById).map((port) => buildPortRow(port)),
    [replayState.portsById],
  );
  const binRows = useMemo(
    () => Object.values(replayState.binsById).map((bin) => buildBinRow(bin)),
    [replayState.binsById],
  );

  const explorerRows = useMemo(() => {
    switch (explorerTab) {
      case "ports":
        return portRows;
      case "bins":
        return binRows;
      case "shipments":
      default:
        return shipmentRows;
    }
  }, [binRows, explorerTab, portRows, shipmentRows]);

  const filteredExplorerRows = useMemo(() => {
    const normalizedSearch = explorerSearch.trim().toLowerCase();
    if (!normalizedSearch) {
      return explorerRows;
    }

    return explorerRows.filter((row) => row.searchText.includes(normalizedSearch));
  }, [explorerRows, explorerSearch]);

  const selectedExplorerRow =
    filteredExplorerRows.find((row) => row.key === selectedExplorerKey) ??
    explorerRows.find((row) => row.key === selectedExplorerKey) ??
    null;

  useEffect(() => {
    setSelectedExplorerKey(null);
    setExplorerSearch("");
    setExplorerScrollTop(0);
    if (explorerListRef.current) {
      explorerListRef.current.scrollTop = 0;
    }
  }, [explorerTab]);

  useEffect(() => {
    setExplorerScrollTop(0);
    if (explorerListRef.current) {
      explorerListRef.current.scrollTop = 0;
    }
  }, [explorerSearch]);

  useEffect(() => {
    if (
      selectedExplorerKey &&
      !explorerRows.some((row) => row.key === selectedExplorerKey)
    ) {
      setSelectedExplorerKey(null);
    }
  }, [explorerRows, selectedExplorerKey]);

const timelineEvents =
  loadedSimulation
    ? (() => {
        const groupSpacing = 360;
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

          const groupEventHeights = groupEvents.map((event) =>
            estimateTimelineCardHeight(event.event),
          );
          const betweenCardSpacing = 32;
          const totalGroupHeight =
            groupEventHeights.reduce((sum, height) => sum + height, 0) +
            Math.max(0, groupSize - 1) * betweenCardSpacing;
          let cursorY = columnCenterY - totalGroupHeight / 2;

          return groupEvents.map((event, eventIndexWithinGroup) => {
            const cardHeight = groupEventHeights[eventIndexWithinGroup] ?? 122;
            const y = cursorY;
            cursorY += cardHeight + betweenCardSpacing;

            return {
              ...event,
              x: columnX,
              y,
              cardHeight,
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
    Math.max(...timelineEvents.map((event) => event.y + event.cardHeight + 80), 980),
  );

  const visibleBounds = useMemo(() => {
    const overscan = 500;

    const viewportWidth = viewportSize.width || window.innerWidth;
    const viewportHeight = viewportSize.height || window.innerHeight;

    const left = -mapOffset.x - overscan;
    const top = -mapOffset.y - overscan;
    const right = -mapOffset.x + viewportWidth + overscan;
    const bottom = -mapOffset.y + viewportHeight + overscan;

    return { left, top, right, bottom };
  }, [mapOffset.x, mapOffset.y, viewportSize.width, viewportSize.height]);

  const visibleTimelineEvents = useMemo(() => {
    const selectedIndex = selectedEvent?.index ?? -1;
    const currentIndex = replayState.currentEventIndex;

    return timelineEvents.filter((event) => {
      const cardLeft = event.x;
      const cardTop = event.y;
      const cardRight = event.x + 208;
      const cardBottom = event.y + event.cardHeight;

      const inViewport =
        cardRight >= visibleBounds.left &&
        cardLeft <= visibleBounds.right &&
        cardBottom >= visibleBounds.top &&
        cardTop <= visibleBounds.bottom;

      const mustKeep =
        event.index === currentIndex || event.index === selectedIndex;

      return inViewport || mustKeep;
    });
  }, [
    timelineEvents,
    visibleBounds,
    replayState.currentEventIndex,
    selectedEvent?.index,
  ]);

  const visibleEventIndexSet = useMemo(() => {
    return new Set(visibleTimelineEvents.map((event) => event.index));
  }, [visibleTimelineEvents]);

  const visibleLinePairs = useMemo(() => {
    return timelineEvents.slice(0, -1).flatMap((event, index) => {
      const nextEvent = timelineEvents[index + 1];
      if (!nextEvent) {
        return [];
      }

      const shouldRender =
        visibleEventIndexSet.has(event.index) ||
        visibleEventIndexSet.has(nextEvent.index);

      return shouldRender ? [{ event, nextEvent }] : [];
    });
  }, [timelineEvents, visibleEventIndexSet]);

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

  useEffect(() => {
    if (activeTab !== "timeline" || !timelineViewportRef.current) {
      return;
    }

    const element = timelineViewportRef.current;

    const updateSize = () => {
      setViewportSize({
        width: element.clientWidth,
        height: element.clientHeight,
      });
    };

    updateSize();

    const observer = new ResizeObserver(() => {
      updateSize();
    });

    observer.observe(element);

    return () => {
      observer.disconnect();
    };
  }, [activeTab, loadedSimulation]);

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
              {visibleLinePairs.map(({ event, nextEvent }) => {
                const lineVisible = event.revealed && nextEvent.revealed;
                const sameGroup = event.groupIndex === nextEvent.groupIndex;

                const d = sameGroup
                  ? `M ${event.x + 104} ${event.y + event.cardHeight} L ${nextEvent.x + 104} ${nextEvent.y}`
                  : `M ${event.x + 208} ${event.y + event.cardHeight / 2} C ${event.x + 280} ${event.y + event.cardHeight / 2}, ${nextEvent.x - 72} ${nextEvent.y + nextEvent.cardHeight / 2}, ${nextEvent.x} ${nextEvent.y + nextEvent.cardHeight / 2}`;

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

            {visibleTimelineEvents.map((event) => {
              const activeReplayEvent = event.index === replayState.currentEventIndex;
              const selectedTimelineEvent = selectedEvent?.index === event.index;

              return (
                <button
                  key={`event-${event.index}`}
                  data-no-pan="true"
                  onPointerDown={(pointerEvent) => {
                    pointerEvent.stopPropagation();
                  }}
                  className={cn("absolute min-h-[122px] w-[208px] rounded-[1.55rem] border bg-white px-4 pb-5 pt-4 text-left shadow-[0_22px_55px_rgba(148,163,184,0.14)] transition-all duration-300",
                    
                    activeReplayEvent
                      ? "border-sky-400 shadow-[0_24px_60px_rgba(14,165,233,0.24)]"
                      : selectedTimelineEvent
                        ? "border-violet-300 shadow-[0_20px_48px_rgba(139,92,246,0.16)]"
                        : "border-slate-200 hover:border-sky-300",
                    event.revealed
                      ? "pointer-events-auto translate-y-0 opacity-100"
                      : "pointer-events-none translate-y-4 opacity-0",
                  )}
                  style={{ left: event.x, top: event.y, minHeight: event.cardHeight }}
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

                  <p className="mt-3 break-words text-base font-semibold leading-5 text-slate-950">
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
                  label="Duration"
                  value={String(selectedEvent.duration)}
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

              {buildEventStructuredFields(selectedEvent).length > 0 && (
                <div className="mt-5">
                  <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                    Key details
                  </p>
                  <div className="mt-2 grid gap-3 sm:grid-cols-2">
                    {buildEventStructuredFields(selectedEvent).map((field) => (
                      <DetailCard
                        key={`${selectedEvent.index}-${field.label}`}
                        label={field.label}
                        value={field.value}
                        multiline
                      />
                    ))}
                  </div>
                </div>
              )}

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
                <label className="relative flex h-[42px] w-[110px] cursor-pointer items-center justify-between gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600 transition hover:border-sky-300 hover:bg-sky-50">
                  <span className="pointer-events-none">Speed</span>
                  <span className="pointer-events-none text-slate-900">
                    {playbackSpeed}x
                  </span>
                  <select
                    className="absolute inset-0 h-full w-full cursor-pointer opacity-0"
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

        <div className="mt-6 rounded-[1.8rem] border border-slate-200 bg-white p-5 shadow-sm">
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">
                Entity Explorer
              </p>
              <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
                Browse replay entities at scale
              </h3>
              <p className="mt-2 text-sm text-slate-500">
                Scan rows quickly, filter the active entity type, and inspect the
                selected entity in the side drawer.
              </p>
            </div>

            <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
              <div className="flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 p-1">
                <ExplorerTabButton
                  active={explorerTab === "shipments"}
                  label="Shipments"
                  onClick={() => setExplorerTab("shipments")}
                />
                <ExplorerTabButton
                  active={explorerTab === "ports"}
                  label="Ports"
                  onClick={() => setExplorerTab("ports")}
                />
                <ExplorerTabButton
                  active={explorerTab === "bins"}
                  label="Bins"
                  onClick={() => setExplorerTab("bins")}
                />
              </div>

              <label className="flex min-w-[280px] items-center gap-3 rounded-[1.2rem] border border-slate-200 bg-slate-50 px-4 py-3">
                <svg
                  className="h-4 w-4 text-slate-400"
                  viewBox="0 0 20 20"
                  fill="none"
                  aria-hidden="true"
                >
                  <path
                    d="M8.75 3.75a5 5 0 1 0 0 10a5 5 0 0 0 0-10ZM13.25 13.25L16.25 16.25"
                    stroke="currentColor"
                    strokeWidth="1.8"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                  />
                </svg>
                <input
                  className="w-full bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
                  placeholder={`Search ${explorerTab}...`}
                  value={explorerSearch}
                  onChange={(event) => setExplorerSearch(event.target.value)}
                />
              </label>
            </div>
          </div>

          <div className="mt-5 grid gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
            <div className="rounded-[1.5rem] border border-slate-200 bg-slate-50/70">
              <div className="grid grid-cols-[minmax(0,1.3fr)_160px_160px_160px] gap-4 border-b border-slate-200 px-4 py-3 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-500">
                <span>{explorerTab.slice(0, 1).toUpperCase() + explorerTab.slice(1, -1)}</span>
                <span>Summary</span>
                <span>Context</span>
                <span>Signals</span>
              </div>

              {filteredExplorerRows.length ? (
                <VirtualizedEntityList
                  listRef={explorerListRef}
                  rows={filteredExplorerRows}
                  selectedKey={selectedExplorerKey}
                  scrollTop={explorerScrollTop}
                  onScroll={setExplorerScrollTop}
                  onSelect={setSelectedExplorerKey}
                />
              ) : (
                <div className="flex h-[560px] items-center justify-center px-6">
                  <div className="max-w-md text-center">
                    <p className="text-lg font-semibold text-slate-900">
                      No matching {explorerTab}
                    </p>
                    <p className="mt-2 text-sm leading-6 text-slate-500">
                      Try a different search term or move replay to a point where
                      more entities exist.
                    </p>
                  </div>
                </div>
              )}
            </div>

            <EntityDetailDrawer
              entityType={explorerTab}
              row={selectedExplorerRow}
            />
          </div>
        </div>
      </div>
    </div>
  );
}

function buildShipmentRow(shipment: ShipmentState) {
  const handlingFlags = formatStringList(shipment.handlingFlags);
  const itemSummary = formatItemQuantities(shipment.items);
  return {
    key: shipment.id,
    title: shipment.id,
    badge: shipment.status,
    summary: [
      shipment.sortingDirection ?? "No sorting direction",
      shipment.activePortId ?? shipment.packingGridId ?? shipment.gridId ?? "No assignment",
      handlingFlags === "Unavailable" ? "No handling flags" : handlingFlags,
    ] as [string, string, string],
    searchText: [
      shipment.id,
      shipment.status,
      shipment.sortingDirection ?? "",
      shipment.gridId ?? "",
      shipment.packingGridId ?? "",
      shipment.activePortId ?? "",
      handlingFlags === "Unavailable" ? "" : handlingFlags,
      itemSummary === "Unavailable" ? "" : itemSummary,
    ]
      .join(" ")
      .toLowerCase(),
    fields: [
      { label: "Status", value: shipment.status },
      {
        label: "Sorting direction",
        value: shipment.sortingDirection ?? "Unavailable",
      },
      {
        label: "Handling flags",
        value: handlingFlags,
      },
      {
        label: "Packing grid",
        value: shipment.packingGridId ?? "Unavailable",
      },
      { label: "Grid", value: shipment.gridId ?? "Unavailable" },
      { label: "Active port", value: shipment.activePortId ?? "Unavailable" },
      { label: "Items", value: itemSummary },
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
  const handlingFlags = port.handlingFlags.length
    ? port.handlingFlags.join(", ")
    : "None";
  return {
    key: port.id,
    title: port.id,
    badge: port.status,
    summary: [
      port.gridId ?? "No grid",
      port.activeShipmentId ?? "No active shipment",
      handlingFlags,
    ] as [string, string, string],
    searchText: [
      port.id,
      port.status,
      port.gridId ?? "",
      port.activeShipmentId ?? "",
      port.activeBinId ?? "",
      handlingFlags,
      port.lastTransition ?? "",
    ]
      .join(" ")
      .toLowerCase(),
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
        value: handlingFlags,
      },
    ],
  };
}

function buildBinRow(bin: BinState) {
  const binStock = formatItemQuantities(bin.binStock);
  const itemsPicked = formatItemQuantities(bin.itemsPicked);
  return {
    key: bin.id,
    title: bin.id,
    badge: bin.status,
    summary: [
      bin.shipmentId ?? "No shipment link",
      bin.status === "outside"
        ? `Moving to ${bin.destGridId ?? "unknown destination"}`
        : bin.portId ?? bin.gridId ?? "No location",
      itemsPicked === "Unavailable" ? "No pick detail" : "Pick detail available",
    ] as [string, string, string],
    searchText: [
      bin.id,
      bin.status,
      bin.shipmentId ?? "",
      bin.portId ?? "",
      bin.gridId ?? "",
      bin.destGridId ?? "",
      binStock === "Unavailable" ? "" : binStock,
      itemsPicked === "Unavailable" ? "" : itemsPicked,
    ]
      .join(" ")
      .toLowerCase(),
    fields: [
      { label: "Status", value: bin.status },
      { label: "Grid", value: bin.gridId ?? "Unavailable" },
      { label: "Destination grid", value: bin.destGridId ?? "Unavailable" },
      { label: "Port", value: bin.portId ?? "Unavailable" },
      { label: "Shipment", value: bin.shipmentId ?? "Unavailable" },
      { label: "Bin stock", value: binStock },
      {
        label: "Items picked",
        value: itemsPicked,
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

function ExplorerTabButton({
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
          : "text-slate-600 hover:bg-white hover:text-slate-950",
      )}
      onClick={onClick}
      type="button"
    >
      {label}
    </button>
  );
}

function VirtualizedEntityList({
  listRef,
  rows,
  selectedKey,
  scrollTop,
  onScroll,
  onSelect,
}: {
  listRef: RefObject<HTMLDivElement>;
  rows: ExplorerRow[];
  selectedKey: string | null;
  scrollTop: number;
  onScroll: (value: number) => void;
  onSelect: (value: string) => void;
}) {
  const rowHeight = 78;
  const viewportHeight = 560;
  const overscan = 6;
  const startIndex = Math.max(0, Math.floor(scrollTop / rowHeight) - overscan);
  const visibleCount = Math.ceil(viewportHeight / rowHeight) + overscan * 2;
  const endIndex = Math.min(rows.length, startIndex + visibleCount);
  const visibleRows = rows.slice(startIndex, endIndex);

  return (
    <div
      ref={listRef}
      className="h-[560px] overflow-auto"
      onScroll={(event) => onScroll(event.currentTarget.scrollTop)}
    >
      <div
        className="relative"
        style={{ height: rows.length * rowHeight }}
      >
        {visibleRows.map((row, index) => {
          const rowIndex = startIndex + index;
          const selected = row.key === selectedKey;

          return (
            <button
              key={row.key}
              className={cn(
                "absolute left-0 right-0 grid grid-cols-[minmax(0,1.3fr)_160px_160px_160px] gap-4 border-b border-slate-200 px-4 py-3 text-left transition",
                selected
                  ? "bg-sky-50"
                  : "bg-transparent hover:bg-white/70",
              )}
              style={{ top: rowIndex * rowHeight, height: rowHeight }}
              onClick={() => onSelect(row.key)}
              type="button"
            >
              <div className="min-w-0">
                <div className="flex items-center gap-2">
                  <span className="truncate text-sm font-semibold text-slate-950">
                    {row.title}
                  </span>
                  <span className="rounded-full bg-white px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {row.badge}
                  </span>
                </div>
              </div>
              {row.summary.map((value, summaryIndex) => (
                <div
                  key={`${row.key}-${summaryIndex}`}
                  className="truncate text-sm text-slate-600"
                >
                  {value}
                </div>
              ))}
            </button>
          );
        })}
      </div>
    </div>
  );
}

function EntityDetailDrawer({
  entityType,
  row,
}: {
  entityType: ExplorerEntityType;
  row: ExplorerRow | null;
}) {
  return (
    <aside className="h-[640px] overflow-hidden rounded-[1.5rem] border border-slate-200 bg-slate-50/70 p-4">
      {row ? (
        <div className="flex h-full flex-col">
          <div className="flex items-start justify-between gap-3">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">
                {entityType.slice(0, 1).toUpperCase() + entityType.slice(1, -1)} detail
              </p>
              <h4 className="mt-2 text-xl font-semibold text-slate-950">
                {row.title}
              </h4>
            </div>
            <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
              {row.badge}
            </span>
          </div>

          <div className="mt-5 flex-1 space-y-3 overflow-auto pr-1">
            {row.fields.map((field) => (
              <div
                key={`${row.key}-${field.label}`}
                className="rounded-[1.1rem] border border-white bg-white px-4 py-3"
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
      ) : (
        <div className="flex h-full min-h-[560px] items-center justify-center">
          <div className="max-w-sm text-center">
            <p className="text-lg font-semibold text-slate-950">
              Select a row to inspect details
            </p>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              The selected {entityType.slice(0, -1)} stays highlighted in the
              explorer while this drawer is open.
            </p>
          </div>
        </div>
      )}
    </aside>
  );
}
