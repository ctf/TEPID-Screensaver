package ca.mcgill.sus.screensaver;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public class AnimatedSprite {
	
	private final BufferedImage[] frames;
	private int frame, speedMs = 100;
	private final AtomicBoolean dirty = new AtomicBoolean();
	private long lastDirty;
	
	public AnimatedSprite(BufferedImage[] frames) {
		this.frames = frames;
	}
	
	public void step(long timestamp) {
		if (timestamp - lastDirty >= speedMs) {
			frame = (frame + 1) % AnimatedSprite.this.frames.length;
			this.setDirty(true);
			lastDirty = timestamp;
		}
	}
	
	public void draw(Graphics2D g, int x, int y) {
		g.drawImage(frames[frame], x, y, null);
	}

	public int getSpeedMs() {
		return speedMs;
	}

	public AnimatedSprite setSpeedMs(int speedMs) {
		this.speedMs = speedMs;
		return this;
	}
	
	public int getWidth() {
		return frames[0].getWidth();
	}
	
	
	public int getHeight() {
		return frames[0].getHeight();
	}

	public boolean isDirty() {
		return dirty.get();
	}

	public void setDirty(boolean dirty) {
		this.dirty.set(dirty);
	}
}
