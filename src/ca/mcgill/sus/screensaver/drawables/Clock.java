package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.text.SimpleDateFormat;
import java.util.Date;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Stage;

/**The big clock in the corner
 * 
 */
public class Clock implements Drawable {
	
	private final SimpleDateFormat format;
	public String time = "";
	private final Color color;
	private Stage parent;
	
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
		new Thread("Time Update"){
			@Override
			public void run() {
				for (;;) {
					String oldTime = time;
					time = Clock.this.format.format(new Date());
					if (!oldTime.equals(time)) {
						if (parent != null) parent.safeRepaint();
					}
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
					}
				}
			}
		}.start();
		if (parent != null) parent.safeRepaint();
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
	public void setParent(Stage parent) {
		this.parent = parent;
	}

}
