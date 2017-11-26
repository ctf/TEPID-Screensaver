package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;

/**The big clock in the corner
 * 
 */
public class Clock implements Drawable {
	
	private final SimpleDateFormat format;
	public String time = "";
	private final Color color;
	private long lastUpdate;
	private boolean dirty;
	
	/**Constructor
	 * @param format	the format for the time
	 * @param color		the colour for the font
	 */
	public Clock(String format, int color) {
		if (((color >> 24) & 0xff) > 0) { 
			this.color = new Color(color, true);
		} else {
			this.color = new Color(color);
		}
		this.format = new SimpleDateFormat(format == null ? "h:mm a" : format); //will provide a default format
	}
	
	public Clock() {
		this(null, 0x000000);
	}

	@Override
	public void draw(Graphics2D g, int canvasWidth, int canvasHeight) {
		g.setFont(FontManager.getInstance().getFont("nhg.ttf").deriveFont(72f));
		g.setColor(this.color);
		int x = canvasWidth - g.getFontMetrics().stringWidth(this.time) - 10;
		g.drawString(time, x, 100);
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
		return dirty;
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty = dirty;		
	}

}
