package com.example.alvaro.compactcalendarviewfork.calendarview;

import com.example.alvaro.compactcalendarviewfork.calendarview.comparators.EventComparator;
import com.example.alvaro.compactcalendarviewfork.calendarview.domain.Event;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.example.alvaro.compactcalendarviewfork.calendarview.CompactCalendarView.MONTHLY;

public class EventsContainer {

    private Map<String, List<Events>> eventsByKeyAndYearMap = new HashMap<>();
    private Comparator<Event> eventsComparator = new EventComparator();
    private Calendar eventsCalendar;
    private int CalendarFormat;

    public EventsContainer(Calendar eventsCalendar, int format) {
        this.eventsCalendar = eventsCalendar;
        this.CalendarFormat = format;
    }

    void addEvent(Event event) {
        eventsCalendar.setTimeInMillis(event.getTimeInMillis());
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForKey = eventsByKeyAndYearMap.get(key);
        if (eventsForKey == null) {
            eventsForKey = new ArrayList<>();
        }
        Events eventsForTargetDay = getEventDayEvent(event.getTimeInMillis());
        if (eventsForTargetDay == null) {
            List<Event> events = new ArrayList<>();
            events.add(event);
            eventsForKey.add(new Events(event.getTimeInMillis(), events));
        } else {
            eventsForTargetDay.getEvents().add(event);
        }
        eventsByKeyAndYearMap.put(key, eventsForKey);
    }

    void removeAllEvents() {
        eventsByKeyAndYearMap.clear();
    }

    void addEvents(List<Event> events) {
        int count = events.size();
        for (int i = 0; i < count; i++) {
            addEvent(events.get(i));
        }
    }

    List<Event> getEventsFor(long epochMillis) {
        Events events = getEventDayEvent(epochMillis);
        if (events == null) {
            return new ArrayList<>();
        } else {
            return events.getEvents();
        }
    }

    List<Events> getEventsForMonthAndYear(int month, int year) {
        return getEventsForKeyAndYear(month, year);
    }

    List<Events> getEventsForWeekAndYear(int week, int year) {
        return getEventsForKeyAndYear(week, year);
    }

    private List<Events> getEventsForKeyAndYear(int key, int year){
        return eventsByKeyAndYearMap.get(year + "_" + key);
    }

    List<Event> getEventsForMonth(long eventTimeInMillis) {
        eventsCalendar.setTimeInMillis(eventTimeInMillis);
        String keyForCalendarEvent = getKeyForCalendarEvent(eventsCalendar);
        List<Events> events = eventsByKeyAndYearMap.get(keyForCalendarEvent);
        List<Event> allEventsForMonth = new ArrayList<>();
        if (events != null) {
            for (Events eve : events) {
                if (eve != null) {
                    allEventsForMonth.addAll(eve.getEvents());
                }
            }
        }
        Collections.sort(allEventsForMonth, eventsComparator);
        return allEventsForMonth;
    }

    private Events getEventDayEvent(long eventTimeInMillis) {
        eventsCalendar.setTimeInMillis(eventTimeInMillis);
        int dayInMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
        String keyForCalendarEvent = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthsAndYear = eventsByKeyAndYearMap.get(keyForCalendarEvent);
        if (eventsForMonthsAndYear != null) {
            for (Events events : eventsForMonthsAndYear) {
                eventsCalendar.setTimeInMillis(events.getTimeInMillis());
                int dayInMonthFromCache = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                if (dayInMonthFromCache == dayInMonth) {
                    return events;
                }
            }
        }
        return null;
    }

    void removeEventByEpochMillis(long epochMillis) {
        eventsCalendar.setTimeInMillis(epochMillis);
        int dayInMonth = eventsCalendar.get(Calendar.DAY_OF_MONTH);
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthAndYear = eventsByKeyAndYearMap.get(key);
        if (eventsForMonthAndYear != null) {
            Iterator<Events> calendarDayEventIterator = eventsForMonthAndYear.iterator();
            while (calendarDayEventIterator.hasNext()) {
                Events next = calendarDayEventIterator.next();
                eventsCalendar.setTimeInMillis(next.getTimeInMillis());
                int dayInMonthFromCache = eventsCalendar.get(Calendar.DAY_OF_MONTH);
                if (dayInMonthFromCache == dayInMonth) {
                    calendarDayEventIterator.remove();
                    return;
                }
            }
        }
    }

    void removeEvent(Event event) {
        eventsCalendar.setTimeInMillis(event.getTimeInMillis());
        String key = getKeyForCalendarEvent(eventsCalendar);
        List<Events> eventsForMonthAndYear = eventsByKeyAndYearMap.get(key);
        if (eventsForMonthAndYear != null) {
            for (Events events : eventsForMonthAndYear) {
                int indexOfEvent = events.getEvents().indexOf(event);
                if (indexOfEvent >= 0) {
                    events.getEvents().remove(indexOfEvent);
                    return;
                }
            }
        }
    }

    void removeEvents(List<Event> events) {
        int count = events.size();
        for (int i = 0; i < count; i++) {
            removeEvent(events.get(i));
        }
    }

    //E.g. 4 2016 becomes 2016_4
    private String getKeyForCalendarEvent(Calendar cal) {
        cal.setMinimalDaysInFirstWeek(1);
        String key;
        if (CalendarFormat == MONTHLY) {
            key = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.MONTH);
        } else {
            key = cal.get(Calendar.YEAR) + "_" + cal.get(Calendar.WEEK_OF_YEAR);
        }
        return key;
    }

}
