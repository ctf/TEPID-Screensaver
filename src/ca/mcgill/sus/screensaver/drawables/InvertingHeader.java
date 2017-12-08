package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import ca.mcgill.sus.screensaver.FontManager;
import ca.mcgill.sus.screensaver.Util;

/** Same as Header, but color inverts based on luminosity of canvas beneath
 */
public class InvertingHeader extends Header {
	
	public final Color invertedColor;

	public InvertingHeader(String text, int size, int y, int color) {
		super(text, size, y, color);
		this.invertedColor = new Color((color & 0xff000000) | (0xffffff - (0xffffff & color)), true);
	}
	
	public InvertingHeader(String text, int size, int y, int color, boolean bold) {
		super(text, size, y, color, bold);
		this.invertedColor = new Color((color & 0xff000000) | (0xffffff - (0xffffff & color)), true);
	}
	
	public InvertingHeader(String text, int size, int y, int color, boolean bold, String font) {
		super(text, size, y, color, bold, font);
		this.invertedColor = new Color((color & 0xff000000) | (0xffffff - (0xffffff & color)), true);
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont(this.font).deriveFont((float) size));
		int x;
		switch (alignment) {
		case ALIGN_LEFT:
			x = 10;
			break;
		case ALIGN_RIGHT:
			x = canvasWidth - g.getFontMetrics().stringWidth(this.text) - 10;
			break;
		default:
		case ALIGN_CENTER:
			x = canvasWidth / 2 - g.getFontMetrics().stringWidth(this.text) / 2;
			break;
		}
		Rectangle bounds = g.getFontMetrics().getStringBounds(this.text, g).getBounds();
		boolean invert = Util.luminanceAvg(canvas, x + bounds.x, y + bounds.y, bounds.width, bounds.height) < 0.4;
		g.setColor(invert ? invertedColor : color);
		g.drawString(this.text, x, y);
	}

}
