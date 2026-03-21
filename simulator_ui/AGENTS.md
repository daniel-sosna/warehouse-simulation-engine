# AGENTS.md

## Project goal

Build a **React + TypeScript warehouse simulation visualizer**.

This app is a **metrics-first replay dashboard** for a `simulation.log` file in **JSONL** format.

The UI should feel:
- polished
- easy to demo
- easy to browse
- easy to understand quickly
- impressive for hackathon judges

This is **not** an animation-heavy simulation game.
Clarity, insight, and usability matter more than flashy motion.

---

## What to read first

Before making substantial changes:

1. Read `README.md`
2. Inspect the sample `simulation.log` files
3. Understand the real event shapes from the logs before making assumptions
4. Propose a short implementation plan before coding

`README.md` contains the **task description / simulator requirements / event meanings / metrics expectations**.

---

## Source of truth

- `simulation.log` is the **only required input**
- Treat `simulation.log` as the **canonical source of truth** for replay order and state transitions
- Preserve original **file order** for events with the same `simTime`
- `simTime` may be **negative**
- Unknown event types must **never crash** the app
- Sparse or partially missing payloads must **never crash** the app

Do **not** assume that every event has rich metadata.
Do **not** assume that every future log will look identical to the sample logs.

---

## Product direction

This product is primarily an **operational insights dashboard**.

Prioritize:
- live metrics
- timeline navigation
- event inspection
- clear derived state
- polished information design
- demo quality

De-prioritize:
- flashy animation
- complex decorative visuals
- unnecessary motion
- hardcoded warehouse assumptions that are not supported by the logs

The UI should make it easy to answer:
- what happened?
- what changed?
- what is happening now?
- what are the key metrics?
- what is blocked, busy, delayed, packed, or shipped?

---

## Core features

The app must support:

- upload of `simulation.log`
- in-browser JSONL parsing
- useful parsing/validation errors with line numbers
- replay controls:
  - play
  - pause
  - next event
  - previous event
  - next same-timestamp group
  - previous same-timestamp group
- timeline scrubbing
- grouped display for events that share the same `simTime`
- live metrics panel
- event inspector
- long-log filtering and navigation
- robust handling of sparse and unknown events

---

## Replay and timeline rules

The simulator is a **discrete-event system**.

Rules:
- process events in ascending `simTime`
- if multiple events share the same `simTime`, preserve original log order
- support both:
  - **event-level stepping** for correctness
  - **same-timestamp group stepping** for usability
- long logs must remain responsive

Build the replay engine so it supports:
- deterministic replays
- backward navigation
- fast scrubbing
- large logs

Use checkpoints / snapshots every N events so backward stepping and scrubbing do not require replaying the full history every time.

---

## Long-log requirements

Real logs may be:
- long
- multi-day
- noisy
- dominated by repeated operational events

The UI must work well with logs containing many repeated:
- `RouterTick`
- `TruckArrival`
- `PortOpens`
- `PortCloses`

The timeline should support:
- filtering by event type
- grouping by same timestamp
- collapsing low-signal events
- quick jumps to important events
- clear navigation through dense sections

The app should feel good to use even on logs with lots of repetitive infrastructure events.

---

## Event importance model

Use an event importance model for default presentation and navigation.

Suggested importance levels:

### Low importance
- `RouterTick`

### Medium importance
- `PortOpens`
- `PortCloses`
- `TruckArrival`

### High importance
- `ShipmentReceived`
- `ShipmentIsReady`
- `PortStartsShipment`
- `BinArrivesAtPort`
- `BinPickCompleted`
- `ShipmentPacked`
- `ShipmentShipped`

Requirements:
- low-importance events may be hidden in the default feed
- hidden events must still be processed internally for replay correctness
- grouped same-timestamp cards should show:
  - time
  - event count
  - ordered list of events in that group

---

## Known event support

Implement robust handling for these event types.

For each event:
- process it safely even if payload data is sparse
- show it clearly in the event feed / inspector
- update derived state conservatively
- never invent missing data that is not supported by the log

### `ShipmentReceived`
Meaning:
- A shipment becomes available to the system.
- In the simulator code, this marks the shipment as received.

Expected state effect:
- create the shipment if not already present
- mark shipment status as **Received**

Important data:
- usually includes `shipmentId`

UI meaning:
- this is the start of a shipment story in the simulation
- good candidate for a high-importance event in the feed

---

### `RouterTick`
Meaning:
- Periodic routing cycle.
- The simulator calls the router to assign or reassign shipments to grids / bins.
- In the provided code, it also checks whether scheduled shipments should now become received.
- It repeats on a fixed interval.

Expected state effect:
- may indirectly cause shipments to become received
- may update routing / assignments
- may trigger dispatching decisions

Important data:
- payload may be empty
- this event is still meaningful even without detailed payload fields

UI meaning:
- treat as low importance by default
- useful for timeline context, but often noisy in long logs

---

### `BinRequestedAtPort`
Meaning:
- A port has requested a bin needed for picking.
- This event is part of the shipment picking flow.
- Exact internal code was not provided here, so treat the behavior conservatively based on logs and surrounding flow.

Expected state effect:
- record that a bin request exists for a given port / shipment when inferable
- mark the port / shipment as waiting for a required bin

Important data:
- payload may be sparse or empty in real logs

UI meaning:
- useful as a “waiting for bin” or “bin requested” state marker
- should not break the UI if key IDs are missing

---

### `BinArrivesAtPort`
Meaning:
- A requested bin has arrived at the port and is ready for picking.
- In the simulator code, this schedules the pick completion event.

Expected state effect:
- mark a bin as present at the port / in active service
- record that the port can proceed with picking
- usually leads toward `BinPickCompleted`

Important data:
- often includes `binId`
- duration may be present at top level

UI meaning:
- a meaningful picking milestone
- good candidate for visual highlighting in the warehouse/state view

---

### `BinPickCompleted`
Meaning:
- Picking from a bin is complete.
- In the provided code, the shipment records the picked bin, stock is deducted from the bin, the bin is released, and if the shipment is fully picked then `ShipmentPacked` is enqueued.

Expected state effect:
- associate the bin with the shipment’s completed picks
- reduce stock / mark bin as released when modeling that level of detail
- if all required bins are picked, transition toward **Packed**

Important data:
- usually includes `shipmentId`
- usually includes `binId`
- duration exists at the top level

UI meaning:
- high-signal event in the picking lifecycle
- good candidate for port activity visualization and shipment progress

---

### `PortStartsShipment`
Meaning:
- A port begins processing a shipment.
- In the provided code, the port starts its next shipment, the shipment enters picking, and bin-request events are created for each required pick.

Expected state effect:
- mark the port as active / busy
- assign or expose the active shipment for that port
- mark shipment as **Picking**
- create or imply pending bin requests

Important data:
- usually includes `gridId`
- usually includes `portId`

UI meaning:
- key transition from waiting/ready state into active work
- should be clearly visible in the port and shipment views

---

### `ShipmentIsReady`
Meaning:
- A shipment has returned from routing and is ready for operational handling.
- In the provided code, the shipment starts consolidation, is marked ready, and is either assigned to an available port or enqueued at the grid.

Expected state effect:
- mark shipment as **Ready**
- possibly assign shipment to a port
- otherwise place shipment in a queue

Important data:
- usually includes `shipmentId`

UI meaning:
- important transition between routing and actual handling
- helpful for queue visualization and shipment lifecycle views

---

### `ShipmentPacked`
Meaning:
- A shipment has finished picking / packing at a port.
- In the provided code, the port completes the active shipment, the shipment is marked packed, and the next shipment may start immediately if the queue is non-empty.

Expected state effect:
- mark shipment as **Packed**
- release the port’s active shipment
- possibly trigger the next shipment on that port

Important data:
- usually includes `shipmentId`

UI meaning:
- major milestone
- strong candidate for metrics and shipment progression views

---

### `TruckArrival`
Meaning:
- A truck arrives for a sorting direction.
- In the provided code, all packed shipments matching that direction are scheduled to be shipped.

Expected state effect:
- record truck arrival moment
- capture how many shipments were taken for shipping when available
- may immediately coincide with `ShipmentShipped` events at the same `simTime`

Important data:
- may include `sortingDirection`
- may include `shipmentsTakenForShipping`

UI meaning:
- important logistics event
- useful in metrics such as average shipments per truck
- should be visible in the timeline and in shipment shipping stories

---

### `ShipmentShipped`
Meaning:
- A shipment has been shipped.
- In the provided code, the shipment is marked shipped.

Expected state effect:
- mark shipment status as **Shipped**

Important data:
- usually includes `shipmentId`

UI meaning:
- final shipment lifecycle milestone
- should be emphasized in metrics and narrative views

---

### `PortOpens`
Meaning:
- A port opens for shift start or for break end.
- In the provided code, the port is opened or reopened, and the next closing event is scheduled.
- Real logs may include break-related semantics such as `fromBreak`.

Expected state effect:
- mark port as open / idle / available
- possibly distinguish reopening from break vs opening for shift

Important data:
- usually includes `gridId`
- usually includes `portId`
- usually includes `fromBreak`
- usually includes `handlingFlags`

UI meaning:
- operational availability signal
- useful in port status timelines and port detail panels

---

### `PortCloses`
Meaning:
- A port closes for shift end or break start.
- In the provided code, the port requests closure and the next opening event may be scheduled.
- Real logs may include break-related semantics such as `intoBreak`.

Expected state effect:
- mark port as closed or pending close
- possibly distinguish break start vs shift end

Important data:
- usually includes `gridId`
- usually includes `portId`
- usually includes `intoBreak`
- usually includes `handlingFlags`

UI meaning:
- useful for operational schedule context
- should be visible in the port timeline and availability state

---

## Future / partial support

### `BinTransferStarted`
Meaning:
- A bin transfer between grids has started.
- This is relevant for more advanced levels and may not appear in all logs.

Expected state effect:
- mark bin as in transfer
- record source/destination when known

UI meaning:
- show as transfer activity, but only if the log actually supports it

---

### `BinTransferCompleted`
Meaning:
- A bin transfer between grids has completed.

Expected state effect:
- mark bin at its new location
- close the transfer activity

UI meaning:
- update transfer metrics and location state when derivable

---

## Event handling rules

- Prefer the **real sample logs** over idealized assumptions
- Keep reducer logic conservative
- If a payload is incomplete, store only what is known
- Unknown future events must still appear in the timeline and inspector
- Do not crash when an event is missing fields
---

## Port lifecycle expectations

The UI should distinguish port lifecycle transitions clearly.

When possible, differentiate:
- opening because a shift started
- opening because a break ended
- closing because a shift ended
- closing because a break started

If the log provides enough information, surface those reasons in:
- port details
- tooltips
- timeline annotations
- status displays

If the information is missing, show only what is actually known.

---

## State model expectations

Keep **raw parsed events** separate from **derived replay state**.

Recommended concepts in state:
- `events`
- `eventGroups`
- `checkpoints`
- `currentEventIndex`
- `currentGroupIndex`
- `currentSimTime`
- `currentTimestamp`
- `shipmentsById`
- `binsById`
- `gridsById`
- `portsByGridId`
- `metrics`
- `uiFilters`
- `selectedEvent`
- `selectedEntity`

Do not tightly couple parsing, replay, metrics, and rendering.

---

## Metrics requirements

This is a **metrics-first application**.

Metrics should be prominent and easy to understand.

Expected metrics include, when derivable:
- packed on time
- priority fulfillment
- port utilization
- lead time
- average dwell time
- total shipments packed
- total shipments not packed
- total shipments shipped
- total items packed
- total bins transferred between each grid
- average shipments per truck

Rules:
- compute a metric only when the necessary data truly exists
- if a metric cannot be derived accurately, show it as unavailable
- never invent missing data
- prefer correctness and transparency over pretending completeness

The UI should make metric status obvious:
- exact
- inferred
- unavailable

---

## UI expectations

The UI should be:
- demo-friendly
- judge-friendly
- operator-friendly
- clean
- modern
- polished

It should feel like a compact operational control center.

Recommended layout:

### Top bar
- file upload
- playback controls
- speed controls
- current `simTime`
- current timestamp
- event counter
- quick search / filters

### Left panel
- timeline
- scrubber
- grouped event feed
- filters by event type / importance
- quick jump controls

### Center panel
Use a practical warehouse/state visualization that is easy to read.

Support:
- grid/port status overview
- active shipment visibility when inferable
- queue / flow visibility when inferable
- clear indication of what changed at the current step

This visualization should be useful, not overly theatrical.

### Right panel
- live metrics
- selected shipment details
- selected event details
- selected port/grid/bin details when useful
- metric confidence / availability indicators

### Bottom panel or drawer
- raw event inspector
- parsed payload
- state diff / “what changed”

---

## Shipment narratives

Make shipment stories easy to follow.

The UI should help users understand a shipment lifecycle across events.

Helpful capabilities:
- show a selected shipment’s event trail / mini-timeline
- highlight the shipment currently affected by the selected event
- highlight state changes after each event or event group

A user should be able to quickly explain the simulation flow during a demo.

---

## Animations

Animations should be **minimal but available**.

Use motion only where it improves comprehension:
- subtle transitions
- highlighting changed entities
- timeline scrubbing smoothness
- selection changes
- small status-change animations

Do not rely on animation to make the UI feel impressive.
The app should still look strong with reduced motion.

---

## Technical stack

Use:
- React
- TypeScript
- Vite
- Tailwind CSS
- Zustand or Redux Toolkit
- Recharts

Optional:
- Framer Motion for subtle polish only

Avoid backend dependencies unless absolutely necessary.
Prefer a frontend-only implementation.

---

## Architecture expectations

Recommended structure:
- parser layer for JSONL parsing and validation
- replay engine with checkpoints / snapshots
- domain layer for event handling and derived state
- metrics derivation layer
- UI layer for controls, charts, timeline, and inspectors

Prefer:
- small pure functions
- strongly typed domain models
- clear separation of concerns
- safe fallback handling for unknown events

Do not mix raw log parsing directly into visual components.

---

## Working style

For non-trivial work:

1. Read `README.md` and sample logs first
2. Produce a short plan before coding
3. Implement in small phases
4. Keep the app runnable throughout
5. Summarize:
   - what was built
   - architecture decisions
   - assumptions
   - remaining gaps

When uncertain, prefer:
- inspecting the real logs
- making conservative assumptions
- clearly marking inferred behavior

---

## Done when

A user can:
- upload a real `simulation.log`
- replay the simulation correctly
- move event by event
- move group by group for same-timestamp events
- browse long logs comfortably
- inspect raw events and derived state
- see clear live metrics
- confidently demo the product to hackathon judges

The final result should feel:
- correct
- polished
- understandable
- demo-ready