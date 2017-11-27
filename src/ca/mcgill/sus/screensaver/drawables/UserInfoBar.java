package ca.mcgill.sus.screensaver.drawables;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;

import ca.mcgill.sus.screensaver.DataFetch;
import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Main;
import ca.mcgill.sus.screensaver.Util;
import ca.mcgill.sus.screensaver.io.UserInfo;

public class UserInfoBar extends Header {

	public String displayName = "";
	private final long startTime = System.currentTimeMillis();
	private final Color textColor = new Color(0x44ffffff, true);
	private final Runnable update;
	private long lastUpdate;
	private int interval = 500;
	
	public UserInfoBar(int size, int y) {
		super(null, size, y, !Main.LOGGED_IN ? Main.COLOR_UP : Main.COLOR_DOWN);
		update = new Runnable() {
			public void run() {
				try {
					UserInfo userInfo = DataFetch.getInstance().userInfo.peek();
					if (userInfo == null) {
						displayName = System.getenv("username");
					} else {
						if (Main.OFFICE_COMPUTER) {
							displayName = userInfo.givenName;
						} else {
							displayName = String.format("%s. %s", userInfo.givenName.charAt(0), userInfo.lastName);
						}
					}
				} catch (Exception e) {
					displayName = System.getenv("username");
				}
				setDirty(true);
			}
		};
		DataFetch.getInstance().addChangeListener(update);
	}
	
	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		//time away from machine rounded to 5 minute intervals
		int timeAway = (int) (((System.currentTimeMillis() - this.startTime) / 1000 / 60 / 5) * 5);
		String text;
		if (Main.LOGGED_IN && timeAway > 120) {
			text = "SEEMS LIKE IT'S BEEN FOREVER " + displayName.toUpperCase() + " HAS BEEN GONE";
		} else if (Main.LOGGED_IN && timeAway > 60) {
				text = displayName.toUpperCase() + " HAS BEEN AWAY FOR OVER AN HOUR";
		} else if (Main.LOGGED_IN && timeAway >= 5) {
				text = String.format(displayName.toUpperCase() + " HAS BEEN AWAY FOR ABOUT %d MINUTES", timeAway);
		} else if (Main.LOGGED_IN) {
			text = "THIS COMPUTER IS IN USE BY " + displayName.toUpperCase();
		} else {
			text = "PRESS CTRL + ALT + DELETE TO LOG IN";
		}
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont(bold ? "constanb.ttf" : "constan.ttf").deriveFont((float) size));
		int x;
		switch (alignment) {
		case ALIGN_LEFT:
			x = 10;
			break;
		case ALIGN_RIGHT:
			x = canvasWidth - g.getFontMetrics().stringWidth(text) - 10;
			break;
		default:
		case ALIGN_CENTER:
			x = canvasWidth / 2 - g.getFontMetrics().stringWidth(text) / 2;
			break;
		}
		g.setColor(color);
		GlyphVector v = Util.getStringVector(g, text);
		Rectangle sb = Util.getRealStringBounds(v);
		int pad = 14;
		g.fillRect(0, y - sb.height / 2 - pad, canvasWidth, sb.height + pad * 2);
		g.setColor(textColor);
//		g.drawString(text, x, y);
	    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
		g.fill(v.getOutline(x, y + pad));
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
		if (!Main.LOGGED_IN) g.setColor(new Color(0x20000000,true));
		g.fill(v.getOutline(x, y + pad));
	}
	
	@Override
	public void step(long timestamp) {
		if (timestamp - lastUpdate >= interval) {
			lastUpdate = timestamp;
			update.run();
		}
	}

}
