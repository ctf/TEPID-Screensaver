package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.jackson.JacksonFeature;

import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.Stage;
import ca.mcgill.sus.screensaver.io.MarqueeData;

public class Marquee implements Drawable {
	
	final static WebTarget tepidServer = ClientBuilder.newBuilder().register(JacksonFeature.class).build().target(Main.serverUrl); //initialises the server as a targetable thing
	
	private final Queue<MarqueeData> marqueeData = DataFetch.getInstance().marqueeData;
	private ScheduledFuture<?> marqueeHandle;
	public final int y;
	private int alphaTitle = 0, alphaEntry = 0;
	private String currentTitle = "", currentEntry = "";
	private final int color, maxAlpha;

	private Stage parent;
	
	/**
	 * @param y			the y coordinate
	 * @param color		the text colour
	 */
	public Marquee(int y, int color) {
		this.y = y;
		//handles the colour. calibrates the maximum alpha to not excede the alpha specified
		this.color = 0xffffff & color;
		if (((color >> 24) & 0xff) > 0) { 
			maxAlpha = (color >> 24) & 0xff;
		} else {
			maxAlpha = 0xff;
		}
		if (marqueeHandle == null && !marqueeData.isEmpty()) startMarquee();
		DataFetch.getInstance().addChangeListener(new Runnable() {
			public void run() {
				if (marqueeHandle == null) startMarquee();
			}
		});
	}

	/* (non-Javadoc)
	 * @see ca.mcgill.sus.screensaver.Drawable#draw(java.awt.Graphics2D, int, int)
	 */
	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(32f));
		g.setColor(new Color((alphaTitle << 24) | color, true));
		synchronized(currentTitle) {
			g.drawString(currentTitle, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentTitle) / 2, y);
		}
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(24f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry) {
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 40);
		}
	}
	
	
	/** Changes the entry on the marquee with a fadein/fadeout
	 * Note that this method does not change the title.
	 * @param entry the entry to display
	 */
	public void changeEntry(final String entry) 
	{
		new Thread("Change Entry") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaEntry > 0) {
					alphaEntry--;
					if (parent != null) parent.safeRepaint();
					Marquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(Marquee.this.currentEntry) {
					Marquee.this.currentEntry = entry;
				}
				while (alphaEntry < maxAlpha) {
					alphaEntry++;
					if (parent != null) parent.safeRepaint();
					Marquee.sleep(fadeOutMs / maxAlpha);
				}
			}
		}.start();
	}
	
	/** Changes the title
	 * Note that this method only changes the title, and is invoked whenever the entries of one title are exhausted
	 * @param title the new title to display
	 */
	public void changeTitle(final String title) {
		new Thread("Change Title") {
			@Override
			public void run() {
				final int fadeInMs = 1400, fadeOutMs = 800;
				while (alphaTitle > 0) {
					alphaTitle--;
					if (parent != null) parent.safeRepaint();
					Marquee.sleep(fadeInMs / maxAlpha);
				}
				synchronized(Marquee.this.currentTitle) {
					Marquee.this.currentTitle = title;
				}
				while (alphaTitle < maxAlpha) {
					alphaTitle++;
					if (parent != null) parent.safeRepaint();
					Marquee.sleep(fadeOutMs / maxAlpha);
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
	
	/**starts the marquee. It will then iterate over all titles, iterating over all of their display items
	 * 
	 */
	public void startMarquee() 
	{
		final Runnable marquee = new Runnable() {
			Iterator<MarqueeData> iterMarquee = marqueeData.iterator();
			Iterator<String> iterEntry;
			MarqueeData md;
			public void run() {
				if (marqueeData.isEmpty()) {
					marqueeHandle = null;
					return;
				}
				try {
					//gets the next entry to display
					if (!iterMarquee.hasNext()) 
					{
						iterMarquee = marqueeData.iterator(); 
					}
					//if there are no more entries under the current title, it will change to the next title
					if (iterEntry == null || !iterEntry.hasNext()) {
						md = iterMarquee.next();
						changeTitle(md.title);
						iterEntry = md.entry.iterator();
					}
					changeEntry(iterEntry.next());
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

	@Override
	public void setParent(Stage parent) {
		this.parent = parent;
	}

}
