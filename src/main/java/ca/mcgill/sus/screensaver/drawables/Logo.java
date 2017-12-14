package ca.mcgill.sus.screensaver.drawables;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

import ca.mcgill.sus.screensaver.Drawable;
import ca.mcgill.sus.screensaver.SpriteManager;

public class Logo implements Drawable {
	
	private int x, y, w = 1024, h = 768;
	private final BufferedImage logo = SpriteManager.getInstance().getAlphaSprite("logo_nice.png", 0.6f);
	private final int interval;
	private final Random random = new Random();
	private long lastUpdate;
	private final AtomicBoolean dirty = new AtomicBoolean();
	
	public Logo(int interval) {
		this.interval = interval;
	}

	@Override
	public void draw(Graphics2D g, BufferedImage canvas, int canvasWidth, int canvasHeight) {
		w = canvasWidth;
		h = canvasHeight;
		g.drawImage(logo, x, y, null);
	}

	@Override
	public void step(long timestamp) {
		if (timestamp - lastUpdate < interval * 1000) return;
		lastUpdate = timestamp;
		if (w > 0 && h > 0) {
			x = random.nextInt(w - logo.getWidth());
			y = random.nextInt(h - logo.getHeight() - 200) + 200;
			this.setDirty(true);
		}
		
	}

	@Override
	public boolean isDirty() {
		return this.dirty.get();
	}

	@Override
	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);
	}
}
