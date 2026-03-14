package lt.bananull.whse.sim;

import lt.bananull.whse.dto.dataset.BinDto;
import lt.bananull.whse.dto.dataset.BinItemDto;
import lt.bananull.whse.dto.dataset.ShipmentDto;
import lt.bananull.whse.load.SimulationState;

import java.util.LinkedHashMap;
import java.util.Map;

public class BasicShipmentProcessor {

    public Result process(SimulationState state) {
        Map<String, Map<String, Integer>> remainingByBin = buildMutableStock(state);

        int packedCount = 0;
        int failedCount = 0;

        for (ShipmentDto shipment : state.shipments()) {
            String matchingBinId = findMatchingBin(remainingByBin, shipment);
            if (matchingBinId == null) {
                failedCount++;
                continue;
            }

            Map<String, Integer> binStock = remainingByBin.get(matchingBinId);
            shipment.items().forEach((ean, qty) ->
                    binStock.put(ean, binStock.get(ean) - qty));
            packedCount++;
        }

        return new Result(packedCount, failedCount, remainingByBin);
    }

    private static Map<String, Map<String, Integer>> buildMutableStock(SimulationState state) {
        Map<String, Map<String, Integer>> remainingByBin = new LinkedHashMap<>();
        for (BinDto bin : state.bins()) {
            Map<String, Integer> stock = new LinkedHashMap<>();
            for (Map.Entry<String, BinItemDto> item : bin.itemsInBin().entrySet()) {
                stock.put(item.getKey(), item.getValue().quantity());
            }
            remainingByBin.put(bin.id(), stock);
        }
        return remainingByBin;
    }

    private static String findMatchingBin(Map<String, Map<String, Integer>> remainingByBin, ShipmentDto shipment) {
        for (Map.Entry<String, Map<String, Integer>> entry : remainingByBin.entrySet()) {
            Map<String, Integer> stock = entry.getValue();
            boolean matches = shipment.items().entrySet().stream()
                    .allMatch(required -> stock.getOrDefault(required.getKey(), 0) >= required.getValue());
            if (matches) {
                return entry.getKey();
            }
        }
        return null;
    }

    public record Result(
            int packedShipments,
            int failedShipments,
            Map<String, Map<String, Integer>> remainingBinQuantities
    ) {}
}
