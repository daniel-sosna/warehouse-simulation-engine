# Warehouse Simulation Hackathon Context

_Provided by **Boozt** team_

**Goal:** Implement the Warehouse Simulator as a separate service that communicates with the Router via `stdin` and `stdout`
Basic Router implementation is provided.

## 1. Overview
The Warehouse Simulator is a discrete-event simulation engine designed to model the operations of a highly automated warehouse. It focuses on the interplay between incoming orders (Shipments), inventory storage (Bins), automated storage and retrieval systems (Grids), and manual or semi-automated packing stations (Ports).

The primary goal of the simulator is to accurately track the lifecycle of shipments from arrival to dispatch, accounting for constraints like physical location of stock, port availability, worker shifts, and bin transfer times.

## 2. Core Entities

### 2.1 Shipment
A Shipment represents a customer order consisting of one or more items (EANs).
- **Status Lifecycle**: `Received` -> `Routed` -> `Consolidation` -> `Ready` -> `Picking` -> `Packed` -> `Shipped`.
- **Handling Flags**: Special requirements (e.g., "fragile", "beauty") that must match Port capabilities.
- **Sorting Direction**: Determines which truck the shipment will be loaded onto.

### 2.2 Bin
A Bin is a physical container that holds inventory.
- **Location**: Bins are always located in a specific **Grid**.
- **Stock**: Bins contain specific quantities of one or more EANs.
- **Status**: `Available`, `Reserved` (locked to a specific port), or `Outside` (moving on a conveyor belt).

### 2.3 Grid
A Grid (e.g., AutoStore) is a self-contained storage area housing Bins and Ports.
- **Inter-Grid Transfers**: Bins can be moved between grids via conveyors.
- **Queue**: Shipments assigned to a grid but not yet assigned to a specific port wait in the Grid Queue.

### 2.4 Packing Port
A Port is a station where items are picked from Bins and packed into Shipments.
- **Status**: `Closed`, `Idle`, `Busy`, `PendingClose`.
- **Queue**: Each port has a limited capacity (default 20) for assigned shipments.
- **Compatibility**: A port can only process shipments if its `HandlingFlags` match the shipment's requirements.

## 3. Simulation Engine

### 3.1 Time Handling
- The simulation uses a continuous representation of time (seconds from `SimulationStartTime` to `SimulationEndTime`).
- All durations (picking, delivery, transfers) are calculated using a base duration modified by a randomness factor (Min/Max).

### 3.2 Event-Driven Architecture
The simulator processes an ordered queue of events. Key events include:
- `ShipmentReceived`: Adds a new shipment to the system.
- `ShipmentRouterTriggered`: Calls the external Router to assign shipments to specific Grids/Bins.
- `BinArrivedAtPort`: Signals that a requested Bin is ready for picking at a Port.
- `BinPickCompleted`: Completes a single/multi items pick; may trigger the next pick or mark the shipment as `Packed`.
- `TruckArrived`: Triggers the shipping of all `Packed` shipments assigned to its sorting direction.
- `BinTransferCompleted`: Moves a Bin from a source Grid to a destination Grid.

## 4. Key Processes

### 4.1 Routing Logic
Shipments must be routed (by calling external Router process) before they can be processed. Routing involves:
1. Identifying Bins that contain the required items.
2. Selecting a target Grid where the picking will occur.
3. Reserving the required quantities in the selected Bins.

### 4.2 Port Assignment
Once a shipment is `Ready` (all items reserved in the same grid), it is assigned to a Port:
- Ports are selected based on availability, compatibility, and the shortest current queue.
- If no port is available, the shipment waits in the Grid Queue.

### 4.3 Picking Process
1. When a Port starts work on a shipment, it requests the first available Bin for the shipment (one bin at a time).
2. The Bin is marked as `Reserved` and moved to the port (calculating `Grid Delivery Time` based on throughput).
3. Once the Bin arrives, a `BinPickCompleted` event is scheduled based on the shipment type (Standard vs. Fragile throughput).
4. After picking, the Bin is returned to the grid or moved to another port if requested.

### 4.4 Consolidation & Transfers
If a shipment's required items are spread across different grids:
1. One grid is selected as the destination.
2. Bins from other grids are scheduled for transfer via conveyors.
3. The shipment only becomes `Ready` once all its reserved Bins have arrived at the destination Grid.

### 4.5 Shifts and Breaks
- Ports operate according to defined `Shifts`.
- `Break Schedules` within a shift temporarily pause port activity.
- If a port is `Busy` when a break or shift ends, it enters `PendingClose` status and finishes the current shipment before closing.

## 5. Performance Metrics
The simulator tracks and logs:
- **Primary Metrics:**
  - Packed On Time: A shipment is considered "packed on time" if the duration between its reception (`ShipmentDate`) and its completion (`CompletionTime`) is within **24 hours**.
  - Priority Fulfillment: % of priority shipments that were shipped by the very first truck arriving after they reached `Packed` status.
  - Port Utilization: Time ports spent in `Busy` status.
  - Lead Time: Time from `Received` to `Shipped`.
  - Average Dwell Time (time spent as `Packed` before being `Shipped`).
- **Secondary Metrics:**
  - Total shipments packed vs. not packed vs. shipped.
  - Total items packed.
  - Total bins transferred between each grid.
  - Average shipments per truck.

## 6. Implementation Difficulty Levels
The following levels define a recommended sequence for developing and testing the simulation's features.

### Level 1: Basic Operations
- **One Grid & One Packing Port**: Initial setup with a single storage area and one functional port.
- **Shipment Lifecycle**: Implementing `Received`, `Routed`, `Picking`, and `Packed` statuses.
- **Single Bin Delivery**: Basic time-based delivery from grid to port.

### Level 2: Scalability (Multi-Ports)
- **Multi-Port Support**: Handling multiple ports within the same grid.
- **Work Assignment**: Implementing the least-queue-size strategy and random tie-breaking.
- **Port Queue Management**: Handling port queue capacities.

### Level 3: Priority Handling
- **Shipment Priority**: Introducing the "priority" flag for shipments.
- **Queue Re-ordering**: Giving priority to marked shipments during routing or assignment.

### Level 4: Specialized Handling (Fragile)
- **Fragile Items**: Introducing shipments marked as "fragile".
- **Port Compatibility**: Ports must have the "fragile" flag to process these shipments.
- **Reduced Throughput**: Fragile items could take twice as long to pick (i.e. 70 units/hour vs 140).

### Level 5: Operational Breaks
- **Port Breaks**: Introducing `BreakSchedules` for individual ports.
- **Port States**: Handling `Closed` status during breaks and returning unassigned work to the grid queue.
- **Overtime Handling**: Implementing `PendingClose` status to allow ports to finish current work.

### Level 6: Shift Management
- **Grid Shifts**: Defining active periods for all ports within a grid.
- **Recurring Schedules**: Auto-rescheduling shifts every 24 hours.

### Level 7: Logistics (Truck Arrivals)
- **Truck Schedules**: Arrivals based on fixed "Pull Times" and sorting directions.
- **Dispatching**: Marking shipments as `Shipped` when their respective truck arrives.
- **Performance Tracking**: Measuring "Packed on Time" vs truck pull times.

### Level 8: Distributed Storage (Multi-Grid & Transfers)
- **Multi-Grid Operations**: Shipments requiring items from different grids.
- **Bin Transfers**: Moving bins between grids via conveyors using `BinTransferStarted` and `BinTransferCompleted` events.
- **Consolidation**: Coordinating multiple bin arrivals before a shipment becomes `Ready`.

### Level 9: Grid Management (Bin Balancing)
- **Bin Balancing**: Ensuring grid capacity is maintained by sending one bin back to the source grid every time a bin is requested from another grid (one-in-one-out).
- **Return Transfer Logic**: When a bin is transferred from Grid A to Grid B for a shipment, the simulation should immediately schedule a transfer of an available (non-reserved, non-outside) bin from Grid B to Grid A to maintain the balance.

## 7. Technical Data Specifications

### 7.1 Input Data Formats (Initial Loading)
The simulator loads state from a data directory (e.g., `./data/1/`) containing:
- `bins.json`: Initial bin locations and stock levels.
- `grids.json`: Grid definitions including ports, shifts, and breaks.
- `shipments.json`: Customer orders to be processed.
- `parameters.json`: Simulation parameters.

### 7.2 External Router Interface (JSON over Stdin/Stdout)
The Router by default is called every 900s.

#### Input (to Router stdin)
```json
{
  "state": {
    "now": "2026-03-13T10:00:00Z",
    "shipments_backlog": [
      {
        "id": "SHIP-PRIORITY-1",
        "created_at": "2026-03-13T09:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": ["priority"],
        "sorting_direction": "DIR-A"
      },
      {
        "id": "SHIP-PRIORITY-2",
        "created_at": "2026-03-13T08:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": ["priority"],
        "sorting_direction": "DIR-B"
      },
      {
        "id": "SHIP-DEADLINE-A-1",
        "created_at": "2026-03-13T07:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-A"
      },
      {
        "id": "SHIP-DEADLINE-B-1",
        "created_at": "2026-03-13T07:05:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-B"
      },
      {
        "id": "SHIP-DEADLINE-A-2",
        "created_at": "2026-03-13T07:10:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-A"
      },
      {
        "id": "SHIP-FIFO-1",
        "created_at": "2026-03-13T05:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-C"
      },
      {
        "id": "SHIP-FIFO-2",
        "created_at": "2026-03-13T06:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-C"
      },
      {
        "id": "SHIP-FRAGILE-HEAVY",
        "created_at": "2026-03-13T04:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": ["fragile", "heavy"],
        "sorting_direction": "DIR-C"
      },
      {
        "id": "SHIP-CONSOLIDATION",
        "created_at": "2026-03-13T03:00:00Z",
        "items": { "EAN-1": 1, "EAN-2": 1 },
        "handling_flags": [],
        "sorting_direction": "DIR-C"
      },
      {
        "id": "SHIP-UNSUPPORTED-FLAGS",
        "created_at": "2026-03-13T02:00:00Z",
        "items": { "EAN-1": 1 },
        "handling_flags": ["hazardous"],
        "sorting_direction": "DIR-C"
      }
    ],
    "stock_bins": [
      {
        "bin_id": "BIN-AS1-1",
        "grid_id": "AS1",
        "items": { "EAN-1": 20 }
      },
      {
        "bin_id": "BIN-AS2-1",
        "grid_id": "AS2",
        "items": { "EAN-2": 20 }
      }
    ],
    "truck_arrival_schedules": [
      {
        "sorting_direction": "dir-1",
        "pull_times": ["14:00", "18:00"],
        "weekdays": ["Monday", "Tuesday"]
      }
    ],
    "grids": [
      {
        "id": "AS1",
        "shifts": [
          {
            "start_at": "2026-03-13T06:00:00Z",
            "end_at": "2026-03-13T18:00:00Z",
            "break_schedules": [
              {
                "start_at": "2026-03-13T12:00:00Z",
                "end_at": "2026-03-13T13:00:00Z"
              }
            ],
            "port_config": [
              {
                "port_id": "P1",
                "handling_flags": ["fragile", "heavy"]
              }
            ]
          }
        ]
      },
      {
        "id": "AS2",
        "shifts": [
          {
            "start_at": "2026-03-13T06:00:00Z",
            "end_at": "2026-03-13T18:00:00Z",
            "break_schedules": [
              {
                "start_at": "2026-03-13T12:00:00Z",
                "end_at": "2026-03-13T13:00:00Z"
              }
            ],
            "port_config": [
              {
                "port_id": "P2",
                "handling_flags": ["priority", "standard"]
              }
            ]
          }
        ]
      }
    ]
  }
}
```

#### Output (from Router stdout)
```json
{
  "assignments": [
    {
      "shipment_id": "SHIP-PRIORITY-2",
      "priority": 10,
      "packing_grid": "AS2",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-PRIORITY-1",
      "priority": 9,
      "packing_grid": "AS2",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-DEADLINE-A-1",
      "priority": 8,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-DEADLINE-B-1",
      "priority": 7,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-DEADLINE-A-2",
      "priority": 6,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-CONSOLIDATION",
      "priority": 4,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        },
        {
          "ean": "EAN-2",
          "bin_id": "BIN-AS2-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-FRAGILE-HEAVY",
      "priority": 3,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-FIFO-1",
      "priority": 2,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    },
    {
      "shipment_id": "SHIP-FIFO-2",
      "priority": 1,
      "packing_grid": "AS1",
      "picks": [
        {
          "ean": "EAN-1",
          "bin_id": "BIN-AS1-1",
          "qty": 1
        }
      ]
    }
  ]
}
```

## 8. Operational Rules & Edge Cases

### 8.1 Bin Reservation & Waiting List
- **One-by-One Requests**: A Port requests only one required bin at a time. It will not request the next bin until the current pick is finished.
- **Reservation Logic**: When a bin is requested, it is marked `Reserved`. If the bin is already `Reserved` or `Outside`, the port enters a **First-Come-First-Served (FCFS)** waiting list on that bin.
- **Release & Notification**: When a bin becomes `Available` again, the next port in the waiting list is automatically notified and granted a reservation.

### 8.2 Shift & Break Transitions
- **PendingClose**: If a shift ends or a break starts while a port is `Busy`, it finishes the current shipment and then transitions to `Closed`.
- **Queue Rollback**: When a port closes, any shipments still in its local queue are returned to the **back** of the general Grid Queue.
- **Router Rollback**: Shipments that are `Routed`,`Ready` or `Consolidation` but not yet in a port's queue or have active transfers are rolled back to `Received`, their bin reservations cleared before each router run.

### 8.3 Event Priority
- The simulation is a discrete-event engine using a min-priority queue ordered by time.
- If multiple events have the exact same timestamp, they are processed in the order they were scheduled (FIFO).

## 9. Default Configuration
All the default parameters can be overridden by parameters via parameters file (`parameters.json`) in a dataset (except for the ones that are updated via command line arguments).

The simulation uses the following default parameters:

### 9.1 Command Line Arguments
- `--dataDir`: Directory containing simulation data (default: `./data`).
- `--router`: Command to run the external router (default: `./build/router`).
- `--eventLogFile`: Path to the event log file (default: `./simulation.log`).

### 9.2 Picking Throughput
- **Standard Handling**: 140 units per hour.
- **Fragile Handling**: 70 units per hour.
- **Pick Duration Randomness**: 0.8 to 1.2 (applied to base duration).

### 9.3 Grid & Bin Delivery
- **Port Queue Capacity**: 20 shipments.
- **Grid-to-Port Bin Delivery Times**:
  - **AS1**: 6s average (0.8 to 1.2 randomness).
  - **AS2**: 4s average (0.8 to 1.2 randomness).
  - **AS3**: 5s average (0.8 to 1.2 randomness).

### 9.4 Transfers & Conveyors
- **Inter-Grid Transfer Durations**:
  - AS1 <-> AS2: 60s (1000 bins/hour)
  - AS1 <-> AS3: 240s (800 bins/hour)
  - AS2 <-> AS3: 360s (600 bins/hour)
- **Transfer Duration Randomness**: 0.8 to 1.2.
- **Transfer Throughput Randomness**: 0.9 to 1.1.

## 10. Simulation Event Log Format
The simulator produces a JSON-line formatted log file (default: `simulation.log`). Each line is a standalone JSON object representing a simulation event.

### 10.1 Event Log Entry Structure
```json
{
  "simTime": 1234,
  "timestamp": "2026-03-01T06:00:00Z",
  "event": "BinPickCompleted",
  "data": { ... }
}
```
- **simTime**: Total seconds elapsed since the simulation start.
- **timestamp**: RFC3339 time within the simulation.
- **event**: The type of event (e.g., `ShipmentReceived`, `BinArrivedAtPort`, `BinPickCompleted`).
- **data**: Event-specific payload.

### 10.2 Key Event Payloads
To support detailed visualization, many events include duration information:
- **BinRequestedAtPort**: Includes `EstimatedDeliveryTime` (seconds).
- **BinArrivedAtPort**: Same data as request, triggers at arrival.
- **BinPickCompleted**: Includes `PickDuration` (seconds).
- **ShipmentPacked**: Includes `PackingDuration` (seconds).
- **BinTransferStarted**: Includes `TransferDuration` (seconds) and `ArrivalTime`.
- **ShipmentReceived**: Basic shipment details.
- **TruckArrived**: Sorting direction that arrived.

## 11. Bonus Points

### 11.1 Visualization & UI
Contestants can earn **bonus points** by building a UI or visualization tool that can replay the simulation from the `simulation.log` file.

Some examples of possible features:
1.  **Replay events** in chronological order.
2.  **Visualize durations**: Use the provided duration fields (`PickDuration`, `TransferDuration`, etc.) to animate bins moving, workers picking, or trucks arriving.
3.  **Show state transitions**: Display the real-time status of Ports (Busy/Idle/Closed) and Grids.
4.  **Track Metrics**: Show live counters for shipped items, lead times, and port utilization.

### 11.2 Advanced Router
Additional **bonus points** can be earned by building a more advanced Router.

The default router provided is a simple reference implementation. A more advanced router could:
1.  **Optimize Bin Requests**: Minimize the number of bin movements by grouping shipments that require the same bins.
2.  **Smart Port Allocation**: Balance the workload across available Ports and Grids to maximize throughput.
3.  **Priority Handling**: Intelligently prioritize shipments based on their deadline (truck arrival times) and importance.
