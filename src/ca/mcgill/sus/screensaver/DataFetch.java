package ca.mcgill.sus.screensaver;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.javatuples.Pair;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import biweekly.io.TimezoneAssignment;
import biweekly.io.TimezoneInfo;
import biweekly.property.DateStart;
import biweekly.util.com.google.ical.compat.javautil.DateIterator;
import ca.mcgill.sus.screensaver.io.MarqueeData;
import ca.mcgill.sus.screensaver.io.PrintJob;
import ca.mcgill.sus.screensaver.io.PrintQueue;

public class DataFetch extends Thread {
	
	public static final int interval = 60;
 
	private DataFetch() {
		this.setDaemon(true);
		this.start();
	}
	
	private static DataFetch INSTANCE;
	
	public static synchronized DataFetch getInstance() {
		if (INSTANCE == null) INSTANCE = new DataFetch();
		return INSTANCE;
	}
	
	private final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl); 
	private final WebTarget icalServer = ClientBuilder
			.newBuilder()
			.register(JacksonFeature.class)
			.build()
			.target("https://calendar.google.com/calendar/ical"); 
	private final String icsPath = ***REMOVED***;
	private final Queue<Runnable> listeners = new ConcurrentLinkedQueue<>();
	
	//models
	public final Map<String, Boolean> printerStatus = new ConcurrentHashMap<>();
	public final Map<String, List<PrintJob>> jobData = new TreeMap<String, List<PrintJob>>();
	public final Queue<MarqueeData> marqueeData = new ConcurrentLinkedQueue<>();
	public final Queue<String> upcomingEvents = new ConcurrentLinkedQueue<>();
	
	@Override
	public void run() {
		while (!Thread.interrupted()) {
			Future<Map<String, Boolean>> futureStatus = tepidServer.path("/queues/status").request(MediaType.APPLICATION_JSON).async().get(new GenericType<Map<String, Boolean>>(){});
			Future<PrintQueue[]> futureQueues = tepidServer.path("queues").request(MediaType.APPLICATION_JSON).async().get(PrintQueue[].class);
			Future<MarqueeData[]> futureMarquee = tepidServer.path("marquee").request(MediaType.APPLICATION_JSON).async().get(MarqueeData[].class);
			Future<String> futureEvents = Main.OFFICE_COMPUTER ? icalServer.path(icsPath).request(MediaType.TEXT_PLAIN).async().get(String.class) : null;
			try {
				//update marquee data
				List<MarqueeData> newMarquee = Arrays.asList(futureMarquee.get());
				marqueeData.clear();
				marqueeData.addAll(newMarquee);
				
				//update printer status
				Map<String, Boolean> newStatus = futureStatus.get();
				printerStatus.clear();
				printerStatus.putAll(newStatus);
				
				//process and update printer queues
				PrintQueue[] printers = futureQueues.get();	
				Calendar calendar = GregorianCalendar.getInstance(); 
				calendar.setTime(new Date());
				calendar.set(Calendar.HOUR, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0); //sets the calendar to the start of the day
				Map<String, Future<List<PrintJob>>> futureJobs = new HashMap<>();
				Map<String, List<PrintJob>> newJobs = new HashMap<>();
				//iterates over each queue and gets a list of jobs sent to them
				for (PrintQueue q : printers) {
					if (printerStatus.get(q.name)) {						
						futureJobs.put(q.name, tepidServer
										.path("queues").path(q.name)  	//path to specific queue
										.queryParam("limit", 13)		//will return the last 13 print jobs, which is what we had before
										.queryParam("from", calendar.getTimeInMillis())
										.request(MediaType.APPLICATION_JSON).async()
										.get(new GenericType <List<PrintJob>>(){}));
						
					} else {
						newJobs.put(q.name, new ArrayList<PrintJob>(0));
					}
				}
				for (Entry<String, Future<List<PrintJob>>> e : futureJobs.entrySet()) {
					newJobs.put(e.getKey(), e.getValue().get());
				}
				jobData.clear();
				jobData.putAll(newJobs);
				
				//process upcoming events (if this is an office computer) 
				if (Main.OFFICE_COMPUTER) {
					ICalendar ical = Biweekly.parse(futureEvents.get()).first();
					TimezoneInfo tzInfo = ical.getTimezoneInfo();
					Date rightNow = new Date();
					Semester currentSemester = getSemester(rightNow);
					List<VEvent> rawEvents = ical.getEvents();
					List<Pair<Date, VEvent>> events = new ArrayList<>();
					//filter events (remove past events, only include soonest instance of recurring event, make sure it's current semester)
					for (VEvent e : rawEvents) {
						Date soonest = null;
						for (DateIterator iter = e.getDateIterator(getTimezone(tzInfo, e)); iter.hasNext();) {
							Date d = iter.next();
							if (d.before(rightNow) || getSemester(d) != currentSemester) continue;
							if (soonest == null || d.before(soonest)) soonest = d;
						}
						if (soonest != null) events.add(new Pair<Date, VEvent>(soonest, e));
					}
					Collections.sort(events, new Comparator<Pair<Date, VEvent>>() {
						@Override
						public int compare(Pair<Date, VEvent> e1, Pair<Date, VEvent> e2) {
							return e1.getValue0().compareTo(e2.getValue0());
						}
					});
					//format into human-friendly strings
					upcomingEvents.clear();
					for (Pair<Date, VEvent> event : events) {
						Date d = event.getValue0();
						VEvent e = event.getValue1();
						Calendar c = GregorianCalendar.getInstance();
						c.setTime(d);
						Calendar oneWeek = GregorianCalendar.getInstance();
						oneWeek.add(Calendar.DATE, 6);
						boolean isSoon = c.before(oneWeek);
						String dateFormat = (isSoon ? "E": "MMM d") + (c.get(Calendar.MINUTE) == 0 ? " @ h a" : " @ h:mm a");
						upcomingEvents.add(new SimpleDateFormat(dateFormat).format(d) + " - " + e.getSummary().getValue());
					}
				}
				
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
			for (Runnable listener : listeners) {
				listener.run();
			}
			try {
				Thread.sleep(interval * 1000);
			} catch (InterruptedException e) {
				break;
			}
		}
		System.out.println("Data fetch thread over and out");
	}
	
	private static TimeZone getTimezone(TimezoneInfo tzInfo, VEvent e) {
		DateStart dtstart = e.getDateStart();
		TimeZone timezone;
		if (tzInfo.isFloating(dtstart)){
		  timezone = TimeZone.getDefault();
		} else {
		  TimezoneAssignment dtstartTimezone = tzInfo.getTimezone(dtstart);
		  timezone = (dtstartTimezone == null) ? TimeZone.getTimeZone("UTC") : dtstartTimezone.getTimeZone();
		}
		return timezone;
	}
	
	private static enum Semester {
		FALL, WINTER, SPRING
	}
	private static Semester getSemester(Date d) {
		Calendar c = GregorianCalendar.getInstance();
		c.setTime(d);
		int month = c.get(Calendar.MONTH);
		if (month > Calendar.AUGUST) return Semester.FALL;
		if (month > Calendar.APRIL) return Semester.SPRING;
		return Semester.WINTER;
	}
	
	public void addChangeListener(Runnable listener) {
		listeners.add(listener);
	}
}
