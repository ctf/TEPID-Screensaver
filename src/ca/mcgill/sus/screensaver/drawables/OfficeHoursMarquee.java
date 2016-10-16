package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.io.SignUp;

public class OfficeHoursMarquee implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder
			.newBuilder()
			.register(JacksonFeature.class)
			.build()
			.target(Main.serverUrl); //initialises the server as a targetable thing
	
	private final Queue<String> names = new ConcurrentLinkedQueue<>(Arrays.asList(new String[]{}));
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry = "";
	private final String title = "CURRENT VOLUNTEERS ON DUTY";
	private Runnable onChange;
	private final int color, maxAlpha;
	
	private final Random randomizer = new Random(); 	//"necessary" for the random nickname selector
	private final List<String> genericNicknames = Arrays.asList("The Great", "Unnicknamed", "awesomesauce", "Mr. Clean", "Captain America", "Cupcake", "Kiddo", "Stitches", "Spookypants", "Professor", "Bugs", "The Nice", "The Magnificent", "The Clever", "The Kind", "The Courageous", "The Hero", "The Beloved", "McCool", "Bob", "Bob", "Bob", "Bob", "Jimbo", "Oompa", "Loompa", "Finch", "Hawk", "Eagle", "SegFault", "Elmer", "Tweety Bird", "Grover", "Cookie Monster", "Yarwhal", "Spock-lite","Darth", "Skywalker", "");
	
	public OfficeHoursMarquee(int y, int color) {
		startDataFetch();
		this.y = y;
		this.color = 0xffffff & color;
		if (((color >> 24) & 0xff) > 0) { 
			maxAlpha = (color >> 24) & 0xff;
		} else {
			maxAlpha = 0xff;
		}
	}
	
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(new Color((maxAlpha << 24) | color, true));
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry) {
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 40);
		}
	}
	
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable dataFetch = new Runnable() {
			public void run() 
			{
				System.out.println("Fetching OH data");
				names.clear();
				Calendar calendar = GregorianCalendar.getInstance(); //creates a new calendar instance
				calendar.setTime(new Date());
				SimpleDateFormat slotFormat = new SimpleDateFormat ("EEEEE-HH:mm");
				SimpleDateFormat timeFormat = new SimpleDateFormat ("HH:mm");
				SimpleDateFormat dayFormat = new SimpleDateFormat ("EEEEE");
				
				//debug (preset options to set time data) 
				calendar.set(Calendar.MINUTE, 30);
				calendar.set(Calendar.HOUR, 2);
				calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
				//end debug				
				
				calendar.set(Calendar.MINUTE, (calendar.get(Calendar.MINUTE)/30)*30);
				slotFormat.setCalendar(calendar);
				dayFormat.setCalendar(calendar);
				timeFormat.setCalendar(calendar);
				
				List<SignUp> signUps = (tepidServer
						.path("office-hours").path("on-duty")
						.path(slotFormat.format(calendar.getTime()))
						.request(MediaType.APPLICATION_JSON)
						.get(new GenericType <List<SignUp>>(){}));
				String out;
				for (SignUp s : signUps)
				{
					out = "";
					out = (s.getGivenName());
					if (s.getNickname() != null)
					{
						out += " \"" + s.getNickname() + "\" "; //TODO: get lastname here as well
					}
					else
					{
						out += " \"" +  genericNicknames.get(randomizer.nextInt(genericNicknames.size())) + "\" ";
					}
					
					Calendar tempTime = (Calendar) calendar.clone();
					tempTime.add(Calendar.MINUTE, 30);
					for (String slot : (s.getSlots().get(dayFormat.format(calendar.getTime()))))
					{
						//this part will determine whether the person's next slot is contiguous with this one.
						//it increments to the beginning of the next time slot, and then iterates over the person's slots for today looking for one which is equal to it
						//if it finds one, it increments again and continues looking for a slot which is equal to that
						
						if (timeFormat.format(tempTime.getTime()).compareTo(slot) == 0) 
						{
							tempTime.add(Calendar.MINUTE, 30);
						}
					}
					out = out + "until " + timeFormat.format(tempTime.getTime());
//					System.out.println("formedstring " + out);
					if (!out.isEmpty())
					{
						names.add(out);
					}
				}
				
				
//				System.out.println("isEmpty?" + names.isEmpty());
				if (names.isEmpty())
				{
					names.add("Nobody!");
				}
				if (marqueeHandle == null) 
				{
					startMarquee();
				}
				
				System.out.println("Marquee Done");
			}
		};
			
/*
				
		{
				try (Reader r = new InputStreamReader(new URL("https://cups.sus.mcgill.ca/functions/office_hours_names_json.php").openStream(), "UTF-8")) {
					names.clear();
					OhName[][][] oh = new Gson().fromJson(r, OhName[][][].class);
					Calendar calendar = GregorianCalendar.getInstance(); // creates a new calendar instance
					calendar.setTime(new Date());
					int hour = calendar.get(Calendar.HOUR_OF_DAY),
					min = calendar.get(Calendar.MINUTE),
					day = calendar.get(Calendar.DAY_OF_WEEK) - 2,
					slot = (hour - 9) * 2 + (min > 30 ? 1 : 0);
					if (day < oh.length && slot < oh[day].length) {
						for (OhName name : oh[day][slot]) {
							int until;
							for (until = slot; until < oh[day].length; until++) {
								if (!Arrays.asList(oh[day][until]).contains(name)) {
									break;
								}
							}
							names.add(name.first_name + " " + name.last_name + " (until " + (9 + until / 2) + (until % 2 == 0 ? ":00" : ":30") + ")");
						}
					} else {
						names.add("Nobody!");
					}
					if (marqueeHandle == null) {
						startMarquee();
					}
				} catch (Exception e) {
					new RuntimeException("Could not fetch data", e).printStackTrace();
				}
				System.out.println("Done");
			}
		};*/
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS); //TODO: restore to 60 seconds
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}
	
	/**Changes from one entry to another, with a fadeout/fadein effect
	 * @param entry
	 */
	public void changeEntry(final String entry) {
		new Thread("Change Entry") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaEntry > 0) {
					alphaEntry--;
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(OfficeHoursMarquee.this.currentEntry) {
					OfficeHoursMarquee.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) {
					alphaEntry++;
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeOutMs / maxAlpha);
				}
			}
		}.start();
	}
	
	public static void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void startMarquee() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable marquee = new Runnable() {
			Iterator<String> iterNames = names.iterator();
			public void run() {
				try {
					if (!iterNames.hasNext()) {
						iterNames = names.iterator();
					}
					
					
					if (iterNames.hasNext()){
//						String s = iterNames.next();
						changeEntry(iterNames.next());
//						System.out.println("iternames" + s);
					}

					OfficeHoursMarquee.this.onChange();
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
	
	public static void trustAllCerts() {
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager(){
		    public X509Certificate[] getAcceptedIssuers(){return null;}
		    public void checkClientTrusted(X509Certificate[] certs, String authType){}
		    public void checkServerTrusted(X509Certificate[] certs, String authType){}
		}};
		try {
		    SSLContext sc = SSLContext.getInstance("TLS");
		    sc.init(null, trustAllCerts, new SecureRandom());
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
		    ;
		}
	}

	@Override
	public OfficeHoursMarquee setOnChange(Runnable r) {
		this.onChange = r;
		return this;
	}
	
	private void onChange() {
		if (onChange != null) {
			onChange.run();
		}
	}

}
