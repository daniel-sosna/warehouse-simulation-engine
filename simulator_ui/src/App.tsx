import {
  ChangeEvent,
  PointerEvent as ReactPointerEvent,
  useEffect,
  useRef,
  useState,
} from "react";
import { NormalizedEvent } from "./lib/types";
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
    stepNextGroup,
    stepPreviousGroup,
    scrubToEvent,
  } = useSimulationStore();
  const [activeTab, setActiveTab] = useState<AppTab>("timeline");
  const [selectedEvent, setSelectedEvent] = useState<NormalizedEvent | null>(null);
  const [mapOffset, setMapOffset] = useState({ x: 72, y: 36 });
  const [isPanning, setIsPanning] = useState(false);
  const dragStateRef = useRef<{
    startX: number;
    startY: number;
    originX: number;
    originY: number;
    moved: boolean;
  } | null>(null);

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
    setMapOffset({ x: 72, y: 36 });
  }, [loadedSimulation?.sourceName]);

  async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) {
      return;
    }

    const text = await file.text();
    loadFromText(file.name, text);
    event.target.value = "";
  }

  function handlePointerDown(event: ReactPointerEvent<HTMLDivElement>) {
    dragStateRef.current = {
      startX: event.clientX,
      startY: event.clientY,
      originX: mapOffset.x,
      originY: mapOffset.y,
      moved: false,
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
    }

    setMapOffset({
      x: dragState.originX + deltaX,
      y: dragState.originY + deltaY,
    });
  }

  function handlePointerUp() {
    setIsPanning(false);
    window.setTimeout(() => {
      dragStateRef.current = null;
    }, 0);
  }

  if (!loadedSimulation) {
    return (
      <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#eff6ff_45%,#eef2ff_100%)] px-4 py-6 text-slate-900 md:px-8">
        <div className="mx-auto flex min-h-[calc(100vh-3rem)] max-w-5xl items-center justify-center">
          <div className="w-full max-w-3xl rounded-[2rem] border border-slate-200 bg-white/90 p-8 shadow-[0_40px_120px_rgba(148,163,184,0.18)] backdrop-blur md:p-12">
            <div className="space-y-4 text-center">
              <p className="text-xs font-semibold uppercase tracking-[0.28em] text-sky-600">
                Warehouse Simulation Visualizer
              </p>
              <h1 className="text-4xl font-semibold tracking-tight text-slate-950">
                Upload a `simulation.log` to start replaying the warehouse story
              </h1>
              <p className="mx-auto max-w-2xl text-base leading-7 text-slate-600">
                The app will parse the JSONL log in-browser, preserve same-time
                ordering, and unlock the timeline and statistics views once the
                file is valid.
              </p>
            </div>

            <div className="mt-10 rounded-[1.75rem] border border-dashed border-sky-200 bg-sky-50/80 p-8">
              <label className="flex cursor-pointer flex-col items-center gap-4 rounded-[1.5rem] border border-white bg-white px-6 py-10 text-center shadow-sm transition hover:border-sky-300 hover:shadow-md">
                <input
                  className="hidden"
                  type="file"
                  accept=".log,.jsonl,.txt,application/json"
                  onChange={handleFileChange}
                />
                <span className="rounded-full bg-sky-100 px-4 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-sky-700">
                  simulation.log
                </span>
                <span className="text-lg font-medium text-slate-900">
                  Choose a file to load the replay
                </span>
                <span className="text-sm text-slate-500">
                  JSONL only. Unknown or sparse events will still load safely.
                </span>
              </label>
            </div>

            {parseIssues.length > 0 && (
              <div className="mt-8 rounded-[1.5rem] border border-rose-200 bg-rose-50 p-5">
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
      </div>
    );
  }

  const timelineGroups = loadedSimulation.eventGroups.map((group, index) => {
    const lane = index % 3;
    return {
      ...group,
      x: 140 + index * 260,
      y: 110 + lane * 170,
      revealed:
        replayState.currentSimTime !== null &&
        group.simTime <= replayState.currentSimTime,
    };
  });

  const timelineWidth = Math.max(
    1600,
    timelineGroups.length * 260 + 360,
  );
  const timelineHeight = 620;

  return (
    <div className="min-h-screen bg-[linear-gradient(180deg,#f8fafc_0%,#f8fbff_30%,#eef4ff_100%)] px-4 py-4 text-slate-900 md:px-6">
      <div className="mx-auto flex max-w-[1600px] flex-col gap-4">
        <header className="rounded-[2rem] border border-slate-200 bg-white/90 p-5 shadow-[0_35px_90px_rgba(148,163,184,0.16)] backdrop-blur">
          <div className="flex flex-col gap-5 xl:flex-row xl:items-start xl:justify-between">
            <div className="space-y-2">
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-sky-700">
                Warehouse Simulation Visualizer
              </p>
              <h1 className="text-3xl font-semibold tracking-tight text-slate-950">
                {loadedSimulation.sourceName}
              </h1>
              <p className="max-w-3xl text-sm leading-6 text-slate-600">
                Replay timeline and metrics stay driven by the uploaded
                `simulation.log`. The core parser, grouping, and checkpointed
                replay behavior are unchanged.
              </p>
            </div>

            <div className="grid gap-3 sm:grid-cols-2 xl:grid-cols-4">
              <HeaderStat label="Current simTime" value={formatSimTime(replayState.currentSimTime)} />
              <HeaderStat label="Timestamp" value={formatTimestamp(replayState.currentTimestamp)} />
              <HeaderStat
                label="Replay progress"
                value={`${Math.max(replayState.currentEventIndex + 1, 0)} / ${loadedSimulation.events.length}`}
              />
              <label className="flex cursor-pointer items-center justify-center rounded-[1.25rem] border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-800 transition hover:border-sky-300 hover:bg-sky-50">
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

          <div className="mt-5 flex flex-wrap gap-2">
            <TabButton
              active={activeTab === "timeline"}
              label="Timeline"
              onClick={() => setActiveTab("timeline")}
            />
            <TabButton
              active={activeTab === "statistics"}
              label="Statistics"
              onClick={() => setActiveTab("statistics")}
            />
          </div>
        </header>

        {activeTab === "timeline" ? (
          <section className="relative overflow-hidden rounded-[2rem] border border-slate-200 bg-white/92 shadow-[0_32px_100px_rgba(148,163,184,0.16)]">
            <div className="flex items-center justify-between gap-3 border-b border-slate-100 px-5 py-4">
              <div>
                <h2 className="text-lg font-semibold text-slate-950">
                  Timeline Map
                </h2>
                <p className="text-sm text-slate-500">
                  Drag to pan. Click an event card to inspect it.
                </p>
              </div>
              <div className="rounded-full bg-slate-100 px-3 py-1 text-xs font-medium uppercase tracking-[0.18em] text-slate-500">
                {timelineGroups.filter((group) => group.revealed).length} /{" "}
                {timelineGroups.length} groups revealed
              </div>
            </div>

            <div
              className={cn(
                "relative h-[calc(100vh-21rem)] min-h-[560px] overflow-hidden bg-[radial-gradient(circle_at_top_left,rgba(14,165,233,0.08),transparent_28%),linear-gradient(180deg,#ffffff_0%,#f8fbff_100%)]",
                isPanning ? "cursor-grabbing" : "cursor-grab",
              )}
              onPointerDown={handlePointerDown}
              onPointerMove={handlePointerMove}
              onPointerUp={handlePointerUp}
              onPointerLeave={handlePointerUp}
            >
              <div
                className="absolute left-0 top-0 transition-transform duration-150"
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
                  {timelineGroups.slice(0, -1).map((group, index) => {
                    const nextGroup = timelineGroups[index + 1];
                    if (!nextGroup) {
                      return null;
                    }

                    const lineVisible = group.revealed && nextGroup.revealed;
                    return (
                      <path
                        key={`line-${group.groupIndex}`}
                        d={`M ${group.x + 88} ${group.y + 54} C ${group.x + 150} ${group.y + 54}, ${nextGroup.x - 70} ${nextGroup.y + 54}, ${nextGroup.x} ${nextGroup.y + 54}`}
                        stroke={lineVisible ? "#38bdf8" : "#dbeafe"}
                        strokeDasharray={lineVisible ? "0" : "8 10"}
                        strokeLinecap="round"
                        strokeWidth={lineVisible ? 3 : 2}
                        style={{
                          opacity: lineVisible ? 0.75 : 0.55,
                          transition: "all 260ms ease",
                        }}
                      />
                    );
                  })}
                </svg>

                {timelineGroups.map((group) => {
                  const active = group.groupIndex === replayState.currentGroupIndex;
                  return (
                    <button
                      key={`group-${group.groupIndex}`}
                      className={cn(
                        "absolute w-[176px] rounded-[1.4rem] border bg-white p-4 text-left shadow-[0_18px_45px_rgba(148,163,184,0.12)] transition-all duration-300",
                        active
                          ? "border-sky-400 shadow-[0_20px_45px_rgba(56,189,248,0.20)]"
                          : "border-slate-200 hover:border-sky-300",
                        group.revealed
                          ? "pointer-events-auto translate-y-0 opacity-100"
                          : "pointer-events-none translate-y-3 opacity-0",
                      )}
                      style={{ left: group.x, top: group.y }}
                      onClick={() => {
                        if (dragStateRef.current?.moved) {
                          return;
                        }
                        scrubToEvent(group.endEventIndex);
                        setSelectedEvent(group.events[group.events.length - 1] ?? null);
                      }}
                      type="button"
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span className="rounded-full bg-sky-50 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-[0.18em] text-sky-700">
                          {group.eventCount} event{group.eventCount > 1 ? "s" : ""}
                        </span>
                        <span className="text-xs text-slate-400">
                          #{group.groupIndex + 1}
                        </span>
                      </div>
                      <p className="mt-3 text-base font-semibold text-slate-950">
                        simTime {group.simTime}
                      </p>
                      <p className="mt-1 text-xs leading-5 text-slate-500">
                        {group.timestamp ?? "No timestamp"}
                      </p>
                      <div className="mt-4 space-y-1.5">
                        {group.events.slice(0, 3).map((event) => (
                          <button
                            key={event.index}
                            className="block w-full rounded-xl bg-slate-50 px-2 py-1.5 text-left text-sm text-slate-700 transition hover:bg-sky-50"
                            onClick={(clickEvent) => {
                              clickEvent.stopPropagation();
                              if (dragStateRef.current?.moved) {
                                return;
                              }
                              scrubToEvent(event.index);
                              setSelectedEvent(event);
                            }}
                            type="button"
                          >
                            {event.event}
                          </button>
                        ))}
                        {group.events.length > 3 && (
                          <p className="px-2 text-xs text-slate-400">
                            +{group.events.length - 3} more
                          </p>
                        )}
                      </div>
                    </button>
                  );
                })}

                {replayState.currentSimTime === null && (
                  <div className="absolute left-20 top-16 rounded-[1.5rem] border border-slate-200 bg-white/95 px-5 py-4 text-sm text-slate-600 shadow-lg">
                    Start replaying to reveal the timeline map.
                  </div>
                )}
              </div>
            </div>

            <div className="sticky bottom-0 border-t border-slate-100 bg-white/95 px-5 py-4 backdrop-blur">
              <div className="grid gap-4 xl:grid-cols-[1fr_auto] xl:items-end">
                <div className="space-y-2">
                  <div className="flex items-center justify-between gap-3 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                    <span>Scrub by event</span>
                    <span>
                      {replayState.currentEventIndex < 0
                        ? "Before first event"
                        : `Line ${loadedSimulation.events[replayState.currentEventIndex]?.lineNumber ?? "-"}`}
                    </span>
                  </div>
                  <input
                    className="w-full"
                    type="range"
                    min={-1}
                    max={Math.max(loadedSimulation.events.length - 1, -1)}
                    step={1}
                    value={replayState.currentEventIndex}
                    onChange={(event) => scrubToEvent(Number(event.target.value))}
                  />
                  <div className="flex flex-wrap items-center gap-3 text-sm text-slate-500">
                    <span>
                      Event {Math.max(replayState.currentEventIndex + 1, 0)} /{" "}
                      {loadedSimulation.events.length}
                    </span>
                    <span>
                      Group {Math.max(replayState.currentGroupIndex + 1, 0)} /{" "}
                      {loadedSimulation.eventGroups.length}
                    </span>
                    <span>{formatTimestamp(replayState.currentTimestamp)}</span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2">
                  <ControlButton
                    label={isPlaying ? "Pause" : "Play"}
                    onClick={isPlaying ? pause : play}
                    primary
                  />
                  <ControlButton label="Prev event" onClick={stepPreviousEvent} />
                  <ControlButton label="Next event" onClick={stepNextEvent} />
                  <ControlButton label="Prev group" onClick={stepPreviousGroup} />
                  <ControlButton label="Next group" onClick={stepNextGroup} />
                  <label className="ml-1 flex items-center gap-2 rounded-full border border-slate-200 bg-slate-50 px-3 py-2 text-sm text-slate-600">
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
              </div>
            </div>

            {selectedEvent && (
              <div className="pointer-events-none absolute inset-y-4 right-4 flex w-full max-w-md justify-end">
                <div className="pointer-events-auto max-h-[calc(100%-2rem)] w-full overflow-auto rounded-[1.75rem] border border-slate-200 bg-white p-5 shadow-[0_24px_70px_rgba(148,163,184,0.24)]">
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
                    <DetailCard label="Timestamp" value={formatTimestamp(selectedEvent.timestamp)} />
                    <DetailCard label="Line" value={String(selectedEvent.lineNumber)} />
                    <DetailCard
                      label="Duration"
                      value={
                        selectedEvent.duration === null
                          ? "Unavailable"
                          : `${selectedEvent.duration}s`
                      }
                    />
                  </div>

                  <div className="mt-5">
                    <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                      Payload
                    </p>
                    <pre className="mt-2 overflow-auto rounded-[1.25rem] bg-slate-950 px-4 py-4 text-xs leading-6 text-slate-100">
                      {JSON.stringify(selectedEvent.data, null, 2)}
                    </pre>
                  </div>
                </div>
              </div>
            )}
          </section>
        ) : (
          <section className="rounded-[2rem] border border-slate-200 bg-white/92 p-5 shadow-[0_32px_100px_rgba(148,163,184,0.16)]">
            <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
              <div>
                <h2 className="text-2xl font-semibold text-slate-950">
                  Statistics
                </h2>
                <p className="text-sm text-slate-500">
                  Replay-derived KPI snapshot and current entity state.
                </p>
              </div>
              <div className="text-sm text-slate-500">
                Current replay: {formatSimTime(replayState.currentSimTime)}
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
                rows={Object.values(replayState.shipmentsById).map((shipment) => ({
                  key: shipment.id,
                  primary: shipment.id,
                  secondary: shipment.status,
                  tertiary:
                    shipment.activePortId || shipment.gridId
                      ? `port ${shipment.activePortId ?? "?"} · grid ${shipment.gridId ?? "?"}`
                      : "No active assignment",
                }))}
              />
              <EntityAccordion
                title="Ports"
                count={Object.keys(replayState.portsById).length}
                rows={Object.values(replayState.portsById).map((port) => ({
                  key: port.id,
                  primary: port.id,
                  secondary: port.status,
                  tertiary: port.gridId
                    ? `grid ${port.gridId} · ${port.lastTransition ?? "state change"}`
                    : port.lastTransition ?? "No grid linked yet",
                }))}
              />
              <EntityAccordion
                title="Bins"
                count={Object.keys(replayState.binsById).length}
                rows={Object.values(replayState.binsById).map((bin) => ({
                  key: bin.id,
                  primary: bin.id,
                  secondary: bin.status,
                  tertiary:
                    bin.portId || bin.gridId
                      ? `port ${bin.portId ?? "-"} · grid ${bin.gridId ?? "-"}`
                      : "No location details yet",
                }))}
              />
            </div>
          </section>
        )}
      </div>
    </div>
  );
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

function HeaderStat({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.25rem] border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
        {label}
      </p>
      <p className="mt-2 text-sm font-medium text-slate-900">{value}</p>
    </div>
  );
}

function ControlButton({
  label,
  onClick,
  primary = false,
}: {
  label: string;
  onClick: () => void;
  primary?: boolean;
}) {
  return (
    <button
      className={cn(
        "rounded-full px-4 py-2 text-sm font-medium transition",
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

function DetailCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-[1.1rem] border border-slate-200 bg-slate-50 px-4 py-3">
      <p className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
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
      <div
        className={cn(
          "inline-flex rounded-full bg-gradient-to-r px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em]",
          accents[accent],
        )}
      >
        KPI
      </div>
      <p className="mt-4 text-sm text-slate-500">{label}</p>
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
    primary: string;
    secondary: string;
    tertiary: string;
  }>;
  defaultOpen?: boolean;
}) {
  const [isOpen, setIsOpen] = useState(defaultOpen);

  return (
    <div className="rounded-[1.6rem] border border-slate-200 bg-white p-4 shadow-sm">
      <button
        className="flex w-full cursor-pointer items-center justify-between gap-3 text-left"
        onClick={() => setIsOpen((current) => !current)}
        type="button"
      >
        <div>
          <p className="text-lg font-semibold text-slate-950">{title}</p>
          <p className="text-sm text-slate-500">{count} tracked entities</p>
        </div>
        <span
          className={cn(
            "rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] transition",
            isOpen
              ? "bg-slate-950 text-white"
              : "bg-slate-100 text-slate-500",
          )}
        >
          Toggle
        </span>
      </button>

      {isOpen && (
        <div className="mt-4 space-y-3">
          {rows.length ? (
            rows.map((row) => (
              <div
                key={row.key}
                className="rounded-[1.2rem] border border-slate-200 bg-slate-50 px-4 py-3"
              >
                <div className="flex items-center justify-between gap-3">
                  <span className="text-sm font-medium text-slate-950">
                    {row.primary}
                  </span>
                  <span className="rounded-full bg-white px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.14em] text-slate-500">
                    {row.secondary}
                  </span>
                </div>
                <p className="mt-2 text-sm text-slate-500">{row.tertiary}</p>
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
