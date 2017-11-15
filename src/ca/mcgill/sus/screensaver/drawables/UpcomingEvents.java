package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Stage;

public class UpcomingEvents implements Drawable {

	private final Queue<String> entries = DataFetch.getInstance().upcomingEvents;
	private ScheduledFuture<?> marqueeHandle;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry = "";
	private final static String[] titles = {"upcoming", "stay in the know", "don\u2019t be lazy", "good CTFers are informed CTFers"};
	private final int color, maxAlpha;	//maximum alpha value for the marquee during fadein
	private Stage parent;


	/**Constructor
	 * @param y		//the y position
	 * @param color	//the colour of the text
	 */
	public UpcomingEvents(int y, int color)
	{
		this.y = y;		//y position
		//handles the colour. calibrates the maximum alpha to not excede the alpha specified
		this.color = 0xffffff & color; //the text colour
		if (((color >> 24) & 0xff) > 0)
			{maxAlpha = (color >> 24) & 0xff;}
		else
			{maxAlpha = 0xff;}
		if (marqueeHandle == null && !entries.isEmpty()) startMarquee();
		DataFetch.getInstance().addChangeListener(new Runnable() {
			public void run() {
				if (marqueeHandle == null) startMarquee();
			}
		});
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
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(64f));
		g.setColor(new Color((maxAlpha << 24) | color, true));
		String title = getTitle();
		g.drawString(title, canvasWidth / 2 - g.getFontMetrics().stringWidth(title) / 2, y);
		//draws the person logged in
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(48f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry)
		{
			g.setColor(new Color((alphaEntry << 24) | color, true));	//sets the colour for the font
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 80); //draws the font
		}
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

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}

}
