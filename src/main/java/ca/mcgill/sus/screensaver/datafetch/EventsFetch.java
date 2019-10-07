package ca.mcgill.sus.screensaver.datafetch;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.io.TimezoneInfo;
import biweekly.property.DateStart;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;
import org.javatuples.Pair;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class EventsFetch extends DataFetchable<Queue<String>> {

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
        List<Pair<Date, VEvent>> filteredEvents = Semester.filterEvents(ical.getEvents(), eventsStart, eventsEnd, tzInfo);

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
            value.add((isToday ? "Today" : "") + new SimpleDateFormat(dateFormat).format(d) + " - " + e.getSummary().getValue());
        }
        System.out.println("Fetched events");
        return new FetchResult<>(value);
    }

    private static TimeZone getTimezone(TimezoneInfo tzInfo, VEvent e) {
        DateStart dateStart = e.getDateStart();
        TimeZone timezone;
        if (tzInfo.isFloating(dateStart)){
            timezone = TimeZone.getDefault();
        } else {
            TimezoneAssignment dateStartTimezone = tzInfo.getTimezone(dateStart);
            timezone = (dateStartTimezone == null) ? TimeZone.getTimeZone("UTC") : dateStartTimezone.getTimeZone();
        }
        return timezone;
    }

    public enum Semester {
        FALL, WINTER, SPRING;
        public Semester next() {
            return values()[(this.ordinal() + 1) % values().length];
        }
        private static Semester getSemester(Date d) {
            Calendar c = GregorianCalendar.getInstance();
            c.setTime(d);
            int month = c.get(Calendar.MONTH);
            if (month > Calendar.AUGUST) return Semester.FALL;
            if (month > Calendar.APRIL) return Semester.SPRING;
            return Semester.WINTER;
        }
        static List<Pair<Date, VEvent>> filterEvents(List<VEvent> rawEvents, Date start, Date end, TimezoneInfo tzInfo) {
            List<Pair<Date, VEvent>> events = new ArrayList<>();
            for (VEvent e : rawEvents) {
                Date soonest = null;
                for (DateIterator iter = e.getDateIterator(getTimezone(tzInfo, e)); iter.hasNext();) {
                    Date d = iter.next();
                    if (d.before(start)) continue;
                    if (soonest == null || d.before(soonest)) soonest = d;
                    if (d.after(end)) break;
                }
                if (soonest != null) events.add(new Pair<>(soonest, e));
            }
            events.sort(Comparator.comparing(Pair::getValue0));
            return events;
        }
    }

}
