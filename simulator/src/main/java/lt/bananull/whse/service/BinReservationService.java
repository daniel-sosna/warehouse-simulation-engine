package lt.bananull.whse.service;

import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.List;
import java.util.Set;

public class BinReservationService {

    public static boolean canReserveAllPicks(Simulator simulator, Set<String> binIds) {
        for (String id : binIds) {
            Bin bin = simulator.getState().getBin(id);
            boolean ok = bin.canReserve();
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static void reserveAllPicks(Simulator simulator, String shipmentId, Set<String> binIds) {
        for (String id : binIds) {
            Bin bin = simulator.getState().getBin(id);
            bin.reserveForConsolidation(shipmentId);
        }
    }
}
