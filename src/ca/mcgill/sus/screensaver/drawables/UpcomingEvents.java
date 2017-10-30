package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
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
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Stage;

public class UpcomingEvents implements Drawable {
	
	private final Queue<String> entries = new ConcurrentLinkedQueue<>();
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry = "";
	private final static String[] titles = {"upcoming", "stay in the know", "don't be lazy", "good CTFers are informed CTFers"};
	private final int color, maxAlpha;	//maximum alpha value for the marquee during fadein 
	private final WebTarget icalServer = ClientBuilder
			.newBuilder()
			.register(JacksonFeature.class)
			.build()
			.target("https://calendar.google.com/calendar/ical"); 
	private final String icsPath = ***REMOVED***;
	private Stage parent;
	

	/**Constructor
	 * @param y		//the y position
	 * @param color	//the colour of the text
	 */
	public UpcomingEvents(int y, int color) 
	{
		startDataFetch();
		this.y = y;		//y position
		//handles the colour. calibrates the maximum alpha to not excede the alpha specified
		this.color = 0xffffff & color; //the text colour
		if (((color >> 24) & 0xff) > 0) 
			{maxAlpha = (color >> 24) & 0xff;} 
		else 
			{maxAlpha = 0xff;}
	}
	
	/* (non-Javadoc)
	 * 
	 * @see ca.mcgill.sus.screensaver.Drawable#draw(java.awt.Graphics2D, int, int)
	 */
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) 
	{
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		//draws the "CURRENT VOLUNTEERS ON DUTY"
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(new Color((maxAlpha << 24) | color, true));
		String title = getTitle();
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		//draws the person logged in
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry) 
		{
			g.setColor(new Color((alphaEntry << 24) | color, true));	//sets the colour for the font
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 40); //draws the font
		}
	}
	
	/**Fetches the data to populate the messages
	 */
	public void startDataFetch() {
		final Runnable dataFetch = new Runnable() {
			public void run() 
			{
				ICalendar ical = Biweekly.parse(icalServer.path(icsPath).request(MediaType.TEXT_PLAIN).get(String.class)).first();
				TimezoneInfo tzInfo = ical.getTimezoneInfo();
				entries.clear();
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
				for (Pair<Date, VEvent> event : events) {
					Date d = event.getValue0();
					VEvent e = event.getValue1();
					Calendar c = GregorianCalendar.getInstance();
					c.setTime(d);
					Calendar oneWeek = GregorianCalendar.getInstance();
					oneWeek.add(Calendar.DATE, 6);
					boolean isSoon = c.before(oneWeek);
					String dateFormat = (isSoon ? "E": "MMM d") + (c.get(Calendar.MINUTE) == 0 ? " @ h a" : " @ h:mm a");
					entries.add(new SimpleDateFormat(dateFormat).format(d) + " - " + e.getSummary().getValue());
				}
				if (marqueeHandle == null) 
					{startMarquee();}
				
				System.out.println("Marquee Done");
			}
		};
		
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 10, TimeUnit.MINUTES);
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}
	
	/**Changes from one entry to another, with a fadeout/fadein effect
	 * @param entry	The thing to fade
	 * 
	 */
	public void changeEntry(final String entry) 
	{
		new Thread("Change Entry") 
		{
			@Override
			public void run() 
			{
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaEntry > 0)
				{
					alphaEntry--;
					if (parent != null) parent.safeRepaint();
					UpcomingEvents.sleep(fadeInMs / maxAlpha);
				}
				synchronized(UpcomingEvents.this.currentEntry) 
				{
					UpcomingEvents.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) 
				{
					alphaEntry++;
					if (parent != null) parent.safeRepaint();
					UpcomingEvents.sleep(fadeOutMs / maxAlpha);
				}
			}
		}.start();
	}
	
	/**A function which causes the thread to wait for a time 
	 * @param ms	The time for which the thread should do nothing
	 */
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	/**Starts the Marquee and iterates through all of the items
	 * 
	 */
	public void startMarquee() {
		final Runnable marquee = new Runnable() {
			Iterator<String> iterNames = entries.iterator();
			public void run() {
				try {
					if (!iterNames.hasNext()) 
						{iterNames = entries.iterator();}
					if (iterNames.hasNext())
						{changeEntry(iterNames.next());}
					if (parent != null) parent.safeRepaint();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		if (marqueeHandle != null) marqueeHandle.cancel(false);
		marqueeHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(marquee, 0, 5, TimeUnit.SECONDS);
	}
	
	public void stopMarquee() {
		if (marqueeHandle != null) marqueeHandle.cancel(false);
	}
	
	public static String getTitle() {
		MessageDigest md5;
		try {
			md5 = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
		String hash = new BigInteger(1, md5.digest(("" + new SimpleDateFormat("yyyy-MM-dd'T'HH").format(new Date())).getBytes(Charset.forName("UTF-8")))).toString(16);
		double ind = (double) Integer.parseInt(hash.substring(0,2) + hash.substring(hash.length()-2), 16) / 0xffff;
		return titles[(int) Math.floor(ind * titles.length)].toUpperCase();
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

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}

}
