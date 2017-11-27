package ca.mcgill.sus.screensaver.drawables;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.FontManager;

/** Used to display the computer name
 */
public class Header implements Drawable {
	
	public static final int ALIGN_LEFT = 0, ALIGN_CENTER = 1, ALIGN_RIGHT = 2;
	public final String text, font;
	public final int size, y;
	public final Color color;
	protected int alignment = ALIGN_CENTER;
	protected final boolean bold;
	private final AtomicBoolean dirty = new AtomicBoolean(true);

	public Header(String text, int size, int y, int color) {
		this(text, size, y, color, true);
	}
	
	public Header(String text, int size, int y, int color, boolean bold) {
		this(text, size, y, color, bold, bold ? "nhg-bold.ttf" : "nhg.ttf");
	}
	
	public Header(String text, int size, int y, int color, boolean bold, String font) {
		super();
		this.text = text;
		this.size = size;
		this.y = y;
		if (((color >> 24) & 0xff) > 0) { 
			this.color = new Color(color, true);
		} else {
			this.color = new Color(color);
		}
		this.bold = bold;
		this.font = font;
	}
	
	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		g.setFont(FontManager.getInstance().getFont(this.font).deriveFont((float) size));
		g.setColor(color);
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
		g.drawString(text, x, y);
	}

	public Header setAlignment(int alignment) {
		this.alignment = alignment;
		return this;
	}
	
	public int getAlignment() {
		return this.alignment;
	}

	@Override
	public void step(long timestamp) {
		
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
