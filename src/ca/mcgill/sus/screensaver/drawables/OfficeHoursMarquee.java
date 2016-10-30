package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
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
import ca.mcgill.sus.screensaver.io.CheckedInData;
import ca.mcgill.sus.screensaver.io.SignUp;

public class OfficeHoursMarquee implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder
			.newBuilder()
			.register(JacksonFeature.class)
			.build()
			.target(Main.serverUrl); //initialises the server as a targetable thing
	
	private final Queue<CheckedInData> checkedInData = new ConcurrentLinkedQueue<>();
	private ScheduledFuture<?> dataFetchHandle, marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private CheckedInData currentEntry = new CheckedInData();
	private final String title = "CURRENT VOLUNTEERS ON DUTY";
	private Runnable onChange;
	private final int color, maxAlpha;	//maximum alpha value for the marquee during fadein 
	private final int pad = 14;
	private static final Color clrNotCheckedIn = new Color(0xccdc241f, true);	//TODO: factor all of these into main
	private static final Color clrCheckedIn = new Color (0xcc50c954, true);
	
	private final Random randomizer = new Random(); 	//"necessary" for the random nickname selector
	private final List<String> genericNicknames = Arrays.asList("The Great", "Unnicknamed", "awesomesauce", "Mr. Clean", "Captain America", "Cupcake", "Kiddo", "Stitches", "Spookypants", "Professor", "Bugs", "The Nice", "The Magnificent", "The Clever", "The Kind", "The Courageous", "The Hero", "The Beloved", "McCool", "Bob", "Bob", "Bob", "Bob", "Jimbo", "Oompa", "Loompa", "Finch", "Hawk", "Eagle", "SegFault", "Elmer", "Tweety Bird", "Grover", "Cookie Monster", "Yarwhal", "Spock-lite","Darth", "Skywalker");
	

	/**Constructor
	 * @param y		//the y position
	 * @param color	//the colour of the text
	 */
	public OfficeHoursMarquee(int y, int color) 
	{
		startDataFetch();
		this.y = y;		//y position
		//handles the colour. calibrates the maximum alpha to not excede the alpha specified
		this.color = 0xffffff & color; //the text colour
		if (((color >> 24) & 0xff) > 0) 
			{maxAlpha = (color >> 24) & 0xff;} 
		else 
			{maxAlpha = 0xff;}
		//populates the dummy object so that it doesn't try running with an invalid object
		this.currentEntry.checkedIn=false;
		this.currentEntry.shortUserName="null";
		this.currentEntry.text = "";
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
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		//draws the person logged in
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry) 
		{
			g.setColor(new Color ( (alphaEntry << 24) | ((currentEntry.checkedIn?clrCheckedIn.getRGB():clrNotCheckedIn.getRGB()) & 0x00FFFFFF) , true)); 	//sets red for offline, green for online
			g.fillRect(0, y + g.getFontMetrics().getAscent() - pad, canvasWidth, g.getFontMetrics().getHeight()+pad);			//coloured rectangle for the back of the office hours
			g.setColor(new Color((alphaEntry << 24) | color, true));	//sets the colour for the font
			g.drawString(currentEntry.text, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry.text) / 2, y + 40); //draws the font
		}
	}
	
	/**Fetches the data to populate the messages
	 */
	public void startDataFetch() {
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable dataFetch = new Runnable() {
			public void run() 
			{
				System.out.println("Fetching OH data");
				checkedInData.clear();
				//creates a new calendar instance, as well as the date formats necessary to build the message and the query
				Calendar calendar = GregorianCalendar.getInstance(); 
				calendar.setTime(new Date());
				SimpleDateFormat slotFormat = new SimpleDateFormat ("EEEEE-HH:mm");
				SimpleDateFormat timeFormat = new SimpleDateFormat ("HH:mm");
				SimpleDateFormat dayFormat = new SimpleDateFormat ("EEEEE");
				
				//debug (preset options to set time data) 
//				calendar.set(Calendar.MINUTE, 30);
//				calendar.set(Calendar.HOUR, 2);
//				calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
				//end debug				
				
				calendar.set(Calendar.MINUTE, (calendar.get(Calendar.MINUTE)/30)*30);
				slotFormat.setCalendar(calendar);
				dayFormat.setCalendar(calendar);
				timeFormat.setCalendar(calendar);
				
				//pulls the list of signup objects for the timeslot
				List<SignUp> signUps = (tepidServer
						.path("office-hours").path("on-duty")
						.path(slotFormat.format(calendar.getTime()))
						.request(MediaType.APPLICATION_JSON)
						.get(new GenericType <List<SignUp>>(){}));
				//pulls the list of people checked in
				List<String> checkedIn = (tepidServer
						.path("office-hours").path("checked-in")
						.request(MediaType.APPLICATION_JSON)
						.get(new GenericType <List<String>>(){}));
				
				//iterates over all the fetched signUp objects and turns them into checkedInData
				for (SignUp s : signUps)
				{
					CheckedInData out = new CheckedInData();
					//sets short user (for further ID if necessary) and checked in status
					out.shortUserName = s.getName();
					out.checkedIn = checkedIn.contains(s.getName());
					
					//builds the checkedIn object
					out.text = "";
					out.checkedIn = (checkedIn.contains(s.getName())?true:false);
					out.text = (s.getGivenName()); //adds given name to message
					
					//adds official nickname to message
					//if none, adds a generic nickname from the list
					if (s.getNickname() != null)
					{
						out.text += " \"" + s.getNickname() + "\" "; //TODO: get lastname here as well
					}
					else
					{
						out.text += " \"" +  genericNicknames.get(randomizer.nextInt(genericNicknames.size())) + "\" ";	//adds a generic nickname
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
					out.text += "until " + timeFormat.format(tempTime.getTime());
					checkedInData.add(out);		//appends to list
				}
				//an else clause for none registered for office hours
				if (checkedInData.isEmpty())
				{
					CheckedInData out = new CheckedInData();
					out.text = "Nobody!";
					out.shortUserName = null;
					out.checkedIn = true;
					checkedInData.add(out);
				} 		
				
				if (marqueeHandle == null) 
					{startMarquee();}
				
				System.out.println("Marquee Done");
			}
		};
		
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
		dataFetchHandle = Executors.newScheduledThreadPool(1).scheduleAtFixedRate(dataFetch, 0, 60, TimeUnit.SECONDS); //TODO: restore to 60 seconds
	}
	
	public void stopDataFetch() {
		if (dataFetchHandle != null) dataFetchHandle.cancel(false);
	}
	
	/**Changes from one entry to another, with a fadeout/fadein effect
	 * @param entry	The thing to fade
	 * 
	 */
	public void changeEntry(final CheckedInData entry) 
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
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(OfficeHoursMarquee.this.currentEntry) 
				{
					OfficeHoursMarquee.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) 
				{
					alphaEntry++;
					OfficeHoursMarquee.this.onChange();
					OfficeHoursMarquee.sleep(fadeOutMs / maxAlpha);
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
		//TODO figure out why cert isn't validating
		trustAllCerts();
		final Runnable marquee = new Runnable() {
			Iterator<CheckedInData> iterNames = checkedInData.iterator();
			public void run() {
				try {
					if (!iterNames.hasNext()) 
						{iterNames = checkedInData.iterator();}
					if (iterNames.hasNext())
						{changeEntry(iterNames.next());}
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
	
	/**A fix for it not trusting the certs by default. is an open to do item
	 * 
	 */
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
