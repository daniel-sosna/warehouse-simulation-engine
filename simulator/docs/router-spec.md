# Router

_Provided by **Boozt** team_

This document provides an overview and technical details of the **Router** application.

## Overview
The Router is a Go-based service designed to process a warehouse state (shipments, stock, grid configurations, and truck schedules) and generate optimized shipment assignments for picking and packing.

## Data Formats
### ISO 8601 Timestamps
The application uses standard ISO 8601 (RFC 3339) date-time strings for all timestamp fields.
- **Input fields:** `state.now`, `created_at`, `start_at`, `end_at`.
- **Example:** `2026-03-13T09:30:00Z`

### JSON Schema
The application communicates via JSON over `stdin` (input) and `stdout` (output).

#### Input Structure (`State`)
- `now`: The current simulation time (ISO 8601).
- `shipments_backlog`: List of shipments to be processed.
  - `id`: Unique shipment identifier.
  - `created_at`: Creation time (ISO 8601).
  - `items`: Map of EAN to quantity.
  - `handling_flags`: List of flags (e.g., `"priority"`, `"fragile"`).
  - `sorting_direction`: String representing the sorting direction.
- `stock_bins`: Current inventory distributed across bins and grids.
  - `bin_id`: Unique bin identifier.
  - `grid_id`: Grid identifier where the bin is located.
  - `items`: Map of EAN to quantity.
- `truck_arrival_schedules`: Defines when trucks arrive for specific sorting directions.
  - `truck_arrival_schedules`: List of schedules.
    - `sorting_direction`: Direction associated with this schedule.
    - `pull_times`: List of "HH:MM" pull times.
    - `weekdays`: List of days (e.g., "Monday", "Tuesday").
- `grids`: Physical warehouse layout with shifts and port configurations.
  - `id`: Grid identifier.
  - `shifts`: List of time-bound operational windows.
    - `start_at`, `end_at`: ISO 8601 start/end times.
    - `port_config`: List of active ports during this shift.
      - `port_id` or `port_index`: Port identifiers (strings).
      - `handling_flags`: Allowed shipment types for this port.

#### Output Structure (`Response`)
- `assignments`: A list of `Assignment` objects.
  - `shipment_id`: ID of the shipment.
  - `priority`: A calculated numeric value (higher means sooner packing).
  - `packing_grid`: The grid chosen for packing this shipment.
  - `picks`: A list of individual pick operations (EAN, BinID, Quantity).

## Core Logic

### 1. Shipment Prioritization
Shipments are sorted based on the following hierarchy:
1.  **Priority Flag:** Shipments with the `"priority"` handling flag are moved to the front.
2.  **Near Truck Deadline:** Shipments for a sorting direction with a truck pull time within the next **5 hours** are processed next. These are handled in a **round-robin** fashion across different directions to ensure fairness.
3.  **FIFO:** Within the same priority group, shipments are sorted by their `created_at` timestamp (earliest first).

### 2. Capacity Management
The number of shipments processed in a single run is dynamic and calculated per grid:
- **Active Ports:** Ports configured in shifts that cover the current time (`state.now`).
- **Per-Port Capacity:** Each active port provides capacity for:
  - **25 Packing Shipments:** Shipments that can be fully satisfied by stock from a single grid.
  - **25 Consolidation Shipments:** Shipments that require items from multiple grids (bin consolidation).
- **Handling Flags:** A shipment is only routed to a grid if at least one active port on that grid supports all of the shipment's `handling_flags`. If no port in any grid supports all of a shipment's required flags, the shipment is skipped.
- **Grid Selection:** The router selects the best eligible grid (matching flags and available capacity) that minimizes inter-grid transfers.
- **Fallback:** If no eligible grid is found for a shipment (e.g., due to capacity limits or flag mismatches), it is not routed in the current run.

### 3. Grid & Pick Optimization
- **Grid Selection:** For each shipment, the router selects a `packing_grid` that minimizes inter-grid transfers. It simulates picking for each grid and counts the cost of picks required from other grids.
- **Pick Strategy:** When building picks for a shipment, the router prioritizes bins located in the selected `packing_grid`. If the required quantity cannot be fulfilled from the packing grid, it draws from other grids.

## Error Handling
- Errors are written to `stderr`.

## Execution

### Run
The router expects input via `stdin`:
```bash
cat router_test_input.json | ./router
```

Support for gzipped input and output:
```bash
cat router_test_input.json | gzip | ./router --gzip | gunzip > output.json
```

Write output to a file:
```bash
cat router_test_input.json | ./router > output.json
```
