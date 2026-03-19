package lt.bananull.whse.service;

import lt.bananull.whse.event.events.TruckArrivalEvent;
import lt.bananull.whse.simulator.SimulationParameters;
import lt.bananull.whse.utils.DateTimeResolver;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TruckArrivalService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("H:mm");

    public static List<TruckArrivalEvent> generateTruckArrivalEvents(
        Instant startTimestamp,
        Instant endTimestamp,
        SimulationParameters.TruckArrivalSchedules schedules
    ) {
        List<TruckArrivalEvent> events = new ArrayList<>();

        for (SimulationParameters.TruckArrivalSchedule schedule : schedules.schedules()) {
            List<Instant> arrivalTimestamps = generateInstants(startTimestamp, endTimestamp, schedule, ZoneId.of("UTC"));
            arrivalTimestamps.forEach(timestamp -> {
                long arrivalSimTime = DateTimeResolver.resolveSimTimeFromTimestamp(timestamp, startTimestamp);
                TruckArrivalEvent truckArrivalEvent = new TruckArrivalEvent(arrivalSimTime,
                    schedule.sortingDirection());
                events.add(truckArrivalEvent);
            });
        }

        return events;
    }

    private static List<Instant> generateInstants(
        Instant startTimestamp,
        Instant endTimestamp,
        SimulationParameters.TruckArrivalSchedule schedule,
        ZoneId zoneId
    ) {
        if (schedule == null) {
            return List.of();
        }

        ZonedDateTime startZdt = startTimestamp.atZone(zoneId);
        ZonedDateTime endZdt = endTimestamp.atZone(zoneId);

        LocalDate startDate = startZdt.toLocalDate();
        LocalDate endDate = endZdt.toLocalDate();

        Set<Instant> result = new HashSet<>();

        Set<DayOfWeek> allowedDays = parseWeekdays(schedule.weekdays());
        List<LocalTime> pullTimes = parsePullTimes(schedule.pullTimes());

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (!allowedDays.contains(date.getDayOfWeek())) {
                continue;
            }

            for (LocalTime time : pullTimes) {
                ZonedDateTime candidateZdt = date.atTime(time).atZone(zoneId);
                Instant candidate = candidateZdt.toInstant();

                // inclusive boundaries: >= startTimestamp and <= endTimestamp
                if (!candidate.isBefore(startTimestamp) && !candidate.isAfter(endTimestamp)) {
                    result.add(candidate);
                }
            }
        }

        return result.stream()
            .sorted(Comparator.naturalOrder())
            .toList();
    }

    private static Set<DayOfWeek> parseWeekdays(List<String> weekdays) {
        if (weekdays == null || weekdays.isEmpty()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }

        EnumSet<DayOfWeek> result = EnumSet.noneOf(DayOfWeek.class);

        for (String day : weekdays) {
            if (day == null || day.isBlank()) {
                continue;
            }

            try {
                result.add(DayOfWeek.valueOf(day.trim().toUpperCase(Locale.ENGLISH)));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid weekday: " + day, e);
            }
        }

        return result;
    }

    private static List<LocalTime> parsePullTimes(List<String> pullTimes) {
        if (pullTimes == null || pullTimes.isEmpty()) {
            return List.of();
        }

        List<LocalTime> result = new ArrayList<>();

        for (String time : pullTimes) {
            if (time == null || time.isBlank()) {
                continue;
            }

            try {
                result.add(LocalTime.parse(time.trim(), TIME_FORMATTER));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid pull time: " + time, e);
            }
        }

        return result;
    }
}
