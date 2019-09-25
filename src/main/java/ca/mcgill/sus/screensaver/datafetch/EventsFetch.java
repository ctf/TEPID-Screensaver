package ca.mcgill.sus.screensaver.datafetch;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneInfo;
import org.javatuples.Pair;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class EventsFetch extends DataFetchable<Queue<String>> {

    private Queue<String> events = new LinkedList<>();

    private final WebTarget icalServer;
    private final String icsPath;
    private final long timeOutInterval;

    EventsFetch(long _timeOutInterval,WebTarget _icalServer, String _icsPath){
        timeOutInterval = _timeOutInterval;
        icalServer = _icalServer;
        icsPath = _icsPath;
    }

    @Override
    public FetchResult<Queue<String>> fetch() {
        //process upcoming events (if this is an office computer)
        ICalendar ical;
        try{
            Future<String> futureEvents = icalServer.path(icsPath).request(MediaType.TEXT_PLAIN).async().get(String.class);
            ical = Biweekly.parse(futureEvents.get(timeOutInterval, TimeUnit.SECONDS)).first();
        } catch (Exception e){
            throw new RuntimeException(e);
        }

        Calendar c = Calendar.getInstance();
        Date eventsStart = c.getTime();
        c.add(Calendar.MONTH, 2);
        Date eventsEnd = c.getTime();
        //filter events (remove past events, only include soonest instance of recurring event, make sure it's current semester)
        TimezoneInfo tzInfo = ical.getTimezoneInfo();
        List<Pair<Date, VEvent>> filteredEvents = DataFetch.Semester.filterEvents(ical.getEvents(), eventsStart, eventsEnd, tzInfo);

        //format into human-friendly strings
        for (Pair<Date, VEvent> event : filteredEvents) {
            Date d = event.getValue0();
            VEvent e = event.getValue1();

            Calendar timeOfEvent = GregorianCalendar.getInstance();
            timeOfEvent.setTime(d);
            boolean isOnTheHour = timeOfEvent.get(Calendar.MINUTE) == 0;
            timeOfEvent.set(Calendar.HOUR_OF_DAY, 0);
            timeOfEvent.set(Calendar.MINUTE, 0);
            timeOfEvent.set(Calendar.SECOND, 0);
            timeOfEvent.set(Calendar.MILLISECOND, 0);

            Calendar oneWeek = GregorianCalendar.getInstance();
            oneWeek.add(Calendar.DATE, 6);

            Calendar today = GregorianCalendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            boolean isSoon = timeOfEvent.before(oneWeek),
                    isToday = timeOfEvent.equals(today);
            String dateFormat = (isToday ? "" : (isSoon ? "E": "MMM d")) + (isOnTheHour ? " @ h a" : " @ h:mm a");
            events.add((isToday ? "Today" : "") + new SimpleDateFormat(dateFormat).format(d) + " - " + e.getSummary().getValue());
        }
        System.out.println("Fetched events");
        return new FetchResult<>(events);
    }

}
