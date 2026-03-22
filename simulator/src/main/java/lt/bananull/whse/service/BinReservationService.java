package lt.bananull.whse.service;

import lt.bananull.whse.router.dto.PickDto;
import lt.bananull.whse.simulator.Simulator;
import lt.bananull.whse.simulator.entity.Bin;

import java.util.List;

public class BinReservationService {

    public static boolean canReserveAllPicks(Simulator simulator, List<PickDto> picks) {
        for (PickDto p : picks) {
            Bin bin = simulator.getState().getBin(p.binId());
            boolean ok = bin.canReserve();
            if (!ok) {
                return false;
            }
        }
        return true;
    }

    public static void reserveAllPicks(Simulator simulator, String shipmentId, List<PickDto> picks) {
        for (PickDto p : picks) {
            Bin bin = simulator.getState().getBin(p.binId());
            bin.reserveForConsolidation(shipmentId, p.ean(), p.qty());
        }
    }
}
