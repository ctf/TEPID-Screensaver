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

import ca.mcgill.sus.screensaver.CubicBezier;
import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;

public class UpcomingEvents implements Drawable {

	private final Queue<String> entries = DataFetch.getInstance().upcomingEvents;
	public final int y;
	private int alphaEntry = 0;
	private String currentEntry, currentTitle;
	private final static String[] titles = {"upcoming", "stay in the know", "don\u2019t be lazy", "good CTFers are informed CTFers"};
	private final int color, maxAlpha;	//maximum alpha value for the marquee during fadein
	private final int transition = 800, interval = 5000;
	private final CubicBezier easeInOut = CubicBezier.create(0.42, 0, 0.58, 1.0, (1000.0 / 60.0 / transition) / 4.0);
	private long startTime;
	private double progress;
	private boolean dirty;
	private Iterator<String> entrierator = entries.iterator();

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
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		if (currentEntry == null) return;
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("constanb.ttf").deriveFont(64f));
		g.setColor(new Color((maxAlpha << 24) | color, true));
		g.drawString(currentTitle, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentTitle) / 2, y);
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(48f));
		g.setColor(new Color((alphaEntry << 24) | color, true));
		synchronized(currentEntry)
		{
			g.setColor(new Color((alphaEntry << 24) | color, true));	//sets the colour for the font
			g.drawString(currentEntry, canvasWidth / 2 - g.getFontMetrics().stringWidth(currentEntry) / 2, y + 80); //draws the font
		}
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
	public void step(long timestamp) {
		if (startTime == 0) startTime = timestamp;
		long t = timestamp - startTime;
		int totalDuration = interval + transition;
		double p = 0;
		if (t % totalDuration >= interval) {
			p = easeInOut.calc(((double) (t % totalDuration) - interval) / transition);
		}
		if (entries.isEmpty()) return;
		if ((p >= 0.5 && progress < 0.5) || currentEntry == null) {
			currentEntry = getNextEntry();
		}
		if (p != progress || t == 0) this.setDirty(true);
		progress = p;
		this.alphaEntry = (int) (p < 0.5 ? 1 - (p * maxAlpha * 2) : (p - 0.5) * maxAlpha * 2);
		this.currentTitle = getTitle();
	}
	
	private String getNextEntry() {
		if (entries.isEmpty()) return null;
		if (!entrierator.hasNext()) entrierator = entries.iterator();
		return entrierator.next();
	}

	@Override
	public boolean isDirty() {
		return this.dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;
	}

}
