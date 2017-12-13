package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Util;

/**The big clock in the corner
 * 
 */
public class Clock implements Drawable {
	
	private final SimpleDateFormat format;
	public String time = "";
	private final Color color, invertedColor;
	private long lastUpdate;
	private final AtomicBoolean dirty = new AtomicBoolean();
	private final int y;
	
	/**Constructor
	 * @param format	the format for the time
	 * @param color		the colour for the font
	 * @param y			the y position on the screen
	 */
	public Clock(String format, int color, int y) {
		this.y = y;
		int a = (color >> 24) & 0xff;
		if (a > 0) { 
			this.color = new Color(color, true);
		} else {
			this.color = new Color(color);
		}
		this.invertedColor = new Color((a << 24) | (0xffffff - (0xffffff & color)), true);
		this.format = new SimpleDateFormat(format == null ? "h:mm a" : format); //will provide a default format
	}
	
	public Clock() {
		this(null, 0x000000, 100);
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont("nhg-thin.ttf").deriveFont(72f));
		Rectangle bounds = g.getFontMetrics().getStringBounds(this.time, g).getBounds();
		int x = canvasWidth - bounds.width - 10;
		boolean invert = Util.luminanceAvg(canvas, x, y + bounds.y, bounds.width, bounds.height) < 0.4;
		g.setColor(invert ? invertedColor : color);
		g.drawString(time, x, y);
	}


	@Override
	public void step(long timestamp) {
		if (timestamp - lastUpdate < 1000) return;
		lastUpdate = timestamp;
		String oldTime = time;
		time = this.format.format(new Date());
		if (!oldTime.equals(time)) {
			this.setDirty(true);
		}
		
	}

	@Override
	public boolean isDirty() {
		return dirty.get();
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);	
	}

}
